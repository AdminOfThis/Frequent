package data;

import control.InputListener;
import gui.utilities.DrumTriggerListener;

public class DrumTrigger implements InputListener {

	public static final String[]	DEFAULT_NAMES	= new String[] { "Base", "Snare", "Tom1", "Tom2" };
	private String					name;
	private Channel					channel;
	private double					treshold;
	private DrumTriggerListener		obs;
	private boolean					below			= true;

	public DrumTrigger(final String name) {
		this.name = name;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getName() {
		return name;
	}

	public DrumTriggerListener getObs() {
		return obs;
	}

	public double getTreshold() {
		return treshold;
	}

	@Override
	public void levelChanged(final double level) {
		if (Channel.percentToDB(level) >= treshold && below) {
			if (obs != null) {
				obs.tresholdReached(level, treshold);
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

	public void setObs(final DrumTriggerListener obs) {
		this.obs = obs;
	}

	public void setTreshold(final double treshold) {
		this.treshold = treshold;
	}
}
