package control.bpmdetect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import data.DrumTrigger;
import gui.utilities.DrumTriggerListener;

public final class BeatDetector extends Thread implements DrumTriggerListener {

	private enum Mode {
		CLASSIC, BPM_DETECT
	};

	private static final Logger				LOG			= Logger.getLogger(BeatDetector.class);
	private static final int				LIST_SIZE	= 20;
	private static BeatDetector				instance;
	private static boolean					initialized	= false;
	private List<DrumTrigger>				triggerList	= Collections.synchronizedList(new ArrayList<>());
	private double							bpm			= 0;
	private Map<DrumTrigger, List<Long>>	seriesMap	= Collections.synchronizedMap(new HashMap<>());
	private Mode							mode		= Mode.CLASSIC;

	public static BeatDetector getInstance() {
		if (instance == null) {
			instance = new BeatDetector();
		}
		return instance;
	}

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

	private BeatDetector() {
		setDaemon(true);
		start();
	}

	public void addDrumTrigger(DrumTrigger trigger) {
		synchronized (triggerList) {
			if (!triggerList.contains(trigger)) {
				triggerList.add(trigger);
			}
		}
	}

	public void removeDrumTrigger(DrumTrigger trigger) {
		synchronized (triggerList) {
			triggerList.remove(trigger);
		}
	}

	public List<DrumTrigger> getTriggerList() {
		synchronized (triggerList) {
			return new ArrayList<>(triggerList);
		}
	}

	public double getBPM() {
		return bpm;
	}

	@Override
	public void run() {
		while (true) {
			if (mode == Mode.CLASSIC) {
				calcBPM();
				clearData();
				Thread.yield();
			} else if (mode == Mode.BPM_DETECT) {
				for (DrumTrigger trigger : triggerList) {
					trigger.calcBPM();
				}
				bpm = BPMBestGuess.getInstance().getBPM();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
		bpm = 1.0 / (seriesMean / 1000000000l / 60);
	}

	@Override
	public void tresholdReached(DrumTrigger trigger, double level, double treshold, long time) {
		synchronized (seriesMap) {
			if (seriesMap.get(trigger) == null) {
				seriesMap.put(trigger, Collections.synchronizedList(new ArrayList<>()));
			}
			synchronized (seriesMap.get(trigger)) {
				seriesMap.get(trigger).add(time);
			}
		}
	}

	public static synchronized boolean isInitialized() {
		return initialized;
	}

	public String getBPMString() {
		return Double.toString(Math.round(bpm * 10.0) / 10.0);
	}
}
