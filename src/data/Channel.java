package data;

import com.synthbot.jasiohost.AsioChannel;

public class Channel {
	private AsioChannel	channel;
	private String		name;


	public Channel(AsioChannel channel) {
		this.channel = channel;
		this.name = channel.getChannelName();
	}

	public Channel(AsioChannel channel, String name) {
		this.channel = channel;
		this.name = name;
	}

	public AsioChannel getChannel() {
		return channel;
	}

	public void setChannel(AsioChannel channel) {
		this.channel = channel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
