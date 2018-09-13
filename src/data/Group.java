package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.InputListener;

public class Group extends Input implements InputListener {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private static final Logger	LOG					= Logger.getLogger(Group.class);
	private List<Channel>		channelList			= new ArrayList<>();
	private ArrayList<Double>	channelLevel		= new ArrayList<>();

	public Group(String name) {
		setName(name);
	}

	public void addChannel(Channel channel) {
		if (channel != null) {
			if (!channelList.contains(channel)) {
				channelList.add(channel);
				channel.addListener(this);
			}
			if (channel.getGroup() != this && channel != null) {
				channel.setGroup(this);
				if (this.getColor() != null && !this.getColor().isEmpty() && (channel.getColor() == null || channel.getColor().isEmpty())) {
					channel.setColor(getColor());
				}
			}
			Collections.sort(channelList);
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
			channel.removeListener(this);
			channelList.remove(channel);
		}
	}

	@Override
	public void levelChanged(double level, Input in) {
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

	public void delete() {
		LOG.info("Deleting group " + getName());
		for (Channel c : channelList) {
			removeChannel(c);
		}
		ASIOController.getInstance().removeGroup(this);
	}
}
