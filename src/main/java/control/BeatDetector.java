package control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import data.DrumTrigger;
import gui.utilities.DrumTriggerListener;

public class BeatDetector extends Thread implements DrumTriggerListener {

	private static final Logger					LOG			= Logger.getLogger(BeatDetector.class);
	private static BeatDetector					instance;
	private static boolean						initialized	= false;
	private List<DrumTrigger>					triggerList	= Collections.synchronizedList(new ArrayList<>());
	private double								bpm			= 0;
	private Map<DrumTrigger, ArrayList<Long>>	seriesMap	= Collections.synchronizedMap(new HashMap<>());

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
		while (true) {}
	}

	@Override
	public void tresholdReached(DrumTrigger trigger, double level, double treshold, long time) {
		synchronized (seriesMap) {
			if (seriesMap.get(trigger) == null) {
				seriesMap.put(trigger, new ArrayList<>());
			}
			seriesMap.get(trigger).add(time);
		}
	}

	public static synchronized boolean isInitialized() {
		return initialized;
	}
}
