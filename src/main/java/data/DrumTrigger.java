package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import control.InputListener;
import gui.utilities.DrumTriggerListener;

public class DrumTrigger implements InputListener {

	public static final String[]		DEFAULT_NAMES	= new String[] { "Base", "Snare", "Tom1", "Tom2" };
	private String						name;
	private Channel						channel;
	private double						treshold;
	private List<DrumTriggerListener>	listeners		= Collections.synchronizedList(new ArrayList<>());
	private boolean						below			= true;

	public DrumTrigger(final String name) {
		this.name = name;
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
	public void levelChanged(final double level, long time) {
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
}
