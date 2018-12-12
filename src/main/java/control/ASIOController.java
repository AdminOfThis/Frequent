package control;

import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jtransforms.dct.DoubleDCT_1D;

import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioException;

import data.Channel;
import data.FileIO;
import data.Group;
import data.Input;
import main.Main;

public class ASIOController implements AsioDriverListener, DataHolder<Input> {

	private static ASIOController	instance;
	private static final Logger		LOG				= Logger.getLogger(ASIOController.class);
	private static int				fftBufferSize;
	private String					driverName;
	private AsioDriver				asioDriver;
	private int						bufferSize		= 1024;
	private double					sampleRate;
	private AsioChannel				activeChannel;
	private float					lastPeak		= 0, peak = 0, rms = 0;
	private float					baseFrequency	= -1;
	// FFT
	// private float[] output;
	private int						bufferCount;
	private DoubleDCT_1D			fft;
	private int[]					index;
	private double[][]				fftBuffer;
	private double[][]				spectrumMap;
	private List<Channel>			channelList;
	private List<Group>				groupList		= new ArrayList<>();
	private List<FFTListener>		fftListeners	= new ArrayList<>();
	private double[][][]			bufferingBuffer	= new double[2][2][1024];
	private ExecutorService			exe;
	private long					time;

	public static ASIOController getInstance() {
		return instance;
	}

	public static List<String> getPossibleDrivers() {
		ArrayList<String> result = new ArrayList<>();
		try {
			List<String> preList = AsioDriver.getDriverNames();
			// checking if connected
			for (String possibleDriver : preList) {
				AsioDriver tempDriver = null;
				try {
					tempDriver = AsioDriver.getDriver(possibleDriver);
					// adding if inputs avaliable
					if (tempDriver != null && tempDriver.getNumChannelsInput() > 0) {
						result.add(possibleDriver);
					}
				} catch (Exception e) {
					LOG.debug(possibleDriver + " is unavailable");
				} finally {
					if (tempDriver != null) {
						tempDriver.shutdownAndUnloadDriver();
					}
				}
			}
		} catch (UnsatisfiedLinkError e) {
			LOG.warn("The corresponding library jasiohost64.dll was not found");
		}
		return result;
	}

	public ASIOController(final String ioName) {
		LOG.info("Created ASIO Controller");
		driverName = ioName;
		instance = this;
		FileIO.registerSaveData(this);
		restartASIODriver();
		LOG.info("ASIO driver started");
		initFFT();
		LOG.info("FFT Analysis started");
	}

