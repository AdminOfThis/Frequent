package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.DrumTrigger;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.controller.DrumTriggerItem;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class DrumViewController implements Initializable, PausableView {

	private static final Logger										LOG				= Logger.getLogger(DrumViewController.class);
	private static final int										REFRESH_RATE	= 10;
	private static final double										DRUM_TIME_FRAME	= 5000;
	@FXML
	private AnchorPane												chartPane;
	@FXML
	private ScatterChart<Number, Number>							drumChart;
	@FXML
	private Pane													treshold;
	@FXML
	private VBox													triggerPane;
	@FXML
	private ScrollPane												sidePane;
	@FXML
	private ToggleButton											btnSetup;
	private ArrayList<DrumTrigger>									triggerList		= new ArrayList<>();
	private HashMap<DrumTrigger, XYChart.Series<Number, Number>>	seriesMap		= new HashMap<>();
	private boolean													paused			= false;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		LOG.debug("Loading DrumController");
		// MainController.getInstance().setDrumController(this);
		sidePane.visibleProperty().bind(btnSetup.selectedProperty());
		sidePane.managedProperty().bind(btnSetup.selectedProperty());
		initData();
		initChart();
		initTimer();
	}

	public void addEntry(final DrumTrigger trig, final double value) {
		// LOG.info("Adding Drum Entry " + trig.getName() + ", " + value);
		Platform.runLater(() -> {
			Series<Number, Number> series = seriesMap.get(trig);
			if (series != null) {
				Data<Number, Number> data = new XYChart.Data<>(System.currentTimeMillis(), triggerList.indexOf(trig) + 1);
				series.getData().add(data);
			}
		});
	}

	private void addTrigger(final DrumTrigger trigger) {
		triggerList.add(trigger);
		DrumTriggerItem triggerItem = new DrumTriggerItem(trigger);
		triggerItem.setDrumController(this);
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
		ValueAxis<Number> yaxis = new NumberAxis(-60, 0, 6);
		yaxis.setPrefWidth(20.0);
		// yaxis.setAutoRanging(true);
		// yaxis.setOpacity(0.0);
		yaxis.setAnimated(true);
		NumberAxis timeAxis = new NumberAxis();
		timeAxis.setForceZeroInRange(false);
		timeAxis.setAutoRanging(false);
		timeAxis.setTickUnit(5000);
		timeAxis.setOpacity(0.0);
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
		for (String name : DrumTrigger.DEFAULT_NAMES) {
			DrumTrigger trigger = new DrumTrigger(name);
			addTrigger(trigger);
		}
	}

	private void initTimer() {
		Timeline line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			updateDrumChart();
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Animation.INDEFINITE);
		line.play();
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
	public void setSelectedChannel(final Input in) {}

	public void show() {
		Stage stage = (Stage) btnSetup.getScene().getWindow();
		if (!stage.isShowing()) {
			stage.show();
			stage.requestFocus();
		}
	}

	private void updateDrumChart() {
		long time = System.currentTimeMillis();
		NumberAxis xAxis = (NumberAxis) drumChart.getXAxis();
		xAxis.setLowerBound(time - DRUM_TIME_FRAME);
		xAxis.setUpperBound(time);
		// HashMap<Series, ArrayList<Data>> removeMap = new HashMap<>(6);
		for (Series<Number, Number> series : drumChart.getData()) {
			if (!series.getData().isEmpty()) {
				ArrayList<Data<Number, Number>> removeList = null;
				for (Data<Number, Number> data : series.getData()) {
					if ((long) data.getXValue() < time - DRUM_TIME_FRAME - 1000) {
						if (removeList == null) {
							removeList = new ArrayList<>();
						}
						removeList.add(data);
					} else {
						break;
					}
				}
				if (removeList != null) {
					series.getData().removeAll(removeList);
				}
			}
		}
	}
}
