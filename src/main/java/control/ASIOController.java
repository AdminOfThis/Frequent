package control;

import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.internal.util.Objects;
import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioException;

import data.Channel;
import data.DriverInfo;
import data.FileIO;
import data.Group;
import data.Input;
import main.FXMLMain;

/**
 * @author AdminOfThis
 *
 */
public class ASIOController implements AsioDriverListener, DataHolder<Input>, ChannelListener {

	/**
	 * Default buffer size
	 */
	public static final int DESIRED_BUFFER_SIZE = 1024;
	private static ASIOController instance;
	private static final Logger LOG = LogManager.getLogger(ASIOController.class);
	private static int fftBufferSize;
	private String driverName;
	private AsioDriver asioDriver;
	private int bufferSize = 1024;
	private double sampleRate;
	private Channel activeChannel;
	private float baseFrequency = -1;
	// FFT
	// private float[] output;
	private int bufferCount;
	private int[] index;
	private List<Channel> channelList;
	private List<Group> groupList = new ArrayList<>();
	private List<FFTListener> fftListeners = new ArrayList<>();
	private ExecutorService exe;
	private long time;
	private boolean isFFTing = false;
	private static List<DriverInfo> driverList;

	/**
	 * @return Returns the instance of the {@link #ASIOController}, as definded by
	 *         singleton pattern
	 */
	public static ASIOController getInstance() {
		if (instance == null) {
			instance = new ASIOController(null);
		}
		return instance;

	}

	/**
	 * @return Returns a list of {@link String} of found active ASIO drivers on the
	 *         system
	 */
	public static List<DriverInfo> getPossibleDrivers() {
		if (driverList == null) {
			driverList = loadPossibleDrivers();
		}
		return driverList;
	}

	private static List<DriverInfo> loadPossibleDrivers() {
		ArrayList<DriverInfo> result = new ArrayList<>();
		try {
			List<String> preList = AsioDriver.getDriverNames();
			// checking if connected
			for (String possibleDriver : preList) {
				AsioDriver tempDriver = null;
				try {
					tempDriver = AsioDriver.getDriver(possibleDriver);
					// adding if inputs avaliable
					if (tempDriver != null && tempDriver.getNumChannelsInput() > 0) {
						DriverInfo driverInfo = new DriverInfo(tempDriver);
						result.add(driverInfo);
					}
				} catch (Exception e) {
					LOG.debug(possibleDriver + " is unavailable");
				} finally {
					if (tempDriver != null && (!tempDriver.getName().equals(ASIOController.getInstance().getDevice()))) {
						tempDriver.shutdownAndUnloadDriver();
					}
				}
			}
		} catch (UnsatisfiedLinkError e) {
			LOG.warn("The corresponding library jasiohost64.dll was not found");
		}

		return result;
	}

	/**
	 * Constructor of the ASIOController
	 * 
	 * @param ioName Name of the chosen ASIO driver, should be chosen of the list
	 *               from {@link #getPossibleDrivers()}
	 */
	public ASIOController(final String ioName) {

		if (instance != null) {
			LOG.warn("Another driver already exists");
			shutdown();
		}
		LOG.info("Created ASIO Controller");
		driverName = ioName;
		instance = this;
		FileIO.registerSaveData(this);
		if (driverName != null) {
			restart();
			LOG.info("ASIO driver started");
			initFFT();
		}
		LOG.info("FFT Analysis started");
	}

