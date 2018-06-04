package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Input;
import data.LevelObserver;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class WaveFormChartController implements Initializable, LevelObserver {

	public static final String			PATH		= "/gui/utilities/gui/WaveFormChart.fxml";
	private static final Logger			LOG			= Logger.getLogger(WaveFormChartController.class);
	private static final long			TIME_FRAME	= 5000;
	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series		= new Series<>();
	private Input						channel;
	private double						value		= 1;
	private boolean						negative	= false;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		yAxis.setAutoRanging(true);
		chart.getData().add(series);
	}

	public void setChannel(Input c) {
		try {
			if (!c.equals(channel)) {
				if (channel != null) {
					channel.removeObserver(this);
				}
				series.getData().clear();
				this.channel = c;
				if (c != null) {
					c.addObserver(this);
				}
			}
		}
		catch (Exception e) {
			LOG.warn("", e);
		}
	}

	@Override
	public void levelChanged(double level) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				try {
					NumberAxis xAxis = (NumberAxis) chart.getXAxis();
					value = 0;
					if (channel != null) {
						value = channel.getLevel();
					}
					if (negative) {
						value = -value;
					}
					negative = !negative;
					Data<Number, Number> newData = new Data<>(System.currentTimeMillis(), value);
					series.getData().add(newData);
					long time = System.currentTimeMillis();
					// axis
					xAxis.setLowerBound(time - TIME_FRAME);
					xAxis.setUpperBound(time);
					boolean continueFlag = true;
					int count = 0;
					ArrayList<Data<Number, Number>> removeList = null;
					try {
						while (continueFlag) {
							Data<Number, Number> data = series.getData().get(count);
							if ((long) data.getXValue() < xAxis.getLowerBound() - 10) {
								if (removeList == null) {
									removeList = new ArrayList<>();
								}
								removeList.add(data);
							} else {
								continueFlag = false;
							}
							count++;
						}
					}
					catch (Exception e) {
						LOG.error("", e);
					}
					if (removeList != null) {
						series.getData().removeAll(removeList);
					}
				}
				catch (Exception e) {
					LOG.warn("", e);
				}
			}
		});
	}
}
