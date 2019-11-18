package gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import data.Channel;
import gui.FXMLUtil;
import gui.utilities.controller.VectorScope;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class VectorScopeTest extends Application {

	private static BorderPane root;
	private static PrintStream emptyStream;
	private static VectorScope vector;
	private static Channel c1;
	private static Channel c2;

	@Override
	public void start(Stage primaryStage) throws Exception {
		root = new BorderPane();
		FXMLUtil.setStyleSheet(root);
		primaryStage.setScene(new Scene(root));
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
		primaryStage.show();
		vector = new VectorScope();
		c1 = new Channel("Channel 1");
		c2 = new Channel("Channel 2");
		root.setCenter(vector);
	}

	@BeforeAll
	public static void startApplication() throws Exception {
		emptyStream = new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
			}
		});
		System.setOut(emptyStream);
		System.setErr(emptyStream);

		CountDownLatch latch = new CountDownLatch(1);
		try {
			new Thread(() -> launch()).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		latch.await(10, TimeUnit.SECONDS);
	}

	@AfterEach
	public void sleep() throws InterruptedException {
		Thread.sleep(500);
	}

	@AfterAll
	public static void closeEmptyStream() {
		if (emptyStream != null) {
			emptyStream.close();
		}
	}

	@AfterAll
	public static void shutdown() throws Exception {
		Platform.exit();
	}

	@Test
	public void inits() throws InterruptedException, ExecutionException {
		assertNotNull(root);
		assertNotNull(vector);
	}

	@Test
	public void setChannels() {

		try {
			vector.setChannels(c1, c2);
			assertEquals(c1, vector.getChannel1());
			assertEquals(c2, vector.getChannel2());
		} catch (Exception e) {
			throw (e);
		}
	}

	@Test
	public void removeChannels() {

		try {
			vector.setChannels(null, null);
			assertNull(vector.getChannel1());
			assertNull(vector.getChannel2());
		} catch (Exception e) {
			throw (e);
		}
	}

	@Test
	public void checkPause() {
		assertFalse(vector.isPaused());
		vector.pause(true);
		assertTrue(vector.isPaused());
		setChannels();
		assertTrue(vector.isPaused());
		vector.pause(false);
		assertFalse(vector.isPaused());
		vector.setChannels(c1, null);
		assertTrue(vector.isPaused());
	}

	@Test
	public void addData() throws Exception {
		setChannels();
		try {
			long start = System.currentTimeMillis();
			vector.setChannels(c1, c2);

			vector.pause(false);
			long data = 0;
			while (System.currentTimeMillis() - start < 2500) {
				int buffer_size = 128;
				float[] array1 = new float[buffer_size];
				float[] array2 = new float[buffer_size];
				for (int i = 0; i < buffer_size; i++) {
					array1[i] = (float) (Math.random() * 2.0) - 1;
					array2[i] = (float) (Math.random() * 2.0) - 1;
				}
				c1.setBuffer(array1, data);
				c2.setBuffer(array2, data);
				data++;
				Thread.sleep((long) ((48000.0 / buffer_size) / 1000.0));
			}
		} catch (Exception e) {
			throw (e);
		}
	}
}
