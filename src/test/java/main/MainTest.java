package main;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	public void launchApplication() {

		try {
			Thread thread = new Thread(() -> {

				try {

					Main.main(new String[] { "-debug" });
				} catch (Exception e) {
					e.printStackTrace();
					fail("Unable to start Application", e);
				}

			});

			thread.start();// Initialize the thread
			Thread.sleep(10000);

		} catch (Exception e) {
			fail("Unable to start Application", e);
		}
		return;
	}

}
