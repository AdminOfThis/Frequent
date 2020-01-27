package control.bpmdetect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.DrumTrigger;
import gui.utilities.DrumTriggerListener;

/**
 * 
 * @author AdminOfThis
 *
 */
public final class BeatDetector extends Thread implements DrumTriggerListener {

	private enum Mode {
		CLASSIC, BPM_DETECT
	};

	private static final Logger LOG = LogManager.getLogger(BeatDetector.class);
	private static final int LIST_SIZE = 20;
	private static BeatDetector instance;
	private static boolean initialized = false;
	private List<DrumTrigger> triggerList = Collections.synchronizedList(new ArrayList<>());
	private double bpm = 0;
	private Map<DrumTrigger, List<Long>> seriesMap = Collections.synchronizedMap(new HashMap<>());
	private Mode mode = Mode.BPM_DETECT;

	private BeatDetector() {
		setDaemon(true);
		start();
	}

	/**
	 * Returns the instance of the {@link BeatDetector}, and initializes a new one,
	 * if instance is currently null
	 * 
	 * @return The instance of {@link BeatDetector}
	 */
	public static BeatDetector getInstance() {
		if (instance == null) {
			instance = new BeatDetector();
		}
		return instance;
	}

	/**
	 * Initialized the Beat Detector once new triggers are set
	 */
	public static synchronized void initialize() {
		if (!initialized) {
			for (String s : DrumTrigger.DEFAULT_NAMES) {
				DrumTrigger trigger = new DrumTrigger(s);
				trigger.addListeners(getInstance());
				getInstance().addDrumTrigger(trigger);
			}
			LOG.info("BeatDetector initialized");
			initialized = true;
		}
	}

	/**
	 * Returns wether the {@link BeatDetector} is initialized.
	 * 
	 * @return #true if the {@link BeatDetector} is initialized, otherwise false
	 */
	public static synchronized boolean isInitialized() {
		return initialized;
	}

	/**
	 * Adds a new channel as drum trigger
	 * 
	 * @param trigger The trigger to add
	 */
	public void addDrumTrigger(DrumTrigger trigger) {
		synchronized (triggerList) {
			if (!triggerList.contains(trigger)) {
				triggerList.add(trigger);
			}
		}
	}

	/**
	 * Returns the detected bpm
	 * 
	 * @return The detected bpm
	 */
	public double getBPM() {
		return bpm;
	}

	/**
	 * Returns the BPM as string, with one decimal-point
	 * 
	 * @return The detected BPM as String
	 */
	public String getBPMString() {
//		return Double.toString(Math.round(bpm * 10.0) / 10.0);
		return Double.toString(Math.round(bpm));
	}

	/**
	 * Returns the list of all triggers.
	 * 
	 * @return The list of all triggers
	 */
	public List<DrumTrigger> getTriggerList() {
		synchronized (triggerList) {
			return new ArrayList<>(triggerList);
		}
	}

	/**
	 * Removes a trigger if it is contained in the trigger list
	 * 
	 * @param trigger The trigger to remove
	 */
	public void removeDrumTrigger(DrumTrigger trigger) {
		synchronized (triggerList) {
			triggerList.remove(trigger);
		}
	}

	@Override
	public void run() {
		while (true) {
			if (mode == Mode.CLASSIC) {
				calcBPM();
				clearData();
				Thread.yield();
			} else if (mode == Mode.BPM_DETECT) {
				synchronized (triggerList) {

					for (DrumTrigger trigger : triggerList) {
						trigger.calcBPM();
					}

				}
				bpm = BPMBestGuess.getInstance().getBPM();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
		}
	}

	@Override
	public void tresholdReached(DrumTrigger trigger, double level, double treshold, long time) {

		long timeInMs = (time * (ASIOController.getInstance().getSampleRate() / ASIOController.getInstance().getBufferSize())) * 50;
		synchronized (seriesMap) {
			if (seriesMap.get(trigger) == null) {
				seriesMap.put(trigger, Collections.synchronizedList(new ArrayList<>()));
			}
			synchronized (seriesMap.get(trigger)) {
				seriesMap.get(trigger).add(timeInMs);
			}
		}
	}

	private void calcBPM() {
		double seriesMean = 0;
		synchronized (seriesMap) {
			for (List<Long> list : seriesMap.values()) {
				double deltaMean = 0;
				synchronized (list) {
					if (list.size() >= 2) {
						for (int i = 1; i < list.size(); i++) {
							double singleDelta = list.get(i) - list.get(i - 1);
							deltaMean += singleDelta;
						}
						deltaMean = deltaMean / (list.size() - 1);
					}
				}
				seriesMean += deltaMean;
			}
			seriesMean = seriesMean / seriesMap.size();
		}
		// convertion vom nanos to millis, and dividing by 60 to get bpm
		bpm = 1.0 / (seriesMean / 1000000000L / 60);
	}

	private void clearData() {
		synchronized (seriesMap) {
			for (Entry<DrumTrigger, List<Long>> entry : seriesMap.entrySet()) {
				synchronized (entry.getValue()) {
					if (entry.getValue().size() >= LIST_SIZE) {
						List<Long> list = entry.getValue();
						seriesMap.put(entry.getKey(), list.subList(list.size() - LIST_SIZE, list.size()));
					}
				}
			}
		}
	}
}
