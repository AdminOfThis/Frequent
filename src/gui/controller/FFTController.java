package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Channel;
import data.FFTListener;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeAreaChart;
import gui.utilities.controller.VuMeter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class FFTController implements Initializable, FFTListener, Pausable {

	private static final double		DECAY		= 1.01;
	public static final double		FFT_MIN		= -80;
	private static final Logger		LOG			= Logger.getLogger(FFTController.class);
	private static final int		X_MIN		= 25;
	private static final int		X_MAX		= 20000;
	@FXML
	private HBox					chartRoot;
	private XYChart<Number, Number>	chart;
	private VuMeter					meter;
	private boolean					pause		= true;
	private Series<Number, Number>	series		= new Series<>();
	private Series<Number, Number>	maxSeries	= new Series<>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.info("Loading FFT Chart");
		initVuMeter();
		initChart();
	}

	private void initVuMeter() {
		meter = new VuMeter(null, Orientation.VERTICAL);
		meter.setParentPausable(this);
		meter.setPrefWidth(50.0);
		chartRoot.getChildren().add(meter);
	}

	private void initChart() {
		ValueAxis<Number> yaxis = new NumberAxis(FFT_MIN, 0, 6);
		yaxis.setPrefWidth(20.0);
		// yaxis.setAutoRanging(true);
		// yaxis.setOpacity(0.0);
		yaxis.setAnimated(true);
		ValueAxis<Number> logAxis = new LogarithmicAxis(X_MIN, X_MAX);
		// chart = new NegativeBackgroundAreaChart<>(logAxis, yaxis);
		chart = new NegativeAreaChart(logAxis, yaxis);
		chart.getData().add(series);
		chart.getData().add(maxSeries);
		chart.setAnimated(false);
		((AreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setLegendVisible(false);
		chart.setLegendSide(Side.RIGHT);
		chart.setHorizontalZeroLineVisible(false);
		chartRoot.getChildren().add(chart);
		HBox.setHgrow(chart, Priority.ALWAYS);
	}

	public void setChannel(Channel channel) {
		meter.setChannel(channel);
	}

	@Override
	public void newFFT(double[][] map) {
		if (map != null && !pause) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					ArrayList<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
					for (int count = 0; count < map[0].length; count++) {
						double frequency = map[0][count];
						if (frequency >= 20 && frequency <= X_MAX) {
							double level = Math.abs(map[1][count]);
							level = Channel.percentToDB(level);
							Data<Number, Number> data = new XYChart.Data<>(frequency, level);
							dataList.add(data);
						}
					}
					// max
					if (series.getData().size() != maxSeries.getData().size()) {
						maxSeries.getData().setAll(FXCollections.observableArrayList(series.getData()));
					} else {
						for (int i = 0; i < maxSeries.getData().size(); i++) {
							Data<Number, Number> maxData = maxSeries.getData().get(i);
							Data<Number, Number> data = series.getData().get(i);
							if (data.getYValue().doubleValue() >= maxData.getYValue().doubleValue()) {
								maxData.setYValue(data.getYValue());
							} else {
								maxData.setYValue(maxData.getYValue().doubleValue() * DECAY);
							}
						}
					}
					// series.getData().clear();
					series.getData().setAll(dataList);
				}
			});
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
		LOG.info("Playing animations for spectrum view");
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		LOG.error("Uninplemented method called: addParentPausable");
	}
}
