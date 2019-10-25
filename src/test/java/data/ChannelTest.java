package data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.SuperTest;

public class ChannelTest extends SuperTest{

	private Channel	c1;
	private Channel	c2;

	@Test
	@BeforeEach
	public void createChannel() {
		// Channel 1
		c1 = new Channel("Channel 1");
		assertNotNull(c1);
		assertEquals("Channel 1", c1.getName());
		// Channel 2
		c2 = new Channel("Channel 2");
		assertNotNull(c2);
		assertEquals("Channel 2", c2.getName());
	}

	@Test
	public void setStereoChannel() {
		assertNull(c1.getStereoChannel());
		assertNull(c2.getStereoChannel());
		c1.setStereoChannel(c2);
		assertEquals(c1.getStereoChannel(), c2);
		assertEquals(c2.getStereoChannel(), c1);
	}

	@Test
	public void equals() {
		c2.setName(c1.getName());
		assertTrue(c1.equals(c2));
	}
}
