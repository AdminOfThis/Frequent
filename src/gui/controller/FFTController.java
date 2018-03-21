package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeBackgroundAreaChart;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class FFTController implements Initializable {

	private static final Logger		LOG				= Logger.getLogger(FFTController.class);
	private static final int		X_MIN			= 25;
	private static final int		X_MAX			= 20000;
	private static final int		REFRESH_RATE	= 25;
	@FXML
	private HBox					chartRoot;
	@FXML
	private StackPane				vuPane;
	@FXML
	private Pane					vuPeakPane, vuLastPeakPane;
	@FXML
	private Label					lblPeak;
	@FXML
	private AnchorPane				clippingPane;
	private XYChart<Number, Number>	chart;
	private Timeline				line;
	private ASIOController			controller;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.info("Loading FFT Chart");
		initChart();
		initTimeline();
	}

	private void initTimeline() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (controller != null) {
				double peakdB = percentToDB(controller.getPeak() * 1000.0);
				vuPeakPane.setPrefHeight(vuPane.getHeight() * (peakdB + 60.0) / 60.0);
				double lastPeakdB = percentToDB(controller.getLastPeak() * 1000.0);
				vuLastPeakPane.setPrefHeight(vuPane.getHeight() * (lastPeakdB + 60.0) / 60.0);
				lblPeak.setText(Math.round(peakdB * 10.0) / 10.0 + "");
				if (controller.getPeak() >= 0.99) {
					clippingPane.setStyle("-fx-background-color: red");
				} else {
					clippingPane.setStyle("");
				}
				double[][] map = controller.getSpectrumMap();
				// long before = System.currentTimeMillis();
				// System.out.println("Updating");
				if (map != null) {
					Series<Number, Number> series = null;
					if (!chart.getData().isEmpty()) {
						series = chart.getData().get(0);
						series.getData().clear();
					}
					if (series == null) {
						series = new XYChart.Series<>();
						chart.getData().add(series);
					}
					// System.out.println("Displaying " + map[0].length + "
					// frequencies");
					// series.getData().add(new Data<Number, Number>(20, 0.0));
					ArrayList<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
					for (int count = 0; count < map[0].length; count++) {
						double frequency = map[0][count];
						if (frequency >= 20 && frequency <= X_MAX) {
							double level = Math.abs(map[1][count]);
							level = percentToDB(level);
							Data<Number, Number> data = new XYChart.Data<>(frequency, level);
							dataList.add(data);
						}
					}
					series.getData().addAll(dataList);
					// series.getData().add(new Data<Number, Number>(X_MAX,
					// 0.0));
				}
				// System.out.println("Finished updating");
				// System.out.println(System.currentTimeMillis() - before + "
				// ms");
			}
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Timeline.INDEFINITE);
	}

	private double percentToDB(double level) {
		return 20.0 * Math.log10(level / 1000.0);
	}

	private void initChart() {
		ValueAxis<Number> yaxis = new NumberAxis(-60, 0, 3);
		yaxis.setPrefWidth(20.0);
		// yaxis.setAutoRanging(true);
		// yaxis.setOpacity(0.0);
		yaxis.setAnimated(true);
		ValueAxis<Number> logAxis = new LogarithmicAxis(X_MIN, X_MAX);
		chart = new NegativeBackgroundAreaChart<>(logAxis, yaxis);
		chart.setAnimated(false);
		((NegativeBackgroundAreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setLegendVisible(false);
		chart.setLegendSide(Side.RIGHT);
		chart.setHorizontalZeroLineVisible(false);
		chartRoot.getChildren().add(chart);
		HBox.setHgrow(chart, Priority.ALWAYS);
		// chart.setPrefHeight(100.0);
		// chart.setPrefWidth(200.0);
		clippingPane.prefHeightProperty().bind(logAxis.heightProperty().add(12.0));
		clippingPane.minHeightProperty().bind(logAxis.heightProperty().add(12.0));
	}

	public void setDriver(ASIOController driver) {
		this.controller = driver;
	}

	public void play(boolean play) {
		if (play) {
			line.playFromStart();
		} else {
			line.pause();
		}
	}

	public boolean isPlaying() {
		return line.getStatus() == Animation.Status.RUNNING;
	}


}
