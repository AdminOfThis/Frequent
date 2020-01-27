package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import control.ASIOController;
import control.InputListener;
import control.bpmdetect.BPMDetect;
import gui.utilities.DrumTriggerListener;

public class DrumTrigger implements InputListener {

	public static final String[] DEFAULT_NAMES = new String[] { "Base", "Snare", "Tom1", "Tom2" };
	private static final int BELOW_DELTA = 5;
	private String name;
	private Channel channel;
	private double treshold;
	private List<DrumTriggerListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private int below = 0;
	private BPMDetect bpmDetect;

	public DrumTrigger(final String name) {
		this.name = name;
		this.bpmDetect = new BPMDetect();
	}

	public void addListeners(final DrumTriggerListener obs) {
		synchronized (listeners) {
			if (!listeners.contains(obs)) {
				this.listeners.add(obs);
			}
		}
	}

	public void calcBPM() {
		if (getChannel() != null) {
			bpmDetect.detect(getChannel().getBuffer(), ASIOController.getInstance().getSampleRate());
		}
	}

	@Override
	public void colorChanged(String newColor) {}

	public Channel getChannel() {
		return channel;
	}

	public String getName() {
		return name;
	}

	public List<DrumTriggerListener> getObs() {
		return listeners;
	}

	public double getTreshold() {
		return treshold;
	}

	@Override
	public void levelChanged(final Input channel, final double level, long time) {
		double leveldB = Channel.percentToDB(level);
		if (leveldB >= treshold && below > BELOW_DELTA) {

			synchronized (listeners) {
				for (DrumTriggerListener obs : listeners) {
					obs.tresholdReached(this, level, treshold, time);
				}
			}
			below = 0;
		} else {
			below++;
		}
	}

	@Override
	public void nameChanged(String name) {}

	public void removeListeners(DrumTriggerListener obs) {
		synchronized (listeners) {
			listeners.remove(obs);
		}
	}

	public void setChannel(final Channel channel) {
		if (this.channel != null) {
			this.channel.removeListener(this);
		}
		this.channel = channel;
		if (this.channel != null) {
			this.channel.addListener(this);
		}
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setTreshold(final double treshold) {
		this.treshold = treshold;
	}
}
