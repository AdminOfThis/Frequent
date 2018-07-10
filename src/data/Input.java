package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import control.LevelListener;

public abstract class Input implements Serializable {

	private static final long				serialVersionUID	= 1L;
	private static final Logger				LOG					= Logger.getLogger(Input.class);
	private String							name;
	private float							level				= 0;
	private transient List<LevelListener>	observerList		= new ArrayList<>();
	private String							hexColor;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addObserver(LevelListener obs) {
		if (observerList == null) {
			observerList = new ArrayList<>();
		}
		if (!observerList.contains(obs)) {
			observerList.add(obs);
		}
	}

	public void removeObserver(LevelListener obs) {
		observerList.remove(obs);
	}

	protected void notifyObservers() {
		if (observerList == null) {
			observerList = new ArrayList<>();
		}
		for (LevelListener obs : observerList) {
			// new Thread(new Runnable() {
			//
			// @Override
			// public void run() {
			try {
				obs.levelChanged(level);
			} catch (Exception e) {
				LOG.warn("Unable to notify Level Listener");
				LOG.debug("", e);
			}
			// }
			// }).start();
		}
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
		notifyObservers();
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
