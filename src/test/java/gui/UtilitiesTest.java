package gui;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.javafx.application.LauncherImpl;

import gui.utilities.DoughnutChart;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeAreaChart;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.VuMeterMono;
import gui.utilities.controller.VuMeterStereo;
import gui.utilities.controller.WaveFormChart;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UtilitiesTest extends Application {

	private static VBox			box;
	private static final String	STYLESHEET_LOCATION	= "/css/style.css";
	private Exception			e;
	private static Exception	e2;

	public UtilitiesTest() {
		super();
	}

	@BeforeAll
	public static void startStage() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try {
				LauncherImpl.launchApplication(UtilitiesTest.class, new String[] { });
			}
			catch (Exception ex) {
				ex.printStackTrace();
				e2 = ex;
			}
			finally {
				latch.countDown();
			}
		});
		thread.start();
		latch.await(8, TimeUnit.SECONDS);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		box = new VBox();
		box.setAlignment(Pos.TOP_CENTER);
		Scene scene = new Scene(new ScrollPane(box));
		scene.getStylesheets().add(STYLESHEET_LOCATION);
		box.getStyleClass().add("background");
		primaryStage.setScene(scene);
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
		primaryStage.show();
	}

	private void addNode(Node node) {
		Platform.runLater(() -> {
			try {
				box.getChildren().add(node);
			}
			catch (Exception ex) {
				e = ex;
			}
		});
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException ex2) {
			// do nothing
		}
		if (e != null) {
			fail(e);
		}
		if (e2 != null) {
			fail(e2);
		}
	}

	@AfterAll
	public static void closeApplication() throws Exception {
		Thread.sleep(2000);
	}

	@Test
	void vuMeterMonoHorizontal() {
		addNode(new VuMeterMono(null, Orientation.HORIZONTAL));
	}

	@Test
	void vuMeterMonoVertical() {
		addNode(new VuMeterMono(null, Orientation.VERTICAL));
	}

	@Test
	void vuMeterStereoHorizontal() {
		addNode(new VuMeterStereo(null, null, Orientation.HORIZONTAL));
	}

	@Test
	void vuMeterStereoVertical() {
		addNode(new VuMeterStereo(null, null, Orientation.VERTICAL));
	}

	@Test
	void vectorScope() {
		addNode(new VectorScope());
	}

	@Test
	void waveFormChart() {
		addNode(new WaveFormChart());
	}

	@Test
	void doughnutChart() {
		addNode(new DoughnutChart(FXCollections.observableArrayList()));
	}

	@Test
	void negativeAreaChart() {
		addNode(new NegativeAreaChart(new LogarithmicAxis(1, 20000), new NumberAxis()));
	}
}
