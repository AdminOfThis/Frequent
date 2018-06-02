package data;

import java.util.Comparator;

import com.synthbot.jasiohost.AsioChannel;

public class Channel extends Input {

	/**
	 * 
	 */
	private static final long				serialVersionUID	= 1L;
	public static final Comparator<Channel>	COMPARATOR			= new Comparator<Channel>() {

																	@Override
																	public int compare(Channel o1, Channel o2) {
																		// TODO Auto-generated method stub
																		return o1.getChannelIndex() - o2.getChannelIndex();
																	}
																};
	private transient AsioChannel			channel;
	private int								channelIndex		= -1;
	private Group							group;

	public Channel(AsioChannel channel) {
		this(channel, channel.getChannelName());
	}

	public Channel(AsioChannel channel, String name) {
		this.channelIndex = channel.getChannelIndex();
		this.channel = channel;
		setName(name);
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

	public void resetName() {
		if (channel != null) {
			setName(channel.getChannelName());
		}
	}

	public static double percentToDB(double level) {
		return 20.0 * Math.log10(level / 1000.0);
	}
}
