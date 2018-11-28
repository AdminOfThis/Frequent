package main;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class MainTest {

	private Exception e;

	@Test
	public void launchApplication() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {

			try {
				Main.main(new String[] { "-debug" });
			} catch (Exception ex) {
				e = ex;
				e.printStackTrace();
			} finally {
				latch.countDown();
			}
		});

		thread.start();// Initialize the thread
		latch.await(15, TimeUnit.SECONDS);
		if (e != null) {
			throw e;
		}

	}

}
