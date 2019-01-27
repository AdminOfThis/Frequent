package data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import com.synthbot.jasiohost.AsioChannel;

import control.ASIOController;
import control.ChannelListener;
import control.InputListener;

public class Channel extends Input implements Comparable<Channel>, Comparator<Channel> {

	private static final long		serialVersionUID	= 1L;
	private transient AsioChannel	channel;
	private int						channelIndex		= -1;
	private Group					group;
	private boolean					hide				= false;
	private float[]					buffer;
	private float[]					bufferFull			= new float[ASIOController.DESIRED_BUFFER_SIZE];
	private Channel					stereoChannel;

	public Channel() {
		this(null, null);
	}

	public Channel(final AsioChannel channel) {
		this(channel, null);
	}

	public Channel(final AsioChannel channel, final String name) {
		if (channel != null) {
			this.channel = channel;
			setName(channel.getChannelName());
			channelIndex = channel.getChannelIndex();
		}
		if (name != null) {
			setName(name);
		}
	}

	public Channel(final String name) {
		this(null, name);
	}

	public static double percentToDB(final double level) {
		return 20.0 * Math.log10(level /* / 1000.0 */);
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
			if (super.equals(obj) && Objects.equals(getChannelIndex(), other.getChannelIndex())) {
				// if groups don't exist on both
				if (getGroup() == null && other.getGroup() == null) {
					return true;
				}
				// if groups exist on both
				else if (getGroup() != null && other.getGroup() != null) {
					// check for group equality
					return Objects.equals(getGroup().getName(), other.getGroup().getName());
				} else {
					// if only one has a group
					return false;
				}
			}
		}
		return false;
	}

	public float[] getBuffer() {
		return Arrays.copyOf(bufferFull, bufferFull.length);
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

	private void removeStereoChannel() {
		stereoChannel = null;
	}

	public void resetName() {
		if (channel != null) {
			setName(channel.getChannelName());
		}
	}

	public void setBuffer(final float[] addition, final long time) {
		if (buffer == null) {
			buffer = addition;
		} else {
			int newBufferSize = Math.min(ASIOController.DESIRED_BUFFER_SIZE, buffer.length + addition.length);
			final int lengthBefore = buffer.length;
			buffer = Arrays.copyOf(buffer, newBufferSize);
			int count = 0;
			while (count + lengthBefore < newBufferSize) {
				buffer[lengthBefore + count] = addition[count];
				count++;
			}
			if (buffer.length >= ASIOController.DESIRED_BUFFER_SIZE) {
				bufferFull = Arrays.copyOf(buffer, buffer.length);
				sendFullBuffer(time);
				buffer = new float[addition.length - count];
				int tempCount = 0;
				while (count < addition.length) {
					buffer[tempCount] = addition[count];
					tempCount++;
					count++;
				}
			}
		}
	}

	public void sendFullBuffer(long time) {
		float max = 0;
		for (float f : bufferFull) {
			if (f > max) {
				max = f;
			}
		}
		setLevel(max, time);
		synchronized (getListeners()) {
			for (InputListener l : getListeners()) {
				if (l instanceof ChannelListener) {
					// new Thread(() -> ((ChannelListener)
					// l).newBuffer(buffer)).start();
					((ChannelListener) l).newBuffer(this, bufferFull, time);
				}
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
			// if old stereochannel not null
			if (stereoChannel != null) {
				// remove reference to this on other channel
				stereoChannel.removeStereoChannel();
			}
			// setting channel
			stereoChannel = newChannel;
			// if new channel is not null and does not already reference this as
			// his pair
			if (stereoChannel != null && !Objects.equals(stereoChannel.getStereoChannel(), this)) {
				// setting channel
				stereoChannel.setStereoChannel(this);
			}
		}
	}
}
