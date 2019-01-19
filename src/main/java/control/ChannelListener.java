package control;

import data.Channel;

public interface ChannelListener extends InputListener {

	public void newBuffer(Channel channel, float[] buffer, long time);
}
