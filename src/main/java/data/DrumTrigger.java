package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import control.ASIOController;
import control.InputListener;
import control.bpmdetect.BPMDetect;
import gui.utilities.DrumTriggerListener;

public class DrumTrigger implements InputListener {

	public static final String[]		DEFAULT_NAMES	= new String[] { "Base"/*, "Snare", "Tom1", "Tom2"*/ };
	private String						name;
	private Channel						channel;
	private double						treshold;
	private List<DrumTriggerListener>	listeners		= Collections.synchronizedList(new ArrayList<>());
	private boolean						below			= true;
	private BPMDetect					bpmDetect;

	public DrumTrigger(final String name) {
		this.name = name;
		this.bpmDetect = new BPMDetect();
	}

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
		if (leveldB >= treshold && below) {
			synchronized (listeners) {
				for (DrumTriggerListener obs : listeners) {
					obs.tresholdReached(this, level, treshold, time);
				}
			}
			below = false;
		} else {
			below = true;
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

	public void addListeners(final DrumTriggerListener obs) {
		synchronized (listeners) {
			if (!listeners.contains(obs)) {
				this.listeners.add(obs);
			}
		}
	}

	public void removeListeners(DrumTriggerListener obs) {
		synchronized (listeners) {
			listeners.remove(obs);
		}
	}

	public void setTreshold(final double treshold) {
		this.treshold = treshold;
	}

	public void calcBPM() {
		if (ASIOController.getInstance() != null && getChannel() != null) {
			bpmDetect.detect(getChannel().getBuffer(), ASIOController.getInstance().getSampleRate());
		}
	}
}
