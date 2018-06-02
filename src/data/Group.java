package data;

import java.util.ArrayList;
import java.util.List;

public class Group extends Input implements LevelObserver {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private List<Channel>		channelList			= new ArrayList<>();
	private ArrayList<Double>	channelLevel		= new ArrayList<>();

	public Group(String name) {
		setName(name);
	}

	public void addChannel(Channel channel) {
		if (channel != null) {
			if (!channelList.contains(channel)) {
				channelList.add(channel);
				channel.addObserver(this);
			}
			if (channel.getGroup() != this && channel != null) {
				channel.setGroup(this);
			}
			channelList.sort(Channel.COMPARATOR);
		}
	}

	/*******************
	 * GETTER AND SETTER
	 ********************/
	public List<Channel> getChannelList() {
		return channelList;
	}

	public void removeChannel(Channel channel) {
		if (channel != null) {
			channel.setGroup(null);
			channel.removeObserver(this);
			channelList.remove(channel);
		}
	}

	@Override
	public void levelChanged(double level) {
		channelLevel.add(level);
		if (channelLevel.size() == channelList.size()) {
			double median = 0;
			for (double d : channelLevel) {
				median += d;
			}
			median = median / channelLevel.size();
			channelLevel.clear();
			this.setLevel((float) median);
		}
	}
}
