package main;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	public void launchApplication() {

		try {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {

					try {

						Main.main(new String[] { "-debug" });
					} catch (Exception e) {
						e.printStackTrace();
						fail(e.getMessage());
					}

				}
			});

			thread.start();// Initialize the thread
			Thread.sleep(10000);
			Main.close();
			Thread.sleep(5000);

		} catch (Exception e) {
			fail(e.getMessage());
		}
		return;
	}


}
