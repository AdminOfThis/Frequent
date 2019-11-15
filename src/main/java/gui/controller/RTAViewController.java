package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.FFTListener;
import data.Channel;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeAreaChart;
import gui.utilities.controller.VuMeterMono;
import javafx.animation.AnimationTimer;
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
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import main.Constants;

public class RTAViewController implements Initializable, FFTListener, PausableView {

	private static final double DECAY = 1.01;
	private static final Logger LOG = LogManager.getLogger(RTAViewController.class);
	// private static final String TUNER_PATH = "/gui/gui/Tuner.fxml";
	private static final int X_MIN = 25;
	private static final int X_MAX = 20000;
	@FXML
	private BorderPane root;
	@FXML
	private HBox chartPane;
	@FXML
	private ToggleButton toggleSlowCurve, toggleVPad, tglPause;
	@FXML
	private Slider sliderPad;
	@FXML
	private XYChart<Number, Number> chart;
	@FXML
	private HBox topRight, topLeft;
	private VuMeterMono meter;
	private boolean pause = true;
	private Series<Number, Number> series = new Series<>();
	private Series<Number, Number> maxSeries = new Series<>();
	private Channel channel;
	private double[][] buffer;

	@Override
	public ArrayList<Region> getHeader() {
		ArrayList<Region> list = new ArrayList<>();
//		list.add(sliderPad);
		list.add(toggleVPad);
		list.add(toggleSlowCurve);
		list.add(tglPause);
		return list;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		LOG.debug("Loading FFT Chart");
		topLeft.prefWidthProperty().bind(topRight.widthProperty());
		initVuMeter();
		initChart();
		initButtons();
		// initTuner();
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				if (buffer != null && !isPaused()) {
					updateChart(buffer);
				}
			}
		};
		timer.start();
	}

	// private void initTuner() {
	// Parent p = FXMLUtil.loadFXML(TUNER_PATH);
	// TunerController tunerController = (TunerController)
	// FXMLUtil.getController();
	// topPane.getChildren().add(1, p);
	// HBox.setHgrow(p, Priority.ALWAYS);
	// tunerController.show(true);
	// }
	private void initButtons() {
		toggleSlowCurve.selectedProperty().addListener(e -> {
			if (toggleSlowCurve.isSelected()) {
				if (!chart.getData().contains(maxSeries)) {
					chart.getData().add(maxSeries);
				}
			} else {
				chart.getData().remove(maxSeries);
			}
		});
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		toggleVPad.selectedProperty().addListener(e -> {
			if (toggleVPad.isSelected()) {
				yAxis.setUpperBound(Math.round(sliderPad.getValue()));
			} else {
				yAxis.setUpperBound(0.0);
			}
		});
		sliderPad.valueProperty().addListener(e -> {
			toggleVPad.setText(Math.round(sliderPad.getValue()) + " dB");
			if (toggleVPad.isSelected()) {
				yAxis.setUpperBound(Math.round(sliderPad.getValue()));
			}
		});

		tglPause.selectedProperty().addListener((e, oldV, newV) -> pause(newV));
	}

	private void initChart() {
		ValueAxis<Number> yaxis = new NumberAxis(Constants.FFT_MIN, 0, 6);
		yaxis.setPrefWidth(20.0);
		// yaxis.setAutoRanging(true);
		// yaxis.setOpacity(0.0);
		yaxis.setAnimated(true);
		ValueAxis<Number> logAxis = new LogarithmicAxis(X_MIN, X_MAX);
		// chart = new NegativeBackgroundAreaChart<>(logAxis, yaxis);
		chart = new NegativeAreaChart(logAxis, yaxis);
		chart.getData().add(series);
		// chart.getData().add(maxSeries);
		chart.setAnimated(false);
		((AreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setTitleSide(Side.TOP);
		chart.setLegendVisible(false);
		chart.setHorizontalZeroLineVisible(false);
		chartPane.getChildren().add(1, chart);
		HBox.setHgrow(chart, Priority.ALWAYS);
	}

	private void initVuMeter() {
		meter = new VuMeterMono(null, Orientation.VERTICAL);
		meter.setParentPausable(this);
		meter.setPrefWidth(50.0);
		chartPane.getChildren().add(0, meter);
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void newFFT(final double[][] map) {
		buffer = map;
	}

	@Override
	public void pause(final boolean pause) {
		if (this.pause != pause) {
			this.pause = pause;
			if (pause) {
				LOG.debug(getClass().getSimpleName() + "; pausing animations");
			} else {
				LOG.debug(getClass().getSimpleName() + "; playing animations");
			}
		}
	}

	@Override
	public void refresh() {
		if (channel == null) {
			chart.setTitle("");
		} else {
			chart.setTitle(channel.getName());
		}
	}

	@Override
	public void setSelectedChannel(final Input input) {
		if (input instanceof Channel) {
			Channel channel = (Channel) input;
			this.channel = channel;
			meter.setChannel(channel);
			chart.setTitle(channel.getName());
		}
	}

	private void updateChart(final double[][] map) {
		ArrayList<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
		for (int count = 0; count < map[0].length; count++) {
			double frequency = map[0][count];
			if (frequency >= 20 && frequency <= X_MAX) {
				double level = Math.abs(map[1][count]);
				level = Channel.percentToDB(level / 1000.0);
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
		series.getData().setAll(dataList);
	}
}
