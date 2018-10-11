package data;

import static data.FileIO.compareAndNullCheck;

import java.util.Comparator;

import com.synthbot.jasiohost.AsioChannel;

import control.ChannelListener;
import control.InputListener;

public class Channel extends Input implements Comparable<Channel>, Comparator<Channel> {

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 1L;
	/**
	 * 
	 */
	private transient AsioChannel	channel;
	private int						channelIndex		= -1;
	private Group					group;
	private boolean					hide				= false;
	private float[]					buffer;
	private Channel					stereoChannel;


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

	protected void setGroup(Group group) {
		this.group = group;
	}

	public void resetName() {
		if (channel != null) {
			setName(channel.getChannelName());
		}
	}

	public boolean isHidden() {
		return hide;
	}

	public void setHidden(boolean hide) {
		this.hide = hide;
	}

	public static double percentToDB(double level) {
		return 20.0 * Math.log10(level /* / 1000.0 */);
	}

	@Override
	public int compareTo(Channel o) {
		return this.getChannelIndex() - o.getChannelIndex();
	}

	@Override
	public int compare(Channel o1, Channel o2) {
		return o1.getChannelIndex() - o2.getChannelIndex();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Channel) {
			Channel other = (Channel) obj;
			if (super.equals(obj)) {
				if (compareAndNullCheck(this.getChannelIndex(), other.getChannelIndex())
					&& compareAndNullCheck(this.getGroup(), other.getGroup())) {
					return true;
				}
			}
		}
		return false;
	}

	public float[] getBuffer() {
		return buffer;
	}

	public void setBuffer(float[] buffer) {
		this.buffer = buffer;
		for (InputListener l : getListeners()) {
			if (l instanceof ChannelListener) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						((ChannelListener) l).newBuffer(buffer);
					}
				}).start();
			}
		}
	}

	public Channel getStereoChannel() {
		return stereoChannel;
	}

	public void setStereoChannel(Channel stereoChannel) {
		this.stereoChannel = stereoChannel;
	}
}
