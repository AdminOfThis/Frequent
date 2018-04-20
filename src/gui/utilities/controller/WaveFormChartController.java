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
import javafx.util.Duration;

public class WaveFormChartController implements Initializable {

	private static final Logger LOG = Logger.getLogger(WaveFormChartController.class);
	
	private static final long			TIME_FRAME	= 5000;

	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series		= new Series<>();
	private Timeline					timeline	= new Timeline();
	private Channel						channel;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		series.setName("Test");
		chart.getData().add(series);
		KeyFrame frame = new KeyFrame(Duration.millis(9), e -> {
			if (channel != null) {
				double value = channel.getLevel();
				value = value - 500; // TODO
				Data<Number, Number> newData = new Data<>(System.currentTimeMillis(), value);

				series.getData().add(newData);
			}
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
		timeline.playFromStart();

	}

	public void setChannel(Channel c) {
		
		this.channel = c;
		if(c != null) {
			LOG.info("WaveForm Channel set to " + c.getName());
		}
	}

}
