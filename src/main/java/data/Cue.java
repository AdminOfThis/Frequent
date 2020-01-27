package data;

import java.io.Serializable;

public class Cue implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private String				name;
	private Channel				channelToSelect;
	private long				time;


	public Cue(String name) {
		this.name = name;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Cue) {
			Cue other = (Cue) obj;
			if (this.name.equals(other.name)) {
				if (this.channelToSelect == null && other.channelToSelect == null) {
					return true;
				} else if (this.channelToSelect != null && this.channelToSelect.equals(other.channelToSelect)) {
					return true;
				}
			}
		}
		return false;
	}


	public Channel getChannelToSelect() {
		return channelToSelect;
	}


	public String getName() {
		return name;
	}


	public long getTime() {
		return time;
	}


	public void setChannelToSelect(Channel channelToSelect) {
		this.channelToSelect = channelToSelect;
	}


	public void setName(String name) {
		this.name = name;
	}


	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return name;
	}

}
