package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import control.InputListener;

public abstract class Input implements Serializable {

	private static final long				serialVersionUID	= 1L;
	private static final Logger				LOG					= Logger.getLogger(Input.class);
	private String							name;
	private float							level				= 0;
	private transient List<InputListener>	listeners			= new ArrayList<>();
	private String							hexColor;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addListener(InputListener obs) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		if (!listeners.contains(obs)) {
			listeners.add(obs);
		}
	}

	public void removeListener(InputListener obs) {
		listeners.remove(obs);
	}

	protected void notifyListeners() {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		for (InputListener obs : listeners) {
			// new Thread(new Runnable() {
			//
			// @Override
			// public void run() {
			try {
				obs.levelChanged(level, this);
			} catch (Exception e) {
				LOG.warn("Unable to notify Level Listener");
				LOG.debug("", e);
			}
			// }
			// }).start();
		}
	}

	protected List<InputListener> getListeners() {
		return listeners;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
		notifyListeners();
	}

	public boolean setColor(String color) {
		// trying to parsse string to make sure its a hex string
		try {
			if (color.startsWith("#")) {
				color = color.substring(1);
			}
			Long.parseLong(color, 16);
			hexColor = "#" + color;
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

	public String getColor() {
		return hexColor;
	}
}
