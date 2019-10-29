package control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import data.Cue;
import test.SuperTest;

class TimeKeeperTest extends SuperTest {

	@Test
	public void instance() {
		assertNotNull(TimeKeeper.getInstance());
	}

	@Test()
	public void start() throws InterruptedException {
		TimeKeeper.getInstance().round();
		// assertEquals(0, TimeKeeper.getInstance().getActiveIndex());
		Thread.sleep(100);
		assertTrue(TimeKeeper.getInstance().getRoundTime() > 0);
		// assertEquals(new Cue(TimeKeeper.DEFAULT_CUE_NAME + "1"),
		// TimeKeeper.getInstance().getActiveCue());
		assertEquals(null, TimeKeeper.getInstance().getNextCue());
	}

	@Test()
	public void round() {
		TimeKeeper.getInstance().round();
		// assertEquals(1, TimeKeeper.getInstance().getActiveIndex());#
		assertNotEquals(TimeKeeper.getInstance().getStartTime(), TimeKeeper.getInstance().getRoundTime() > 0);
		// assertEquals(new Cue(TimeKeeper.DEFAULT_CUE_NAME + "2"),
		// TimeKeeper.getInstance().getActiveCue());
		assertEquals(null, TimeKeeper.getInstance().getNextCue());
	}

	@Test
	public void addCue() {
		Cue cue = new Cue("TestCue");
		TimeKeeper.getInstance().add(cue);
		assertEquals(cue, TimeKeeper.getInstance().getNextCue());
		TimeKeeper.getInstance().round();
		assertEquals(cue, TimeKeeper.getInstance().getActiveCue());
	}
}
