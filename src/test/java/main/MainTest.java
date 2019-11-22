package main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

class MainTest {

	private static final long WAIT_TIME = 100;

	private static Exception e;
	private static Scene scene;

	@Test
	@BeforeAll
	public static void launchApplication() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try {
				FXMLLauncher.main(new String[] { "-debug" });
			} catch (Exception ex) {
				e = ex;
				e.printStackTrace();
			} finally {
				latch.countDown();
			}
		});
		thread.start();// Initialize the thread
		latch.await(8, TimeUnit.SECONDS);
		scene = Main.getInstance().getScene();
		if (e != null) {
			throw e;
		}
	}

	@AfterAll
	public static void closeApplication() throws Exception {
		Thread.sleep(WAIT_TIME);
	}

	@ParameterizedTest
	@EnumSource(value = KeyCode.class, names = { "1", "2", "3", "4", "5", "6", "7", "8", "9" }, mode = EnumSource.Mode.INCLUDE)
	public void openModules(KeyCode code) throws Exception {
		pushButton(code, false);
		Thread.sleep(WAIT_TIME);
	}

	@ParameterizedTest
	@EnumSource(value = KeyCode.class, names = { "1", "2", "3", "4", "5", "6", "7", "8", "9" }, mode = EnumSource.Mode.INCLUDE)
	public void pressKeysControlDown(KeyCode code) throws Exception {
		pushButton(code, true);
		Thread.sleep(WAIT_TIME);
	}

	@Test
	public void fullScreenToggle() throws Exception {

		assertFalse(findFullScreenWindow());
		pushButton(KeyCode.F11, false);
		Thread.sleep(1000);
		assertTrue(findFullScreenWindow());
		pushButton(KeyCode.F11, false);
		Thread.sleep(1000);
		assertFalse(findFullScreenWindow());
	}

	private boolean findFullScreenWindow() {
		boolean foundFullScreen = false;

		for (Window w : Stage.getWindows()) {
			if (w instanceof Stage) {
				if (((Stage) w).isFullScreen()) {
					foundFullScreen = true;
					break;
				}
			}
		}
		return foundFullScreen;
	}

	private void pushButton(KeyCode code, boolean control) {
		Platform.runLater(() -> Event.fireEvent(scene.getFocusOwner(), new KeyEvent(null, scene, KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Platform.runLater(() -> Event.fireEvent(scene.getFocusOwner(), new KeyEvent(null, scene, KeyEvent.KEY_RELEASED, "", "", code, false, control, false, false)));
	}
}
