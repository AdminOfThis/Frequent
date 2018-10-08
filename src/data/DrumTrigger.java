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

	public DrumTrigger(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		if (this.channel != null) {
			channel.removeListener(this);
		}
		this.channel = channel;
		if (this.channel != null) {
			this.channel.addListener(this);
		}
	}

	public double getTreshold() {
		return treshold;
	}

	public void setTreshold(double treshold) {
		this.treshold = treshold;
	}

	@Override
	public void levelChanged(double level, Input in) {
		if (Channel.percentToDB(level) >= treshold && below) {
			if (obs != null) {
				obs.tresholdReached(level, treshold);
			}
			below = false;
		} else {
			below = true;
		}
	}

	public DrumTriggerListener getObs() {
		return obs;
	}

	public void setObs(DrumTriggerListener obs) {
		this.obs = obs;
	}
}
