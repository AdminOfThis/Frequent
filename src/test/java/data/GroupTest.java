package data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GroupTest {

	private Group	g1;
//	private Group	g2;

	private Channel	c1;
	private Channel	c2;

	@Test
	@BeforeEach
	public void createGroups() {
		// Group 1
		g1 = new Group("Group 1");
		assertNotNull(g1);
		assertNotNull(g1.getChannelList());
		assertEquals(0, g1.getChannelList().size());
		assertEquals("Group 1", g1.getName());
		// Group 2
		Group g2 = new Group("Group 2");
		assertNotNull(g2);
		assertNotNull(g2.getChannelList());
		assertEquals(0, g2.getChannelList().size());
		assertEquals("Group 2", g2.getName());

	}

	@Test
	@BeforeEach
	public void createChannels() {
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
	public void addChannels() {
		assertTrue(g1.getChannelList().isEmpty());
		g1.addChannel(c1);
		g1.addChannel(c2);
		assertEquals(2, g1.getChannelList().size());
		assertTrue(g1.getChannelList().contains(c1));
		assertTrue(g1.getChannelList().contains(c2));
		assertEquals(c1.getGroup(), c2.getGroup());
	}

	@Test
	public void removeChannel() {
		addChannels();
		g1.removeChannel(c1);
		assertEquals(1, g1.getChannelList().size());
		assertTrue(g1.getChannelList().contains(c2));
		assertFalse(g1.getChannelList().contains(c1));
	}
}
