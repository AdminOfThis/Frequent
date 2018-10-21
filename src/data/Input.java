package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import control.InputListener;

public abstract class Input implements Serializable {

	private static final long				serialVersionUID	= 1L;
	private static final Logger				LOG					= Logger.getLogger(Input.class);
	public static final Comparator<Input>	COMPARATOR			= (o1, o2) -> {
																	if (o1 instanceof Channel && o2 instanceof Channel)
																		return ((Channel) o1).getChannelIndex()
																		        - ((Channel) o2).getChannelIndex();
																	else if (o1 instanceof Group && o2 instanceof Group)
																		return ((Group) o1).getName()
																		        .compareTo(((Group) o2).getName());
																	return 0;
																};
	private String							name;
	private float							level				= 0;
	private transient List<InputListener>	listeners			= Collections.synchronizedList(new ArrayList<>());
	private String							hexColor;

	public Input() {

	}

	public void addListener(final InputListener obs) {
		if (!listeners.contains(obs)) {
			synchronized (listeners) {
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

	protected List<InputListener> getListeners() {
		return listeners;
	}

	public String getName() {
		return name;
	}

	protected void notifyListeners() {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList<>());
		}
		synchronized (listeners) {
			for (InputListener obs : listeners) {
				// new Thread(new Runnable() {
				//
				// @Override
				// public void run() {
				try {
					obs.levelChanged(level);
				} catch (Exception e) {
					LOG.warn("Unable to notify Level Listener", e);
					LOG.debug("", e);
				}
				// }
				// }).start();
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

	public void setLevel(final float level) {
		this.level = level;
		notifyListeners();
	}

	public void setName(final String name) {
		this.name = name;
	}
}
