package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ChannelTest {

	Channel	c1;
	Channel	c2;

	private void setup() {
		c1 = new Channel("Channel 1");
		c2 = new Channel("Channel 2");
	}

	@Test
	public void createChannel() {
		setup();
		assertNotNull(c1);
		assertNotNull(c2);
	}

	@Test
	public void setStereoChannel() {
		setup();
		assertNull(c1.getStereoChannel());
		assertNull(c2.getStereoChannel());
		c1.setStereoChannel(c2);
		assertEquals(c1.getStereoChannel(), c2);
		assertEquals(c2.getStereoChannel(), c1);
	}
}
