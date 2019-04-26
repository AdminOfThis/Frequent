package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.bpmdetect.BeatDetector;
import data.DrumTrigger;
import data.Input;
import gui.FXMLUtil;
import gui.pausable.PausableView;
import gui.utilities.DrumTriggerListener;
import gui.utilities.controller.DrumTriggerItem;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class DrumViewController implements Initializable, PausableView, DrumTriggerListener {

	private static final Logger									LOG				= Logger.getLogger(DrumViewController.class);
	private static final long									DRUM_TIME_FRAME	= 5000000000l;
	@FXML
	private ScatterChart<Number, Number>						drumChart;
	@FXML
	private VBox												triggerPane;
	@FXML
	private ScrollPane											sidePane;
	@FXML
	private ToggleButton										btnSetup;
	@FXML
	private Label												lblBPM;
	@FXML
	private HBox												bpmBox;
	private List<DrumTrigger>									triggerList		= Collections.synchronizedList(new ArrayList<>());
	private Map<DrumTrigger, XYChart.Series<Number, Number>>	seriesMap		= Collections.synchronizedMap(new HashMap<>());
	private Map<DrumTrigger, ArrayList<Data<Number, Number>>>	pendingMap		= Collections.synchronizedMap(new HashMap<>());
	private boolean												paused			= false;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		LOG.debug("Loading DrumController");
		// MainController.getInstance().setDrumController(this);
		sidePane.visibleProperty().bind(btnSetup.selectedProperty());
		sidePane.managedProperty().bind(btnSetup.selectedProperty());
		if (ASIOController.getInstance() == null) {
			bpmBox.setVisible(false);
			bpmBox.setManaged(false);
		}
		initData();
		initChart();
		initTimer();
	}

	private void addTrigger(final DrumTrigger trigger) {
		triggerList.add(trigger);
		trigger.addListeners(this);
		DrumTriggerItem triggerItem = new DrumTriggerItem(trigger);
		triggerPane.getChildren().add(triggerItem);
		VBox.setVgrow(triggerItem, Priority.SOMETIMES);
		if (seriesMap.get(trigger) == null) {
			Series<Number, Number> series = new Series<>();
			seriesMap.put(trigger, series);
			drumChart.getData().add(series);
		}
	}

	@Override
	public ArrayList<Node> getHeader() {
		ArrayList<Node> res = new ArrayList<>();
		res.add(btnSetup);
		return res;
	}

	private void initChart() {
		NumberAxis timeAxis = (NumberAxis) drumChart.getXAxis();
		timeAxis.setForceZeroInRange(false);
		timeAxis.setAutoRanging(false);
		timeAxis.setTickUnit(DRUM_TIME_FRAME / 10.0);
		timeAxis.setOpacity(0.0);
		timeAxis.setPrefHeight(0.0);
		// drumchart
		NumberAxis y = (NumberAxis) drumChart.getYAxis();
		y.setUpperBound(triggerList.size() + 0.5);
		y.setTickLabelFormatter(new StringConverter<Number>() {

			@Override
			public Number fromString(final String string) {
				for (DrumTrigger trig : triggerList) {
					if (trig.getName().equals(string)) { return triggerList.indexOf(trig); }
				}
				return null;
			}

			@Override
			public String toString(final Number object) {
				DrumTrigger trig = null;
				try {
					trig = triggerList.get((int) Math.round((double) object - 1));
				}
				catch (Exception e) {}
				if (trig != null) { return trig.getName(); }
				return null;
			}
		});
		drumChart.getStyleClass().add("drumChart");
	}

	private void initData() {
		if (!BeatDetector.isInitialized()) {
			BeatDetector.initialize();
		}
		for (DrumTrigger trigger : BeatDetector.getInstance().getTriggerList()) {
			addTrigger(trigger);
		}
	}

	private void initTimer() {
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				updateDrumChart();
			}
		};
		timer.start();
	}

	@Override
	public boolean isPaused() {
		return paused;
	}

	@Override
	public void pause(final boolean pause) {
		paused = pause;
	}

	@Override
	public void refresh() {
		// not needed
	}

	@Override
	public void setSelectedChannel(final Input in) {
		// not needed
	}

	public void show() {
		Stage stage = (Stage) btnSetup.getScene().getWindow();
		if (!stage.isShowing()) {
			stage.show();
			stage.requestFocus();
		}
	}

	private void updateDrumChart() {
		if (ASIOController.getInstance() != null) {
			// Adding pending entries
			synchronized (pendingMap) {
				for (Entry<DrumTrigger, ArrayList<Data<Number, Number>>> listEntry : pendingMap.entrySet()) {
					Series<Number, Number> series = seriesMap.get(listEntry.getKey());
					ArrayList<Data<Number, Number>> dataList = new ArrayList<>();
					for (Data<Number, Number> data : listEntry.getValue()) {
						dataList.add(data);
					}
					series.getData().addAll(dataList);
				}
				pendingMap.clear();
			}
			// udating xAxis
			NumberAxis xAxis = (NumberAxis) drumChart.getXAxis();
			FXMLUtil.updateAxis(xAxis, DRUM_TIME_FRAME, ASIOController.getInstance().getTime());
			// removing old data
			for (Series<Number, Number> series : drumChart.getData()) {
				FXMLUtil.removeOldData((long) xAxis.getLowerBound(), series);
			}
			double bpm = BeatDetector.getInstance().getBPM();
			if (bpm > 0) {
				lblBPM.setText(BeatDetector.getInstance().getBPMString());
			} else {
				lblBPM.setText("Unknown");
			}
		}
	}

	@Override
	public void tresholdReached(DrumTrigger trigger, double level, double treshold, long time) {
		// LOG.info("Adding Drum Entry " + trig.getName() + ", " + value);
		synchronized (pendingMap) {
			if (pendingMap.get(trigger) == null) {
				pendingMap.put(trigger, new ArrayList<>());
			}
			pendingMap.get(trigger).add(new XYChart.Data<>(time, triggerList.indexOf(trigger) + 1));
		}
	}
}
