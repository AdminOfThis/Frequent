package gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import gui.utilities.controller.BleedMonitor;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.VuMeterMono;
import gui.utilities.controller.VuMeterStereo;
import gui.utilities.controller.WaveFormChart;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class UtilitiesTest extends Application {

	private static BorderPane root;
	private static PrintStream emptyStream;

	@Override
	public void start(Stage primaryStage) throws Exception {
		root = new BorderPane();
		primaryStage.setScene(new Scene(root));
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
		primaryStage.show();
	}

	@BeforeAll
	public static void startApplication() throws Exception {
		emptyStream = new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {}
		});
		System.setOut(emptyStream);
		System.setErr(emptyStream);

		CountDownLatch latch = new CountDownLatch(1);
		try {
			new Thread(() -> launch()).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		latch.await(2, TimeUnit.SECONDS);
	}

	@AfterAll
	public static void closeEmptyStream() {
		if (emptyStream != null) {
			emptyStream.close();
		}
	}

	@AfterEach
	public void sleep() throws InterruptedException {
		Thread.sleep(500);
	}

	@AfterAll
	public static void shutdown() throws Exception {
		Platform.exit();
	}

	@Test
	public void meterMonoHorizontal() throws InterruptedException, ExecutionException {
		Node node = new VuMeterMono(null, Orientation.HORIZONTAL);
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void meterMonoVertical() throws InterruptedException, ExecutionException {
		Node node = new VuMeterMono(null, Orientation.VERTICAL);
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void meterStereoHorizontal() throws InterruptedException, ExecutionException {
		Node node = new VuMeterStereo(null, null, Orientation.HORIZONTAL);
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void meterStereoVertical() throws InterruptedException, ExecutionException {
		Node node = new VuMeterStereo(null, null, Orientation.VERTICAL);
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void vectorScope() throws InterruptedException, ExecutionException {
		VectorScope node = new VectorScope();
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void waveFormChart() throws InterruptedException, ExecutionException {
		Node node = new WaveFormChart();
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void doughnutChart() throws InterruptedException, ExecutionException {
		Node node = new DoughnutChart(FXCollections.observableArrayList());
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	@Test
	public void bleedMonitor() throws InterruptedException, ExecutionException {
		Node node = new BleedMonitor();
		setAsRoot(node);
		assertEquals(node, root.getCenter());
	}

	private void setAsRoot(Node node) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			root.setCenter(node);
			latch.countDown();
		});
		latch.await(3000, TimeUnit.SECONDS);
	}
}
