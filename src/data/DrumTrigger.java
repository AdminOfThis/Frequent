package data;

public class DrumTrigger implements LevelObserver {

	public static final String[]	DEFAULT_NAMES	= new String[] { "Base", "Snare", "Tom1", "Tom2" };
	private String					name;
	private Channel					channel;
	private double					treshold;
	private DrumTriggerObserver		obs;
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
			channel.removeObserver(this);
		}
		this.channel = channel;
		if (this.channel != null) {
			this.channel.addObserver(this);
		}
	}

	public double getTreshold() {
		return treshold;
	}

	public void setTreshold(double treshold) {
		this.treshold = treshold;
	}

	@Override
	public void levelChanged(double level) {
		if (Channel.percentToDB(level * 1000.0) >= treshold && below) {
			if (obs != null) {
				obs.tresholdReached(level, treshold);
			}
			below = false;
		} else {
			below = true;
		}
	}

	public DrumTriggerObserver getObs() {
		return obs;
	}

	public void setObs(DrumTriggerObserver obs) {
		this.obs = obs;
	}
}
