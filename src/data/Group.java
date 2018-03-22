package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {

	private static final long	serialVersionUID	= 1L;
	private String				name;
	private String				hexColor;
	private List<Channel>		channelList			= new ArrayList<Channel>();

	public Group(String name) {
		this.name = name;
	}

	public void addChannel(Channel channel) {
		if (!channelList.contains(channel)) {
			channelList.add(channel);
		}
	}


	/*******************
	 * GETTER AND SETTER
	 ********************/

	public List<Channel> getChannelList() {
		return channelList;
	}

	public String getName() {
		return name;
	}

	public boolean setColor(String color) {
		// trying to parsse string to make sure its a hex string
		try {
			if (color.startsWith("#")) {
				color = color.substring(1);
			}
			Long.parseLong(color, 16);
			hexColor = "#" + color;
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getColor() {
		return hexColor;
	}

}
