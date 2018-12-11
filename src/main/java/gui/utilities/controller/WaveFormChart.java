package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.InputListener;
import data.Channel;
import data.Input;
import gui.controller.RTAViewController;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.FXMLUtil;
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

public class WaveFormChart extends AnchorPane implements Initializable, InputListener, PausableComponent {

	public enum Style {
		NORMAL, ABSOLUTE
	};

	private static final Logger		LOG			= Logger.getLogger(WaveFormChart.class);
	private static final String		FXML		= "/fxml/utilities/WaveFormChart.fxml";
	private static final long		TIME_FRAME	= 5000000000l;
	@FXML
	private BorderPane				root;
	private XYChart<Number, Number>	chart;
	private Series<Number, Number>	series		= new Series<>();
	private Series<Number, Number>	treshold	= new Series<>();
	private Input					channel;
	private double					value		= 1;
	private boolean					negative	= false;
	private boolean					pause		= false;
	private Pausable				pausableParent;
	private boolean					styleSet	= false;
	private Map<Long, Double>		pendingMap;
	private Style					style;

	public WaveFormChart(final Style style) {
		this.style = style;
		Parent p = FXMLUtil.loadFXML(FXML, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		pendingMap = Collections.synchronizedMap(new HashMap<Long, Double>());
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				update();
			}
		};
		timer.start();
	}

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
		yAxis.setAutoRanging(true);
		for (NumberAxis axis : new NumberAxis[] { xAxis, yAxis }) {
			axis.setTickUnit(TIME_FRAME / 10.0);
			axis.setOpacity(.0);
		}
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(true);
		switch (style) {
		case NORMAL:
			initWaveForm(xAxis, yAxis);
			break;
		case ABSOLUTE:
			initArea(xAxis, yAxis);
			break;
		}
		chart.setAnimated(false);
		chart.setLegendVisible(false);
		chart.setVerticalZeroLineVisible(false);
		chart.setHorizontalZeroLineVisible(true);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);
		chart.getData().add(series);
		root.setCenter(chart);
	}

	private void initArea(final NumberAxis xAxis, final NumberAxis yAxis) {
		chart = new NegativeAreaChart(xAxis, yAxis);
		xAxis.setTickUnit(TIME_FRAME / 10.0);
		((AreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setTitleSide(Side.TOP);
		chart.setLegendVisible(false);
		chart.setHorizontalZeroLineVisible(false);
	}

	private void initWaveForm(final NumberAxis xAxis, final NumberAxis yAxis) {
		chart = new LineChart<>(xAxis, yAxis);
		((LineChart<Number, Number>) chart).setCreateSymbols(false);
	}

	@Override
	public boolean isPaused() {
		return pause || pausableParent != null && pausableParent.isPaused() || channel == null || pendingMap == null;
	}

	@Override
	public void levelChanged(final double level, long time) {
		double value = Channel.percentToDB(level);
		if (value < RTAViewController.FFT_MIN) {
			value = RTAViewController.FFT_MIN;
		}
		pendingMap.put(time, value);
	}

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
			if (!c.equals(channel)) {
				if (channel != null) {
					channel.removeListener(this);
				}
				series.getData().clear();
				synchronized (pendingMap) {
					pendingMap.clear();
				}
				channel = c;
				if (c != null) {
					c.addListener(this);
				}
			}
		}
		catch (Exception e) {
			LOG.warn("", e);
		}
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		pausableParent = parent;
	}

	private void update() {
		if (!isPaused()) {
			NumberAxis xAxis = (NumberAxis) chart.getXAxis();
			synchronized (pendingMap) {
				ArrayList<Data<Number, Number>> dataList = new ArrayList<>();
				for (Entry<Long, Double> entry : pendingMap.entrySet()) {
					try {
						if (!styleSet) {
							series.getNode().setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 1px;");
							styleSet = true;
						}
						value = 0;
						if (channel != null) {
							value = entry.getValue();
						}
						if (style == Style.ABSOLUTE) {
							value = Math.abs(RTAViewController.FFT_MIN) - Math.abs(value);
						} else {
							value = Math.abs(RTAViewController.FFT_MIN) - Math.abs(value);
							if (negative) {
								value = -value;
							}
						}
						negative = !negative;
						Data<Number, Number> newData = new Data<>(entry.getKey(), value);
						dataList.add(newData);
					}
					catch (Exception e) {
						LOG.warn("", e);
					}
				}
				series.getData().addAll(dataList);
				if (series.getData().size() > 1) {
					long time = series.getData().get(series.getData().size() - 1).getXValue().longValue();
					// axis
					xAxis.setLowerBound(time - TIME_FRAME);
					xAxis.setUpperBound(time);
					pendingMap.clear();
				}
				ArrayList<Data<Number, Number>> removeList = null;
				try {
					for (Data<Number, Number> data : series.getData()) {
						if ((long) data.getXValue() < ((NumberAxis) chart.getXAxis()).getLowerBound() - 100) {
							if (removeList == null) {
								removeList = new ArrayList<>();
							}
							removeList.add(data);
						} else {
							break;
						}
					}
				}
				catch (Exception e) {
					// LOG.error("", e);
				}
				if (removeList != null) {
					series.getData().removeAll(removeList);
				}
			}
			if (treshold.getData().size() >= 2) {
				treshold.getData().get(1).setXValue(xAxis.getUpperBound() + 10000);
			}
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

	public void setThreshold(double value) {
		value = Math.abs(RTAViewController.FFT_MIN) - value;
		NumberAxis time = (NumberAxis) chart.getXAxis();
		treshold.getData().clear();
		treshold.getData().add(new Data<>(time.getLowerBound(), value));
		treshold.getData().add(new Data<>(time.getUpperBound() + 10000, value));
	}
}
