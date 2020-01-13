package data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synthbot.jasiohost.AsioChannel;

public class ChannelTest {

	private Channel c1;
	private Channel c2;

	@Test
	@BeforeEach
	public void createChannel() {
		// Channel 1
		AsioChannel asio1 = Mockito.mock(AsioChannel.class);
		Mockito.when(asio1.getChannelIndex()).thenReturn(1);
		c1 = new Channel(asio1, "Channel 1");
		assertNotNull(c1);
		assertEquals("Channel 1", c1.getName());
		// Channel 2
		AsioChannel asio2 = Mockito.mock(AsioChannel.class);
		Mockito.when(asio2.getChannelIndex()).thenReturn(2);
		c2 = new Channel(asio2, "Channel 2");
		assertNotNull(c2);
		assertEquals("Channel 2", c2.getName());
	}

	@Test
	public void createNullChannel() {
		Channel nullChannel = new Channel();
		assertNull(nullChannel.getName());
		assertNull(nullChannel.getChannel());
	}

	@Test
	public void createUnnamedChannel() {
		AsioChannel channel = Mockito.mock(AsioChannel.class);
		Channel unnamedChannel = new Channel(channel);
		assertNull(unnamedChannel.getName());
		assertEquals(channel, unnamedChannel.getChannel());
	}

	@Test
	public void createFullChannel() {
		AsioChannel channel = Mockito.mock(AsioChannel.class);
		String name = "Full Name";
		Channel nullChannel = new Channel(channel, name);
		assertEquals(name, nullChannel.getName());
		assertEquals(channel, nullChannel.getChannel());
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
		c2.setChannel(c1.getChannel());
		assertTrue(c1.equals(c2));
	}

	@Test
	public void compare() {
		assertNotEquals(0, c1.compareTo(c2));
	}
}
