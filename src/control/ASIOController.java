package control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jtransforms.dct.DoubleDCT_1D;

import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioException;

import data.Channel;
import data.FileIO;
import data.Group;
import main.Main;

public class ASIOController implements AsioDriverListener, DataHolder<Serializable> {

	private static ASIOController	instance;
	private static final Logger		LOG				= Logger.getLogger(ASIOController.class);
	private String					driverName;
	private AsioDriver				asioDriver;
	private Set<AsioChannel>		activeChannels;
	private int						bufferSize		= 1024;
	private double					sampleRate;
	private AsioChannel				activeChannel;
	float							lastPeak		= 0, peak = 0, rms = 0;
	private float					baseFrequency	= -1;
	// FFT
	private float[]					output;
	private int						bufferCount;
	private DoubleDCT_1D			fft;
	private int[]					index;
	private double[][]				fftBuffer;
	private static int				fftBufferSize;
	private double[][]				spectrumMap;
	private List<Channel>			channelList;
	private List<Group>				groupList		= new ArrayList<>();

	public static List<String> getInputDevices() {
		return AsioDriver.getDriverNames();
	}

	public static ASIOController getInstance() {
		return instance;
	}

	public ASIOController(String ioName) {
		LOG.info("Created ASIO Controller");
		this.driverName = ioName;
		instance = this;
		FileIO.registerSaveData(this);
		restartASIODriver();
		LOG.info("ASIO driver started");
		initFFT();
		LOG.info("FFT Analysis started");
	}

