package data;

import com.synthbot.jasiohost.AsioChannel;

public class Cue {

	private String	name;
	private Channel	channelToSelect;
	private long	time;


	public Cue(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Channel getChannelToSelect() {
		return channelToSelect;
	}


	public void setChannelToSelect(Channel channelToSelect) {
		this.channelToSelect = channelToSelect;
	}


	public long getTime() {
		return time;
	}


	public void setTime(long time) {
		this.time = time;
	}


}
