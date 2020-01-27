package data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synthbot.jasiohost.AsioChannel;

public class ChannelTest {

	private Channel[] channel = new Channel[3];

	@Test
	public void buffer() {
		for (float f : channel[0].getBuffer()) {
			assertEquals(.0, f);
		}
	}

	@Test
	public void compare() {
		assertNotEquals(0, channel[0].compareTo(channel[1]));
	}

	@Test
	@BeforeEach
	public void createChannel() {
		for (int i = 0; i < channel.length; i++) {
			AsioChannel asio = Mockito.mock(AsioChannel.class);
			Mockito.when(asio.getChannelIndex()).thenReturn(i);
			channel[i] = new Channel(asio, "Channel " + i);
			assertNotNull(channel[i]);
		}

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
	public void equals() {
		assertFalse(channel[0].equals(channel[1]));
		channel[1].setName(channel[0].getName());
		assertFalse(channel[0].equals(channel[1]));
		channel[1].setChannel(channel[0].getChannel());
		assertTrue(channel[0].equals(channel[1]));
		Group group = Mockito.mock(Group.class);
		channel[0].setGroup(group);
		assertFalse(channel[0].equals(channel[1]));
		channel[1].setGroup(group);
		assertTrue(channel[0].equals(channel[1]));

	}

	@Test
	public void isHidden() {
		assertFalse(channel[0].isHidden());
		channel[0].setHidden(true);
		assertTrue(channel[0].isHidden());
		channel[0].setHidden(false);
		assertFalse(channel[0].isHidden());
	}

	@Test
	public void percentToDb() {
		assertEquals(Double.NEGATIVE_INFINITY, Channel.percentToDB(0.0));
		assertEquals(0, Channel.percentToDB(1.0));
		assertEquals(-6, Channel.percentToDB(.5), .1);
	}

	@Test
	public void resetName() {
		String oldName = channel[0].getName();
		channel[0].resetName();
		assertNotEquals(oldName, channel[0].getName());
		assertEquals(channel[0].getName(), channel[0].getChannel().getChannelName());
	}

	@Test
	public void setChannelNull() {
		channel[0].setChannel(null);
		assertNull(channel[0].getChannel());
		assertEquals(-1, channel[0].getChannelIndex());
	}

	@Test
	public void setStereoChannel() {
		assertNull(channel[0].getStereoChannel());
		assertNull(channel[1].getStereoChannel());
		assertFalse(channel[0].isStereo());
		assertFalse(channel[1].isStereo());
		assertFalse(channel[2].isStereo());
		assertFalse(channel[0].isLeftChannel());
		assertFalse(channel[1].isLeftChannel());
		assertFalse(channel[2].isLeftChannel());
		channel[0].setStereoChannel(channel[1]);
		channel[0].setStereoChannel(channel[1]);
		assertEquals(channel[0].getStereoChannel(), channel[1]);
		assertEquals(channel[1].getStereoChannel(), channel[0]);
		assertTrue(channel[0].isStereo());
		assertTrue(channel[1].isStereo());
		assertFalse(channel[2].isStereo());
		assertTrue(channel[0].isLeftChannel());
		assertFalse(channel[1].isLeftChannel());
		channel[0].setStereoChannel(channel[2]);
		assertNull(channel[1].getStereoChannel());
		assertEquals(channel[0].getStereoChannel(), channel[2]);
		assertEquals(channel[2].getStereoChannel(), channel[0]);
		assertTrue(channel[0].isStereo());
		assertFalse(channel[1].isStereo());
		assertTrue(channel[2].isStereo());
		assertTrue(channel[0].isLeftChannel());
		assertFalse(channel[2].isLeftChannel());

	}
}
