package data;

import java.util.ArrayList;
import java.util.List;

public class Group extends Input {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private List<Channel>		channelList			= new ArrayList<>();

	public Group(String name) {
		setName(name);
	}

	public void addChannel(Channel channel) {
		if (!channelList.contains(channel)) {
			channelList.add(channel);
		}
		if (channel.getGroup() != this && channel != null) {
			channel.setGroup(this);
		}
	}

	/*******************
	 * GETTER AND SETTER
	 ********************/
	public List<Channel> getChannelList() {
		return channelList;
	}
}
