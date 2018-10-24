package data;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;

public class ChannelTest {

	Channel	c1;
	Channel	c2;

	@Test
	public void createChannel() {
		c1 = new Channel("Channel 1");
		c2 = new Channel("Channel 2");
		assertNotNull(c1);
		assertNotNull(c2);
	}
// @Test
// public void setStereoChannel() {
// assertNull(c1.getStereoChannel());
// assertNull(c2.getStereoChannel());
// c1.setStereoChannel(c2);
// assertEquals(c1.getStereoChannel(), c2);
// assertEquals(c2.getStereoChannel(), c1);
// }
}
