package data;

import java.util.Arrays;
import java.util.Objects;

import com.synthbot.jasiohost.AsioChannel;

import control.ASIOController;
import control.ChannelListener;
import control.InputListener;

public class Channel extends Input implements Comparable<Channel> {

	private static final long serialVersionUID = 1L;
	public static double percentToDB(final double level) {
		return 20.0 * Math.log10(level /* / 1000.0 */);
	}
	private transient AsioChannel channel;
	private int channelIndex = -1;
	private Group group;
	private boolean hide = false;
	private float[] buffer;
	private float[] bufferFull = new float[ASIOController.DESIRED_BUFFER_SIZE];

	private Channel stereoChannel;

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

	@Override
	public int compareTo(final Channel o) {
		return getChannelIndex() - o.getChannelIndex();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Channel) {
			Channel other = (Channel) obj;
			return super.equals(obj) && Objects.equals(getChannelIndex(), other.getChannelIndex()) && Objects.equals(getGroup(), other.getGroup());
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

	public boolean isLeftChannel() {
		return getStereoChannel() != null && compareTo(getStereoChannel()) < 0;
	}

	public boolean isStereo() {
		return getStereoChannel() != null;
	}

	public void resetName() {
		if (channel != null) {
			setName(channel.getChannelName());
		}
	}

	public void sendFullBuffer(long time) {
		float max = 0;
		float rms = 0;
		for (float f : bufferFull) {
			rms += f * f;
			if (f > max) {
				max = f;
			}
		}
		rms = (float) Math.sqrt(rms / bufferFull.length);
		setLevel(max, rms, time);
		synchronized (getListeners()) {
			for (int i = 0; i < getListeners().size(); i++) {
				InputListener l = getListeners().get(i);
				if (l instanceof ChannelListener) {
					new Thread(() -> ((ChannelListener) l).newBuffer(this, bufferFull, time)).start();
				}
			}
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

	public void setChannel(final AsioChannel channel) {
		this.channel = channel;
		if (channel != null) {
			channelIndex = channel.getChannelIndex();
		} else {
			channelIndex = -1;
		}
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

	private void removeStereoChannel() {
		stereoChannel = null;
	}

	protected void setGroup(final Group group) {
		this.group = group;
	}
}