	@Override
	public void add(final Input t) {
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
			for (Channel c : channelList) {
				if (c.getGroup() != null && c.getGroup().getName().equals(g.getName())) {
					g.addChannel(c);
					c.addListener(g);
				}
			}
		}
	}

	public void addFFTListener(final FFTListener pausableView) {
		if (!fftListeners.contains(pausableView)) {
			fftListeners.add(pausableView);
		}
	}

	public void addGroup(final Group group) {
		if (!groupList.contains(group)) {
			LOG.info("Group " + group.getName() + " added");
			groupList.add(group);
		}
	}

	// taken from Bachelor thesis at
	// https://www.vutbr.cz/studium/zaverecne-prace?zp_id=88462
	private double[] applyHannWindow(final double[] input) {
		double[] out = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			double mul = 0.5 * (1 - Math.cos(2 * Math.PI * i / input.length - 1));
			out[i] = mul * input[i];
		}
		return out;
	}

	@Override
	public void bufferSizeChanged(final int bufferSize) {
		LOG.info("Buffer size changed");
		restartASIODriver();
	}

	@Override
	public void bufferSwitch(final long sampleTime, final long samplePosition, final Set<AsioChannel> channels) {
		time = sampleTime;
		for (AsioChannel channel : channels) {
			try {
				if (channel.isInput() && channel.isActive()) {
					Runnable runnable = () -> {
						try {
							float[] output = new float[bufferSize];
							if (activeChannel != null) {
								try {
									channel.read(output);
								} catch (BufferUnderflowException e1) {
									LOG.debug("Underflow Exception", e1);
								}
								if (channel.getChannelIndex() == activeChannel.getChannelIndex()) {
									calculatePeaks(output);
									fftThis(output);
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
									c.setLevel(max, sampleTime);
									c.setBuffer(Arrays.copyOf(output, output.length), samplePosition);
								}
							}
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					};
					if (exe != null) {
						exe.submit(runnable);
					}
				}
			} catch (ConcurrentModificationException e) {
			}
		}
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

	@Override
	public void clear() {
		// do nothing
		// channelList.clear();
	}

	private void fftThis(final float[] output) {
		try {
			for (int i = 0; i < bufferSize; i++) {
				for (int j = 0; j < bufferCount; j++) {
					fftBuffer[j][index[j]] = output[i];
				}
				for (int j = 0; j < bufferCount; j++) {
					index[j]++;
				}
			}
			// TODO IST DAS RICHTIG???
			for (int i = 0; i < bufferCount; i++) {
				if (index[i] == fftBufferSize) {
					fftBuffer[i] = applyHannWindow(fftBuffer[i]);
					// fft.realForward(fftBuffer[i]);
					fft.forward(fftBuffer[i], false);
					// double[] fftData = fftAbs(fftBuffer[i]);
					double[] fftData = fftBuffer[i];
					int baseFrequencyIndex = getBaseFrequencyIndex(fftData);
					// int baseFrequencyIndex =
					// getBaseFrequencyIndexHPS(fftData);
					baseFrequency = getFrequencyForIndex(baseFrequencyIndex, fftData.length, (int) sampleRate) / 2;
					// System.out.println("Base " + baseFrequency);
					spectrumMap = getSpectrum(fftData);
					// controller.updateText(baseFrequency);
					index[i] = 0;
				}
			}
			bufferingBuffer[1] = bufferingBuffer[0];
			bufferingBuffer[0] = spectrumMap;
			for (int i = 0; i < spectrumMap[0].length - 1; i++) {
				spectrumMap[1][i] = (bufferingBuffer[0][1][i] + bufferingBuffer[1][1][i]) / 2.0;
			}
			for (FFTListener l : fftListeners) {
				if (l != null) {
					new Thread(() -> {
						try {
							l.newFFT(spectrumMap);
						} catch (Exception e) {
							LOG.warn("Unable to notify FFTListener");
							LOG.debug("", e);
						}
					}).start();
				}
			}
		} catch (Exception e) {
			LOG.debug("Problem on FFT", e);
		}
	}

	public float getBaseFrequency() {
		return baseFrequency;
	}

	private int getBaseFrequencyIndex(final double[] spectrum) {
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

	@Override
	public List<Input> getData() {
		ArrayList<Input> result = new ArrayList<>();
		result.addAll(getInputList());
		result.addAll(getGroupList());
		return result;
	}

	// taken from https://gist.github.com/akuehntopf/4da9bced2cb88cfa2d19,
	// author Andreas Kuehntopf
	private float getFrequencyForIndex(final int index, final int size, final int rate) {
		return (float) index * (float) rate / size;
	}

	public ArrayList<Group> getGroupList() {
		return new ArrayList<>(groupList);
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

	public float getLastPeak() {
		return lastPeak;
	}

	public double getLatency() {
		if (asioDriver != null) {
			double inSec = asioDriver.getLatencyInput() / asioDriver.getSampleRate();
			return Math.round(inSec * 10000.0) / 10.0;
		}
		return 0;
	}

	public int getNoOfInputs() {
		if (asioDriver != null)
			return asioDriver.getNumChannelsInput();
		return -1;
	}

	public float getPeak() {
		return peak;
	}

	public float getRms() {
		return rms;
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

	public double[][] getSpectrumMap() {
		return spectrumMap;
	}

	private void initFFT() {
		bufferCount = 1;
		// fftBufferSize = 16384;
		fftBufferSize = bufferSize;
		// fft = new DoubleFFT_1D(fftBufferSize);
		fft = new DoubleDCT_1D(fftBufferSize);
		fftBuffer = new double[bufferCount][fftBufferSize];
		index = new int[bufferCount];
		for (int i = 0; i < bufferCount; i++) {
			index[i] = i * fftBufferSize / bufferCount;
		}
	}

	@Override
	public void latenciesChanged(final int inputLatency, final int outputLatency) {
		LOG.info("Latencies changed");
		restartASIODriver();
	}

	public void removeFFTListener(final FFTListener l) {
		fftListeners.remove(l);
	}

	public void removeGroup(final Group group) {
		groupList.remove(group);
	}

	@Override
	public void resetRequest() {
		LOG.info("Reset requested");
		restartASIODriver();
	}

	private void restartASIODriver() {
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
		}
		LOG.info("Loading ASIO driver '" + driverName + "'");
		try {
			asioDriver = AsioDriver.getDriver(driverName);
		} catch (AsioException e) {
			LOG.error("No ASIO device found");
		}
		if (asioDriver == null) {
			LOG.warn("Unable to load ASIO driver '" + driverName + "'");
			Main.close();
		}
		asioDriver.addAsioDriverListener(this);
		// create a Set of AsioChannels, defining which input and output
		// channels will be used
		Set<AsioChannel> activeChannels = new HashSet<>();
		// configure the ASIO driver to use the given channels
		for (int i = 0; i < asioDriver.getNumChannelsInput(); i++) {
			activeChannels.add(asioDriver.getChannelInput(i));
		}
		// activeChannels.add(asioDriver.getChannelInput(0));
		bufferSize = asioDriver.getBufferPreferredSize();
		// output = new float[bufferSize];
		sampleRate = asioDriver.getSampleRate();
		// create the audio buffers and prepare the driver to run
		asioDriver.createBuffers(activeChannels);
		// start the driver
		asioDriver.start();
		// creating ThreadPool
		exe = new ThreadPoolExecutor(4, activeChannels.size() * 2, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		LOG.info("Inputs " + asioDriver.getNumChannelsInput() + ", Outputs " + asioDriver.getNumChannelsOutput());
		LOG.info("Buffer size: " + bufferSize);
		LOG.info("Samplerate: " + sampleRate);
	}

	@Override
	public void resyncRequest() {
		LOG.info("Resync requested");
		restartASIODriver();
	}

	@Override
	public void sampleRateDidChange(final double sampleRate) {
		LOG.info("Sample rate changed");
		restartASIODriver();
	}

	@Override
	public void set(final List<Input> list) {
		for (Serializable s : list) {
			if (s instanceof Channel) {
				channelList.add((Channel) s);
			} else if (s instanceof Group) {
				groupList.add((Group) s);
			}
		}
	}

	public void setActiveChannel(final AsioChannel channel) {
		activeChannel = channel;
	}

	public void shutdown() {
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
		}
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public long getTime() {
		return time;
	}

	public int getSampleRate() {
		return (int) Math.floor(sampleRate);
	}
}
