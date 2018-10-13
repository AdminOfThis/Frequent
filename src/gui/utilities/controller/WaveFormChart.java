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
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.FXMLUtil;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.AnchorPane;

public class WaveFormChart extends AnchorPane implements Initializable, InputListener, PausableComponent {

	private static final Logger			LOG			= Logger.getLogger(WaveFormChart.class);
	private static final String			FXML		= "/gui/utilities/gui/WaveFormChart.fxml";
	private static final long			TIME_FRAME	= 5000000000l;
	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series		= new Series<>();
	private Input						channel;
	private double						value		= 1;
	private boolean						negative	= false;
	private boolean						pause		= false;
	private Pausable					pausableParent;
	private boolean						styleSet	= false;
	private Map<Long, Double>			pendingMap;

	public WaveFormChart() {
		Parent p = FXMLUtil.loadFXML(FXML, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		pendingMap = Collections.synchronizedMap(new HashMap<Long, Double>());
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				update();
			}
		};
		timer.start();
	}

	private void update() {
		if (!isPaused()) {
			synchronized (pendingMap) {
				for (Entry<Long, Double> entry : pendingMap.entrySet()) {
					try {
						if (!styleSet) {
							series.getNode().setStyle("-fx-stroke: -fx-accent; -fx-stroke-width: 1px;");
							styleSet = true;
						}
						NumberAxis xAxis = (NumberAxis) chart.getXAxis();
						value = 0;
						if (channel != null) {
							value = entry.getValue();
						}
						if (negative) {
							value = -value;
						}
						negative = !negative;
						Data<Number, Number> newData = new Data<>(entry.getKey(), value);
						series.getData().add(newData);
						long time = System.nanoTime();
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
// LOG.error("", e);
						}
						if (removeList != null) {
							series.getData().removeAll(removeList);
						}
					}
					catch (Exception e) {
						LOG.warn("", e);
					}
				}
			}
			pendingMap.clear();
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		yAxis.setAutoRanging(true);
		chart.getData().add(series);
		((NumberAxis) chart.getXAxis()).setTickUnit(50000000000l);
	}

	public void setChannel(Input c) {
		try {
			if (!c.equals(channel)) {
				if (channel != null) {
					channel.removeListener(this);
				}
				series.getData().clear();
				this.channel = c;
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
	public void levelChanged(double level) {
		pendingMap.put(System.nanoTime(), Channel.percentToDB(level));
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
		if (pause) {
			LOG.debug(getClass().getSimpleName() + "; playing animations");
		} else {
			LOG.debug(getClass().getSimpleName() + "; pausing animations");
		}
	}

	@Override
	public boolean isPaused() {
		return pause || (pausableParent != null && pausableParent.isPaused()) || channel == null || pendingMap == null;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		pausableParent = parent;
	}
}