	@Override
	public void bufferSwitch(final long sampleTime, final long samplePosition, final Set<AsioChannel> channels) {
		time = samplePosition;
		AsioChannel[] channelArray = channels.toArray(new AsioChannel[0]);
		for (int i = 0; i < channels.size(); i++) {
			AsioChannel channel = channelArray[i];
			try {
				if (channel.isInput() && channel.isActive() && exe != null) {
					// create new runnable task, for parallel execution
					Runnable runnable = () -> {
						try {
							float[] output = new float[bufferSize];
							if (activeChannel != null) {
								try {
									channel.read(output);
								} catch (BufferUnderflowException e1) {
									LOG.debug("Underflow Exception", e1);
								}

							}
							if (channelList != null) {
								Channel c = null;
								for (int j = 0; j < channelList.size(); j++) {
									Channel cTemp = channelList.get(j);
									if (cTemp.getChannel().equals(channel)) {
										c = cTemp;
										break;
									}
								}
								if (c != null) {
									c.setBuffer(output, samplePosition);
								}
							}
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					};
					exe.submit(runnable);
				}
			} catch (ConcurrentModificationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void add(final Object t) {
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
					channel.addListener(this);
					channel.setChannel(oldChannel.getChannel());
					for (Group g : groupList) {
						g.refreshChannels();
					}
				}
			}
		} else if (t instanceof Group) {
			Group g = (Group) t;
			if (!groupList.contains(g)) {
				groupList.add(g);
			}
			g.refreshChannels();
		}
	}

	/**
	 * Adds a {@link FFTListener} to the list of listeners that get notified by fft
	 * events, if that listener is not already an entry in that list
	 * 
	 * @param fftListener The listener to be added
	 */
	public void addFFTListener(final FFTListener fftListener) {
		if (!fftListeners.contains(fftListener)) {
			fftListeners.add(fftListener);
		}
	}

	/**
	 * Adds a group to the {@link ASIOController}, for the level of the group to be
	 * calculated by the controller
	 * 
	 * @param group The group to add to the list of groups
	 */
	public void addGroup(final Group group) {
		if (!groupList.contains(group)) {
			LOG.info("Group " + group.getName() + " added");
			groupList.add(group);
		}
	}

	@Override
	public void bufferSizeChanged(final int bufferSize) {
		LOG.info("Buffer size changed");
		restart();
	}

	@Override
	public void clear() {
		// do nothing
	}

	private void notifyFFTListeners(final double[][] spectrumMap) {
		for (int i = 0; i < fftListeners.size(); i++) {
			FFTListener l = fftListeners.get(i);
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
	}

	/**
	 * Returns the root frequency of the selected channel, which is prior calculated
	 * while applying the fft calculations
	 * 
	 * @return The root frequency of the selected channel
	 */
	public float getBaseFrequency() {
		return baseFrequency;
	}

	@Override
	public List<Input> getData() {
		ArrayList<Input> result = new ArrayList<>();
		result.addAll(getInputList());
		result.addAll(getGroupList());
		return result;
	}

	/**
	 * Returns the list of {@link Group} that get calculated
	 * 
	 * @return The list of {@link Group}
	 */
	public ArrayList<Group> getGroupList() {
		return new ArrayList<>(groupList);
	}

	/**
	 * Returns the list of {@link Channel} the ASIO driver provides
	 * 
	 * @return List of {@link Channel}
	 */
	public List<Channel> getInputList() {
		if (channelList == null) {
			channelList = new ArrayList<>();
			for (int i = 0; i < getNoOfInputs(); i++) {
				Channel channel = new Channel(asioDriver.getChannelInput(i));
				channelList.add(channel);
				channel.addListener(this);
			}
		}
		return channelList;
	}

	/**
	 * Calculates and returns the latency of the ASIO driver, calculated with the
	 * sample rate and the BufferSize of the {@link AsioDriver}
	 * 
	 * @return The calculated latency in milliseconds
	 */
	public double getLatency() {
		if (asioDriver != null) {
//			double inSec = (1.0 / asioDriver.getSampleRate()) * asioDriver.getBufferPreferredSize();
			double inSec = (1.0 / asioDriver.getSampleRate()) * bufferSize;
			return Math.round(inSec * 10000.0) / 10.0;
		}
		return 0;
	}

	/**
	 * Returns the number of input channels the ASIO driver reports
	 * 
	 * @return Number of inputs
	 */
	public int getNoOfInputs() {
		if (asioDriver != null) {
			return asioDriver.getNumChannelsInput();
		} else {
			return -1;
		}
	}

	private void initFFT() {
		// fftBufferSize = 16384;
		fftBufferSize = DESIRED_BUFFER_SIZE;
		bufferCount = 1;
		// fft = new DoubleFFT_1D(fftBufferSize);
		index = new int[bufferCount];
		for (int i = 0; i < bufferCount; i++) {
			index[i] = i * fftBufferSize / bufferCount;
		}
	}

	@Override
	public void latenciesChanged(final int inputLatency, final int outputLatency) {
		LOG.info("Latencies changed");
		restart();
	}

	/**
	 * Removes a listener from the list of {@link FFTListener} that get notified by
	 * fft events
	 * 
	 * @param listener The {@link FFTListener} that gets removed
	 */
	public void removeFFTListener(final FFTListener listener) {
		fftListeners.remove(listener);
	}

	/**
	 * Removes a group from the list of {@link Group} that get calculated
	 * 
	 * @param group The {@link Group} to be removed
	 */
	public void removeGroup(final Group group) {
		groupList.remove(group);
	}

	@Override
	public void resetRequest() {
		LOG.info("Reset requested");
		restart();
	}

	/**
	 * Restarts the {@link AsioDriver} with the set attributes
	 */
	public void restart() {
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
			FXMLMain.getInstance().close();
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
		restart();
	}

	@Override
	public void sampleRateDidChange(final double sampleRate) {
		LOG.info("Sample rate changed");
		restart();
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

	/**
	 * Sets the active {@link Channel} for which the FFT calculations are done
	 * 
	 * @param channel The {@link Channel} for the fft calculations
	 */
	public void setActiveChannel(final Channel channel) {
		activeChannel = channel;
	}

	/**
	 * Closes and unloads the {@link AsioDriver}
	 */
	public void shutdown() {
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
			FileIO.unregisterSaveData(this);
		}
	}

	/**
	 * Returns the buffer size of the driver
	 * 
	 * @return The buffersize
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Returns the current time, read from the {@link AsioDriver}
	 * 
	 * @return The time as {@link Long}
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Returns the sample rate of the active driver
	 * 
	 * @return samplerate
	 */
	public int getSampleRate() {
		return (int) Math.floor(sampleRate);
	}

	/**
	 * Sets the driver to reloaded once {@link #resetRequest()} is called
	 * 
	 * @param device
	 */
	public void setDevice(String device) {
		driverName = device;
	}

	/**
	 * Sets the buffersize, which gets only loaded once the driver ist restarted via
	 * {@link #restart()}
	 * 
	 * @param value The buffer size
	 */
	public void setBufferSize(int value) {
		if (value > 0 && value < 10000) {
			bufferSize = value;
		}
	}

	/**
	 * Returns the name of the loaded driver
	 * 
	 * @return The name of the active driver as {@link String}
	 */
	public String getDevice() {
		return driverName;
	}

	public boolean isLoaded() {
		return (getDevice() != null && getDevice().isEmpty());
	}

	public static List<String> getPossibleDriverStrings() {
		List<String> result = new ArrayList<String>();
		for (DriverInfo info : getPossibleDrivers()) {
			result.add(info.getName());
		}
		return result;
	}

	@Override
	public void levelChanged(Input input, double level, long time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nameChanged(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void colorChanged(String newColor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newBuffer(Channel channel, float[] buffer, long time) {
		// if channel is currently selected
		if (Objects.equal(channel.getChannelIndex(), activeChannel.getChannelIndex())) {
			if (!isFFTing) {
				isFFTing = true;

				double[][] spectrum = FFT.fftThis(activeChannel.getBuffer(), (float) sampleRate);
				notifyFFTListeners(spectrum);
				isFFTing = false;
			}
		}
	}
}
