package data;

import java.util.Comparator;
import java.util.Objects;

import com.synthbot.jasiohost.AsioChannel;

import control.ChannelListener;
import control.InputListener;

public class Channel extends Input implements Comparable<Channel>, Comparator<Channel> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static double percentToDB(final double level) {
		return 20.0 * Math.log10(level /* / 1000.0 */);
	}

	/**
	 *
	 */
	private transient AsioChannel	channel;
	private int						channelIndex	= -1;
	private Group					group;
	private boolean					hide			= false;
	private float[]					buffer;

	private Channel					stereoChannel;

	public Channel(final AsioChannel channel) {
		this(channel, channel.getChannelName());
	}

	public Channel(final AsioChannel channel, final String name) {
		channelIndex = channel.getChannelIndex();
		this.channel = channel;
		setName(name);
	}

	@Override
	public int compare(final Channel o1, final Channel o2) {
		return o1.getChannelIndex() - o2.getChannelIndex();
	}

	@Override
	public int compareTo(final Channel o) {
		return getChannelIndex() - o.getChannelIndex();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Channel) {
			Channel other = (Channel) obj;
			if (super.equals(obj))
				return Objects.equals(getChannelIndex(), other.getChannelIndex())
				        && Objects.equals(getGroup(), other.getGroup());
		}
		return false;
	}

	public float[] getBuffer() {
		return buffer;
	}

	public AsioChannel getChannel() {
		return channel;
	}

	public int getChannelIndex() {
		return channelIndex;
	}

	public Group getGroup() {
		return group;
	}

	public Channel getStereoChannel() {
		return stereoChannel;
	}

	public boolean isHidden() {
		return hide;
	}

	public void resetName() {
		if (channel != null) {
			setName(channel.getChannelName());
		}
	}

	public void setBuffer(final float[] buffer) {
		this.buffer = buffer;
		for (InputListener l : getListeners()) {
			if (l instanceof ChannelListener) {
				// new Thread(() -> ((ChannelListener)
				// l).newBuffer(buffer)).start();
				((ChannelListener) l).newBuffer(buffer);
			}
		}
	}

	public void setChannel(final AsioChannel channel) {
		this.channel = channel;
		if (channel != null) {
			channelIndex = channel.getChannelIndex();
		} else {
			channelIndex = -1;
		}
	}

	protected void setGroup(final Group group) {
		this.group = group;
	}

	public void setHidden(final boolean hide) {
		this.hide = hide;
	}

	public void setStereoChannel(final Channel newChannel) {
		// if stereo channel is not already equal
		if (!Objects.equals(newChannel, stereoChannel)) {
			if (stereoChannel != null && equals(stereoChannel.getStereoChannel())) {
				Channel oldChannel = stereoChannel;
				stereoChannel = null;
				oldChannel.setStereoChannel(null);
			}
			stereoChannel = newChannel;
			if (stereoChannel != null && !stereoChannel.getStereoChannel().equals(this)) {
				stereoChannel.setStereoChannel(this);
			}
		}
	}
}
