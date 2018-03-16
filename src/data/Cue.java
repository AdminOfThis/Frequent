package data;

import com.synthbot.jasiohost.AsioChannel;

public class Cue {

	private String		name;
	private AsioChannel	channelToSelect;
	private long		time;


	public Cue(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public AsioChannel getChannelToSelect() {
		return channelToSelect;
	}


	public void setChannelToSelect(AsioChannel channelToSelect) {
		this.channelToSelect = channelToSelect;
	}


	public long getTime() {
		return time;
	}


	public void setTime(long time) {
		this.time = time;
	}


}
