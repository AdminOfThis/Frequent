package data;

import java.io.Serializable;

import com.synthbot.jasiohost.AsioChannel;

public class Channel implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;


	private AsioChannel			channel;
	private String				name;
	private float				level				= 0;

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

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
	}


}
