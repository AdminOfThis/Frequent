package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.InputListener;

public class Group extends Input implements InputListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LogManager.getLogger(Group.class);
	private List<Channel> channelList = new ArrayList<>();
	private List<Double> channelLevel = Collections.synchronizedList(new ArrayList<>());
	private Group ghostStereoGroup;
	private boolean isGhostChannel;

	public Group(String name) {
		this(name, false);
	}

	private Group(String name, boolean isGhost) {
		setName(name);
		isGhostChannel = isGhost;
		if (!isGhost) {
			ghostStereoGroup = new Group("#GhostStereoGroup", true);
			ghostStereoGroup.setColor(getColor());
		}
	}

	public void addChannel(Channel channel) {
		if (channel != null) {
			// If stereo channel gets added, check if group is stereo,otherwise make stereo
			if (channel.isStereo() && !isStereo() && !isGhostChannel) {
				// copy channels over
				ghostStereoGroup.setChannelList(getChannelList());

				// ghostStereoGroup.addChannel((Channel) channel.getStereoChannel());
				setStereoChannel(ghostStereoGroup);
			}
			if (channel.isStereo()) {
				if (this.isGhostChannel) {
					if (!channelList.contains(channel.getRightChannel())) {
						channelList.add(channel.getRightChannel());
						channel.getRightChannel().addListener(this);
					}
				} else {
					if (!channelList.contains(channel.getLeftChannel())) {
						channelList.add(channel.getLeftChannel());
						channel.getLeftChannel().addListener(this);
					}
					if (!ghostStereoGroup.getChannelList().contains(channel.getRightChannel())) {
						ghostStereoGroup.addChannel(channel.getRightChannel());
					}
				}
			} else {
				if (!channelList.contains(channel)) {
					channelList.add(channel);
					channel.addListener(this);
					if (isStereo() && !isGhostChannel) {
						ghostStereoGroup.addChannel(channel);
					}
				}
			}

			// coloring
			if (channel.getGroup() != this && channel != null) {
				channel.setGroup(this);
				if (this.getColor() != null && !this.getColor().isEmpty() && (channel.getColor() == null || channel.getColor().isEmpty())) {
					channel.setColor(getColor());
				}
			}

			Collections.sort(channelList);
		}
	}

	private void setChannelList(List<Channel> channelList2) {
		for (Channel c : channelList2) {
			addChannel(c);
		}
	}

	public void removeChannel(Channel channel) {
		if (channel != null) {
			channel.setGroup(null);
			channel.removeListener(this);
			channelList.remove(channel);
			// check for stereo, if possible make mono
			boolean stereo = false;
			for (Channel c : getChannelList()) {
				if (c.isStereo()) {
					stereo = true;
					break;
				}
			}
			// if possible make mono
			if (!stereo) {
				// remove all channels from ghost channel
				while (!ghostStereoGroup.getChannelList().isEmpty()) {
					ghostStereoGroup.removeChannel(ghostStereoGroup.getChannelList().get(0));
				}
				setStereoChannel(null);
			}
		}
	}

	/*******************
	 * GETTER AND SETTER
	 ********************/
	public List<Channel> getChannelList() {
		return channelList;
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		synchronized (channelList) {

			channelLevel.add(level);

			if (channelLevel.size() == channelList.size()) {
				double median = 0;
				for (double d : channelLevel) {
					median += d;
				}
				median = median / channelLevel.size();
				channelLevel.clear();

				this.setLevel((float) median, time);

			}
		}
	}

	public void delete() {
		LOG.info("Deleting group " + getName());
		for (Channel c : channelList) {
			removeChannel(c);
		}
		ASIOController.getInstance().removeGroup(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			Group other = (Group) obj;
			return (super.equals(obj) && Objects.equals(this.getChannelList(), other.getChannelList()));
		}
		return false;
	}
}
