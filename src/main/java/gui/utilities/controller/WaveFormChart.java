package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.adminofthis.util.gui.FXMLUtil;

import control.ASIOController;
import control.InputListener;
import data.Channel;
import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.NegativeAreaChart;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import main.Constants;

public class WaveFormChart extends AnchorPane implements Initializable, InputListener, PausableComponent {

	private static final Logger LOG = LogManager.getLogger(WaveFormChart.class);
	private static final String FXML = "/fxml/utilities/WaveFormChart.fxml";
	private static final long TIME_FRAME = 300000L;
	@FXML
	private BorderPane root;
	@FXML
	private VBox vBox;
	private XYChart<Number, Number> chart;
	private Series<Number, Number> series = new Series<>();
	private Series<Number, Number> treshold = new Series<>();
	private Input channel;
	private boolean pause = true;
	private Pausable pausableParent;
	private boolean styleSet = false;
	private Map<Long, Double> pendingMap = Collections.synchronizedMap(new HashMap<Long, Double>());

	public WaveFormChart() {
		LOG.debug("Creating new WaveFormChart");
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);

		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				new Thread(() -> update()).start();
			}
		};
		timer.start();
	}

	@Override
	public void colorChanged(String newColor) {}

	public Axis<Number> getYAxis() {
		return chart.getYAxis();
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		NumberAxis xAxis = new NumberAxis();
		xAxis.setPrefHeight(0.0);
		xAxis.setForceZeroInRange(false);
		xAxis.setTickUnit(TIME_FRAME / 10.0);
		NumberAxis yAxis = new NumberAxis();
		yAxis.setPrefWidth(0.0);
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(Constants.FFT_MIN);
		yAxis.setUpperBound(.0);
		for (NumberAxis axis : new NumberAxis[] { xAxis, yAxis }) {
			axis.setTickUnit(TIME_FRAME / 10.0);
			axis.setOpacity(.0);
		}
		xAxis.setAutoRanging(false);
		initArea(xAxis, yAxis);
		if (chart instanceof LineChart) {
			((LineChart<Number, Number>) chart).setCreateSymbols(false);
		} else if (chart instanceof AreaChart) {
			((AreaChart<Number, Number>) chart).setCreateSymbols(false);
		}
		chart.setAnimated(false);
		chart.setLegendVisible(false);
		chart.setVerticalZeroLineVisible(false);
		chart.setHorizontalZeroLineVisible(false);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);
		chart.getData().add(series);

		chart.setMinHeight(0.0);

		xAxis.setMinHeight(.0);
		xAxis.setMaxHeight(.0);
		xAxis.setPrefHeight(.0);

		root.setCenter(chart);
	}

	@Override
	public boolean isPaused() {
		return pause || pausableParent != null && pausableParent.isPaused() || channel == null || pendingMap == null || pendingMap.isEmpty();
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		double value = Channel.percentToDB(level);
		if (value < Constants.FFT_MIN) {
			value = Constants.FFT_MIN;
		}
		pendingMap.put(time, value);
	}

	@Override
	public void nameChanged(String name) {}

	@Override
	public void pause(final boolean pause) {
		this.pause = pause;
		if (pause) {
			LOG.debug(getClass().getSimpleName() + "; playing animations");
		} else {
			LOG.debug(getClass().getSimpleName() + "; pausing animations");
		}
	}

	public void setChannel(final Input c) {
		try {
			if (!Objects.equals(c, channel)) {
				if (channel != null) {
					channel.removeListener(this);
				}
				series.getData().clear();
				pendingMap.clear();
				channel = c;
				if (c != null) {
					c.addListener(this);
				}
			}
		} catch (Exception e) {
			LOG.warn("", e);
		}
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		pausableParent = parent;
	}

	public void setThreshold(final double value1) {
		double value = Math.abs(Constants.FFT_MIN) - value1;
		NumberAxis time = (NumberAxis) chart.getXAxis();
		synchronized (treshold) {
			treshold.getData().clear();
			treshold.getData().add(new Data<>(time.getLowerBound(), value));
			treshold.getData().add(new Data<>(time.getUpperBound() + 10000, value));
		}
	}

	public void showTreshold(boolean value) {
		if (value) {
			if (!chart.getData().contains(treshold)) {
				chart.getData().add(treshold);
			}
		} else {
			chart.getData().remove(treshold);
		}
	}

	private void addNewData() {
		ArrayList<Data<Number, Number>> dataList = new ArrayList<>();
		synchronized (pendingMap) {
			for (Entry<Long, Double> entry : pendingMap.entrySet()) {
				try {
					if (!styleSet) {
						synchronized (series) {
							series.getNode().setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 1px;");
						}
						styleSet = true;
					}
					double value = 0;
					if (channel != null) {
						value = entry.getValue();
					}
					Data<Number, Number> newData = new Data<>(entry.getKey(), value);
					dataList.add(newData);
				} catch (Exception e) {
					LOG.warn("", e);
				}
			}
		}
		pendingMap.clear();
//		}
		synchronized (series) {
			series.getData().addAll(dataList);
		}
	}

	private void initArea(final NumberAxis xAxis, final NumberAxis yAxis) {
		chart = new NegativeAreaChart(xAxis, yAxis);
		xAxis.setTickUnit(TIME_FRAME / 10.0);
		chart.setTitleSide(Side.TOP);
	}

	private void update() {

		if (!isPaused()) {
			NumberAxis xAxis = (NumberAxis) chart.getXAxis();

			addNewData();
			FXMLUtil.updateAxis(xAxis, TIME_FRAME, ASIOController.getInstance().getTime());
			synchronized (series) {
				FXMLUtil.removeOldData((long) xAxis.getLowerBound(), series);
			}

			synchronized (treshold) {
				if (treshold.getData().size() >= 2) {
					treshold.getData().get(1).setXValue(xAxis.getUpperBound() + 10000);
				}
			}
		}
	}
}
