package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Channel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import main.Main;

public class WaveFormChartController implements Initializable {

	public static final String			PATH				= "/gui/utilities/gui/WaveFormChart.fxml";

	private static final Logger			LOG					= Logger.getLogger(WaveFormChartController.class);


	private static final double			STARTUP_TIME		= 2000;
	private static final long			TIME_FRAME			= 5000;

	private static final double			DEBUG_MULTIPLIER	= 200.0;

	@FXML
	private VBox						loadChart;
	@FXML
	private ProgressIndicator			progress;
	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series				= new Series<>();
	private Timeline					timeline			= new Timeline();
	private Channel						channel;
	private double						value				= 1;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		series.setName("Test");
		chart.getData().add(series);
		KeyFrame frame = new KeyFrame(Duration.millis(9), e -> {
			if (Main.isDebug()) {
				value = -(value + (Math.random() * DEBUG_MULTIPLIER - (DEBUG_MULTIPLIER / 2.0)));
				if (value < -900) {
					value = -900;
				} else if (value > 900) {
					value = 900;
				}
			} else {
				value = 0;
				if (channel != null) {
					value = channel.getLevel();
					value = value - 500; // TODO

				}
			}


			Data<Number, Number> newData = new Data<>(System.currentTimeMillis(), value);
			series.getData().add(newData);

			long time = System.currentTimeMillis();
			// axis
			xAxis.setLowerBound(time - TIME_FRAME);
			xAxis.setUpperBound(time);

			ArrayList<Data<Number, Number>> removeList = null;
			for (Data<Number, Number> data : series.getData()) {
				if ((long) data.getXValue() < xAxis.getLowerBound()) {
					if (removeList == null) {
						removeList = new ArrayList<>();
					}
					removeList.add(data);
				}
			}
			if (removeList != null) {
				series.getData().removeAll(removeList);
			}
		});

		timeline.getKeyFrames().add(frame);
		timeline.setCycleCount(Timeline.INDEFINITE);

		Timeline startLine = new Timeline();
		long start = System.currentTimeMillis();
		startLine.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
			double runTime = System.currentTimeMillis() - start;
			progress.setProgress(runTime / STARTUP_TIME);
			if (runTime > STARTUP_TIME) {
				timeline.playFromStart();
				((StackPane) loadChart.getParent()).getChildren().remove(loadChart);
				startLine.stop();
			}
		}));
		startLine.setCycleCount(Timeline.INDEFINITE);
		startLine.playFromStart();

	}

	public void setChannel(Channel c) {

		this.channel = c;
		if (c != null) {
			LOG.info("WaveForm Channel set to " + c.getName());
		}
	}

}
