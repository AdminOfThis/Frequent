package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

public class GroupTest {

	private Group g;

	@Test
	@BeforeEach
	public void createGroup() {
		String name = "Test";
		g = new Group(name);
		assertNotNull(g);
		assertNotNull(g.getChannelList());
		assertEquals(name, g.getName());
	}

	public void addChannels() {
		assertTrue(g.getChannelList().isEmpty());
		Channel c1 = new Channel();
		g.addChannel(c1);
		assertEquals(1, g.getChannelList().size());
		assertTrue(g.getChannelList().contains(c1));
	}
}
