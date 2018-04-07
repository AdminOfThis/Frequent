package data;

import java.io.Serializable;

import com.synthbot.jasiohost.AsioChannel;

public class Channel implements Serializable {

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 1L;
	private transient AsioChannel	channel;
	private int						channelIndex		= -1;
	private String					name;
	private Group					group;
	private float					level				= 0;
	private LevelObserver			observer;

	public Channel(AsioChannel channel) {
		this(channel, channel.getChannelName());
	}

	public Channel(AsioChannel channel, String name) {
		this.channelIndex = channel.getChannelIndex();
		this.channel = channel;
		this.name = name;
	}

	public AsioChannel getChannel() {
		return channel;
	}

	public void setChannel(AsioChannel channel) {
		this.channel = channel;
		if (channel != null) {
			this.channelIndex = channel.getChannelIndex();
		} else {
			channelIndex = -1;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
		if (observer != null) {
			observer.levelChanged(level);
		}
	}

	public int getChannelIndex() {
		return channelIndex;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		if (this.group != group) {
			this.group = group;
		}
		if (group != null && !group.getChannelList().contains(this)) {
			group.addChannel(this);
		}
	}

	public LevelObserver getObserver() {
		return observer;
	}

	public void setObserver(LevelObserver observer) {
		this.observer = observer;
	}

	public void resetName() {
		if (channel != null) {
			name = channel.getChannelName();
		}
	}

	public static double percentToDB(double level) {
		return 20.0 * Math.log10(level / 1000.0);
	}
}
