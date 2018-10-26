package data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@Test
	public void addChannels() {
		assertTrue(g.getChannelList().isEmpty());
		Channel c1 = new Channel();
		g.addChannel(c1);
		assertEquals(1, g.getChannelList().size());
		assertTrue(g.getChannelList().contains(c1));
	}
}