	private void restartASIODriver() {
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
		}
		LOG.info("Loading ASIO driver '" + driverName + "'");
		try {
			asioDriver = AsioDriver.getDriver(driverName);
		}
		catch (AsioException e) {
			LOG.error("No ASIO device found");
		}
		if (asioDriver == null) {
			LOG.warn("Unable to load ASIO driver '" + driverName + "'");
			Main.close();
		}
		asioDriver.addAsioDriverListener(this);
		// create a Set of AsioChannels, defining which input and output
		// channels will be used
		activeChannels = new HashSet<>();
		// configure the ASIO driver to use the given channels
		for (int i = 0; i < asioDriver.getNumChannelsInput(); i++) {
			activeChannels.add(asioDriver.getChannelInput(i));
		}
		// activeChannels.add(asioDriver.getChannelInput(0));
		bufferSize = asioDriver.getBufferPreferredSize();
		output = new float[bufferSize];
		sampleRate = asioDriver.getSampleRate();
		// create the audio buffers and prepare the driver to run
		asioDriver.createBuffers(activeChannels);
		// start the driver
		asioDriver.start();
		LOG.info("Inputs " + asioDriver.getNumChannelsInput() + ", Outputs " + asioDriver.getNumChannelsOutput());
		LOG.info("Buffer size: " + bufferSize);
		LOG.info("Samplerate: " + sampleRate);
	}

	private void initFFT() {
		bufferCount = 1;
		// fftBufferSize = 16384;
		fftBufferSize = 2048;
		// fft = new DoubleFFT_1D(fftBufferSize);
		fft = new DoubleDCT_1D(fftBufferSize);
		fftBuffer = new double[bufferCount][fftBufferSize];
		index = new int[bufferCount];
		for (int i = 0; i < bufferCount; i++) {
			index[i] = i * fftBufferSize / bufferCount;
		}
	}

	public void shutdown() {
		asioDriver.shutdownAndUnloadDriver();
	}

	public int getNoOfInputs() {
		if (asioDriver != null) { return asioDriver.getNumChannelsInput(); }
		return -1;
	}

	public List<Channel> getInputList() {
		if (channelList == null) {
			channelList = new ArrayList<>();
			for (int i = 0; i < getNoOfInputs(); i++) {
				channelList.add(new Channel(asioDriver.getChannelInput(i)));
			}
		}
		return channelList;
	}

	public void setActiveChannel(final AsioChannel channel) {
		activeChannel = channel;
	}

	@Override
	public void sampleRateDidChange(double sampleRate) {
		LOG.info("Sample rate changed");
		restartASIODriver();
	}

	@Override
	public void resetRequest() {
		LOG.info("Reset requested");
		restartASIODriver();
	}

	@Override
	public void resyncRequest() {
		LOG.info("Resync requested");
		restartASIODriver();
	}

	@Override
	public void bufferSizeChanged(int bufferSize) {
		LOG.info("Buffer size changed");
		restartASIODriver();
	}

	@Override
	public void latenciesChanged(int inputLatency, int outputLatency) {
		LOG.info("Latencies changed");
		restartASIODriver();
	}

	@Override
	public void bufferSwitch(long sampleTime, long samplePosition, Set<AsioChannel> channels) {
		for (AsioChannel channel : channels) {
			if (channel.isInput() && channel.isActive()) {
				try {
					if (activeChannel != null) {
						if (channel.getChannelIndex() == activeChannel.getChannelIndex()) {
							channel.read(output);
							calculatePeaks(output);
							fftThis();
						} else {
							channel.read(output);
						}
						float max = 0;
						for (float f : output) {
							if (f > max) {
								max = f;
							}
						}
						Channel c = null;
						for (Channel cTemp : channelList) {
							if (cTemp.getChannel().equals(channel)) {
								c = cTemp;
								break;
							}
						}
						if (c != null) {
							c.setLevel(max);
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void fftThis() {
		for (int i = 0; i < bufferSize; i++) {
			for (int j = 0; j < bufferCount; j++) {
				fftBuffer[j][index[j]] = output[i];
			}
			for (int j = 0; j < bufferCount; j++) {
				index[j]++;
			}
		}
		for (int i = 0; i < bufferCount; i++) {
			if (index[i] == fftBufferSize) {
				fftBuffer[i] = applyHannWindow(fftBuffer[i]);
				// fft.realForward(fftBuffer[i]);
				fft.forward(fftBuffer[i], false);
				// double[] fftData = fftAbs(fftBuffer[i]);
				double[] fftData = fftBuffer[i];
				int baseFrequencyIndex = getBaseFrequencyIndex(fftData);
				// int baseFrequencyIndex = getBaseFrequencyIndexHPS(fftData);
				baseFrequency = getFrequencyForIndex(baseFrequencyIndex, fftData.length, (int) sampleRate) / 2;
				// System.out.println("Base " + baseFrequency);
				spectrumMap = getSpectrum(fftData);
				// controller.updateText(baseFrequency);
				index[i] = 0;
			}
		}
	}

	public double[][] getSpectrumMap() {
		return spectrumMap;
	}

	private double[][] getSpectrum(final double[] spectrum) {
		double[][] result = new double[2][spectrum.length];
		for (int i = 0; i < spectrum.length; i++) {
			double level = spectrum[i];
			result[1][i] = Math.abs(level);
			result[0][i] = getFrequencyForIndex(i, spectrum.length, (int) sampleRate) / 2.0;
		}
		return result;
	}

	@SuppressWarnings("unused")
	private int getBaseFrequencyIndex(double[] spectrum) {
		double maxVal = Double.NEGATIVE_INFINITY;
		int maxInd = 0;
		for (int i = 0; i < spectrum.length; i++) {
			if (maxVal < spectrum[i]) {
				maxVal = spectrum[i];
				maxInd = i;
			}
		}
		// Interpolate
		// (https://gist.github.com/akuehntopf/4da9bced2cb88cfa2d19#file-hps-java-L144)
		// not necessary, does not help, gives the same results
		// double mid = spectrum[maxInd];
		// double left = spectrum[maxInd- 1];
		// double right = spectrum[maxInd + 1];
		// double shift = 0.5f*(right-left) / ( 2.0f*mid - left - right );
		// maxInd = (int) Math.round(maxInd + shift);
		// maybe useful can be quadratic interpolation:
		// http://musicweb.ucsd.edu/~trsmyth/analysis/Quadratic_interpolation.html
		return maxInd;
	}

	// taken from https://gist.github.com/akuehntopf/4da9bced2cb88cfa2d19,
	// author Andreas Kühntopf
	private float getFrequencyForIndex(int index, int size, int rate) {
		return (float) index * (float) rate / size;
	}

	private void calculatePeaks(final float[] inputArray) {
		// float rms = 0f;
		peak = 0f;
		for (float sample : inputArray) {
			float abs = Math.abs(sample);
			if (abs > peak) {
				peak = abs;
			}
			rms += Math.abs(sample);
		}
		rms = rms / inputArray.length;
		if (lastPeak > peak) {
			lastPeak = lastPeak * 0.975f;
		} else {
			lastPeak = peak;
		}
	}

	public float getPeak() {
		return peak;
	}

	public float getLastPeak() {
		return lastPeak;
	}

	public float getRms() {
		return rms;
	}

	// taken from Bachelor thesis at
	// https://www.vutbr.cz/studium/zaverecne-prace?zp_id=88462
	public double[] applyHannWindow(double[] input) {
		double[] out = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			double mul = 0.5 * (1 - Math.cos(2 * Math.PI * i / input.length - 1));
			out[i] = mul * input[i];
		}
		return out;
	}

	public double getLatency() {
		if (asioDriver != null) {
			double inSec = asioDriver.getLatencyInput() / asioDriver.getSampleRate();
			return Math.round(inSec * 10000.0) / 10.0;
		}
		return 0;
	}

	@Override
	public void set(List<Serializable> list) {
		for (Serializable s : list) {
			if (s instanceof Channel) {
				channelList.add((Channel) s);
			} else if (s instanceof Group) {
				groupList.add((Group) s);
			}
		}
	}

	@Override
	public List<Serializable> getData() {
		ArrayList<Serializable> result = new ArrayList<>();
		result.addAll(getInputList());
		result.addAll(getGroupList());
		return result;
	}

	@Override
	public void clear() {
		// do nothing
		// channelList.clear();
	}

	@Override
	public void add(Serializable t) {
		if (t instanceof Channel) {
			Channel channel = (Channel) t;
			if (!channelList.contains(channel)) {
				Channel oldChannel = null;
				for (Channel c : channelList) {
					// remove pld channel, and replace with new
					if (c.getChannelIndex() == channel.getChannelIndex()) {
						oldChannel = c;
						break;
					}
				}
				if (oldChannel != null) {
					channelList.remove(oldChannel);
					channelList.add(channel);
					channel.setChannel(oldChannel.getChannel());
				}
			}
		} else if (t instanceof Group) {
			Group g = (Group) t;
			if (!groupList.contains(g)) {
				groupList.add(g);
			}
		}
	}

	public float getBaseFrequency() {
		return baseFrequency;
	}

	public ArrayList<Group> getGroupList() {
		return new ArrayList<>(groupList);
	}

	public void addGroup(Group group) {
		if (!groupList.contains(group)) {
			LOG.info("Group " + group.getName() + " added");
			groupList.add(group);
		}
	}
}
