package control;

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
import main.Main;

public class ASIOController implements AsioDriverListener {

	private static ASIOController	instance;
	private static final Logger		LOG			= Logger.getLogger(ASIOController.class);
	private AsioDriver				asioDriver;
	private Set<AsioChannel>		activeChannels;
	private int						bufferSize	= 1024;
	private double					sampleRate;
	private AsioChannel				activeChannel;
	float							lastPeak	= 0, peak = 0, rms = 0;
	// FFT
	private float[]					output;
	private int						bufferCount;
	private DoubleDCT_1D			fft;
	private int[]					index;
	private double[][]				fftBuffer;
	private static int				fftBufferSize;
	private double[][]				spectrumMap;
	private ArrayList<Channel>		channelList;

	public static List<String> getInputDevices() {
		return AsioDriver.getDriverNames();
	}

	public static ASIOController getInstance() {
		return instance;
	}

	public ASIOController(String ioName) {
		LOG.info("Loading ASIO driver '" + ioName + "'");
		instance = this;
		try {
			asioDriver = AsioDriver.getDriver(ioName);
		}
		catch (AsioException e) {
			LOG.error("No ASIO device found");
		}
		if (asioDriver == null) {
			LOG.warn("Unable to load ASIO driver '" + ioName + "'");
			Main.quit();
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
		initFFT();
		LOG.info("ASIO driver started");
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
		// restartAudioAnalysis();
	}

	private void restartAudioAnalysis() {
		if (activeChannel != null) {
			// JVMAudioInputStream stream = new
			// JVMAudioInputStream(audioStream);
			// dispatcher = new AudioDispatcher(stream, 864, overlap);
			// dispatcher.addAudioProcessor(new
			// PitchProcessor(PitchEstimationAlgorithm.YIN, (float) sampleRate,
			// bufferSize, this));
			// dispatcher.addAudioProcessor(fftProcessor);
			// run the dispatcher (on a new thread).
			// new Thread(dispatcher, "Audio dispatching").start();
		}
	}

	@Override
	public void sampleRateDidChange(double sampleRate) {
		// TODO Auto-generated method stub
	}

	@Override
	public void resetRequest() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resyncRequest() {
		// TODO Auto-generated method stub
	}

	@Override
	public void bufferSizeChanged(int bufferSize) {
		// TODO Auto-generated method stub
	}

	@Override
	public void latenciesChanged(int inputLatency, int outputLatency) {
		// TODO Auto-generated method stub
	}

	@Override
	public void bufferSwitch(long sampleTime, long samplePosition, Set<AsioChannel> channels) {
		for (AsioChannel channel : channels) {
			if (channel.isInput() && channel.isActive() && channel.getChannelIndex() == activeChannel.getChannelIndex()) {
				channel.read(output);
				calculatePeaks(output);
				fftThis();
				break;
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
				// int baseFrequencyIndex = getBaseFrequencyIndex(fftData);
				// int baseFrequencyIndex = getBaseFrequencyIndexHPS(fftData);
				// double baseFrequency =
				// getFrequencyForIndex(baseFrequencyIndex, fftData.length,
				// (int) sampleRate) /2;
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

	public int getLatency() {
		if (asioDriver != null) {
			double inSec = asioDriver.getLatencyInput() / asioDriver.getSampleRate();
			double inMillis = inSec * 100;
			return (int) Math.round(inMillis);
		}
		return 0;
	}
}
