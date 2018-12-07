package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class MainTest {

	private static Exception	e;
	private static Scene		scene;

	public static Object[][] data() {
		return new Object[][] { { KeyCode.DIGIT1, false }, { KeyCode.DIGIT2, false }, { KeyCode.DIGIT3, false }, { KeyCode.DIGIT4, false },
			{ KeyCode.DIGIT5, false }, { KeyCode.DIGIT1, true }, { KeyCode.DIGIT2, true } };
	}


	// @Test
	@BeforeAll
	public static void launchApplication() throws Exception  {
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
		clearSyso();
		thread.start();// Initialize the thread
		latch.await(10, TimeUnit.SECONDS);
		scene = Main.getInstance().getScene();
		if (e != null) {
			throw e;
		}
	}

	static void clearSyso() {
		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
			}
		}));
		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {

			}
		}));
	}

	@AfterAll
	public static void closeApplication() throws Exception {
		Thread.sleep(200);
	}


	@ParameterizedTest
	@MethodSource("data")
	public void openModules(KeyCode code, boolean control) throws Exception {

		pushButton(code, control);
		Thread.sleep(500);
	}

	@RepeatedTest(20)
	public void randomModules() throws Exception {
		Object[] o = data()[(int) Math.floor(Math.random() * (data().length - 1))];
		pushButton((KeyCode) o[0], (boolean) o[1]);
		Thread.sleep((long) Math.floor(50.0 * Math.random() * 10.0) + 100);
	}

	private void pushButton(KeyCode code, boolean control) {
		Platform.runLater(() -> Event.fireEvent(scene.getFocusOwner(),
			new KeyEvent(null, scene, KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false)));
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Platform.runLater(() -> Event.fireEvent(scene.getFocusOwner(),
			new KeyEvent(null, scene, KeyEvent.KEY_RELEASED, "", "", code, false, control, false, false)));
	}


}
