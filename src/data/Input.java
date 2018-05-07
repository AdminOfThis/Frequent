package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public abstract class Input implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private static final Logger	LOG					= Logger.getLogger(Input.class);
	private String				name;
	private float				level				= 0;
	private List<LevelObserver>	observerList		= new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addObserver(LevelObserver obs) {
		if (!observerList.contains(obs)) {
			observerList.add(obs);
		}
	}

	public void removeObserver(LevelObserver obs) {
		observerList.remove(obs);
	}

	protected void notifyObservers() {
		for (LevelObserver obs : observerList) {
// new Thread(new Runnable() {
//
// @Override
// public void run() {
			try {
				obs.levelChanged(level);
			}
			catch (Exception e) {
				LOG.warn("", e);
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
}
