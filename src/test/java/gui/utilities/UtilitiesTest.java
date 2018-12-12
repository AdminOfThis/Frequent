package gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Label;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gui.utilities.controller.VuMeterMono;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class UtilitiesTest extends Application {

	private static BorderPane root;

	@BeforeAll
	public static void startApplication() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);

		try {
			new Thread(() -> launch()).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
		latch.await(5, TimeUnit.SECONDS);
	}

	@AfterAll
	public static void shutdown() throws Exception {
		Thread.sleep(2000);
		Platform.exit();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		root = new BorderPane();
		primaryStage.setScene(new Scene(root));
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
		primaryStage.show();
	}


	@Test
	public void test1() throws InterruptedException, ExecutionException {
		CountDownLatch latch = new CountDownLatch(1);
		VuMeterMono meterMono = new VuMeterMono(null, Orientation.HORIZONTAL);
		Platform.runLater(() -> {
			root.setCenter(meterMono);
			latch.countDown();
		});
		latch.await(1000, TimeUnit.SECONDS);
		assertEquals(meterMono, root.getCenter());

	}


}
