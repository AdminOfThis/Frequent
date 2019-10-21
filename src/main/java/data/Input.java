package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.InputListener;

public abstract class Input implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LogManager.getLogger(Input.class);

	private String name;
	private float level, rmsLevel;
	private long time;

	private Input stereoChannel;
	private transient List<InputListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private String hexColor;

	public Input() {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList<>());
		}
	}

	public void addListener(final InputListener obs) {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList<>());
		}
		synchronized (listeners) {
			if (!listeners.contains(obs)) {
				listeners.add(obs);
			}
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Input) {
			Input other = (Input) obj;
			return Objects.equals(getName(), other.getName());
		}
		return false;
	}

	public String getColor() {
		return hexColor;
	}

	public float getLevel() {
		return level;
	}

	public float getRMSLevel() {
		return rmsLevel;
	}

	protected List<InputListener> getListeners() {
		return listeners;
	}

	public String getName() {
		if (isStereo()) {
			if (Objects.equals(this, ((Channel) this).getLeftChannel())) {
				return name + " L";
			} else {
				return name + " R";

			}
		}
		return name;
	}

	protected String getCleanName() {
		return name;
	}

	protected void notifyListeners() {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList<>());
		}
		synchronized (listeners) {
			for (InputListener obs : listeners) {
				try {
					obs.levelChanged(this, level, time);
				} catch (Exception e) {
					LOG.warn("Unable to notify Level Listener", e);
					LOG.debug("", e);
				}
			}
		}
	}

	public void removeListener(final InputListener obs) {
		synchronized (listeners) {
			listeners.remove(obs);
		}
	}

	public boolean setColor(final String colorIn) {
		// trying to parse string to make sure its a hex string
		String color = colorIn;
		try {
			if (color.startsWith("#")) {
				color = color.substring(1);
			}
			Long.parseLong(color, 16);
			hexColor = "#" + color;
			if (isStereo() && !Objects.equals(getStereoChannel().getColor(), hexColor)) {
				getStereoChannel().setColor(hexColor);
			}

			if (this instanceof Group) {
				Group g = (Group) this;
				for (Channel c : g.getChannelList()) {
					c.setColor(hexColor);
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	protected void setLevel(final float level, long time) {
		if (level > 0) {
			this.level = level;
			this.time = time;
			notifyListeners();
		}
	}

	protected void setLevel(final float level, final float rms, long time) {
		rmsLevel = rms;
		setLevel(level, time);
	}

	public void setName(final String name) {
		this.name = name;
		if (isStereo() && !Objects.equals(getStereoChannel().getName(), getName())) {
			getStereoChannel().setName(name);
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public Input getStereoChannel() {
		return stereoChannel;
	}

	private void removeStereoChannel() {
		stereoChannel = null;
	}

	public void setStereoChannel(final Input newChannel) {
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

	public boolean isStereo() {
		return getStereoChannel() != null;
	}
}
