package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Channel;
import data.DrumTrigger;
import gui.pausable.PausableView;
import gui.utilities.FXMLUtil;
import gui.utilities.NegativeAreaChart;
import gui.utilities.controller.DrumTriggerItemController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class DrumViewController implements Initializable, PausableView {

	private static final Logger										LOG					= Logger.getLogger(DrumViewController.class);
	private static final String										DRUM_ITEM_PATH		= "/gui/utilities/gui/DrumTriggerItem.fxml";
	private static final int										REFRESH_RATE		= 10;
	private static final double										TIME_FRAME			= 7500;
	private static final double										DRUM_TIME_FRAME		= 5000;
	private static final double										MIN_HEIGHT_TRESHOLD	= 38.0;
	private static final double										CHART_SYMBOL_SIZE	= 25.0;
	@FXML
	private AnchorPane												chartPane;
	@FXML
	private ScatterChart<Number, Number>							drumChart;
	@FXML
	private Pane													treshold;
	@FXML
	private VBox													triggerPane, sidePane;
	@FXML
	private Label													label;
	@FXML
	private ToggleButton											btnSetup;
	private XYChart<Number, Number>									chart;
	private Series<Number, Number>									rmsSeries			= new Series<>();
	private Timeline												line;
	private DrumTrigger												activeChartChannel;
	private ArrayList<DrumTrigger>									triggerList			= new ArrayList<>();
	private HashMap<DrumTrigger, XYChart.Series<Number, Number>>	seriesMap			= new HashMap<>();
	private boolean													paused				= false;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.debug("Loading DrumController");
		// MainController.getInstance().setDrumController(this);
		sidePane.visibleProperty().bind(btnSetup.selectedProperty());
		sidePane.managedProperty().bind(btnSetup.selectedProperty());
		initData();
		initChart();
		initTimer();
	}

	private void initData() {
		for (String name : DrumTrigger.DEFAULT_NAMES) {
			DrumTrigger trigger = new DrumTrigger(name);
			addTrigger(trigger);
		}
	}

	private void initTimer() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (activeChartChannel != null && activeChartChannel.getChannel() != null) {
				updateRmsChart();
			} else {
				rmsSeries.getData().clear();
			}
			updateDrumChart();
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Timeline.INDEFINITE);
		line.play();
	}

	private void updateRmsChart() {
		float level = activeChartChannel.getChannel().getLevel();
		level = (float) Channel.percentToDB(level * 1000.0);
		rmsSeries.getData().add(new XYChart.Data<>(System.currentTimeMillis(), level));
		ArrayList<XYChart.Data<Number, Number>> removeList = null;
		long time = System.currentTimeMillis();
		boolean continueFlag = true;
		int count = 0;
		while (continueFlag) {
			XYChart.Data<Number, Number> data = rmsSeries.getData().get(count);
			if (((long) data.getXValue()) < (time - TIME_FRAME - 1000)) {
				if (removeList == null) {
					removeList = new ArrayList<>();
				}
				removeList.add(data);
			} else {
				continueFlag = false;
			}
			count++;
		}
		if (removeList != null) {
			rmsSeries.getData().removeAll(removeList);
		}
		// AXIS
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		xAxis.setLowerBound(time - TIME_FRAME);
		xAxis.setUpperBound(time + 1000);
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
				boolean continueFlag = true;
				int count = 0;
				while (continueFlag) {
					Data<Number, Number> data = series.getData().get(count);
					StackPane stackPane = (StackPane) data.getNode();
					if (stackPane != null) {
						stackPane.setPrefWidth(CHART_SYMBOL_SIZE);
						stackPane.setPrefHeight(CHART_SYMBOL_SIZE);
					}
					if (((long) data.getXValue()) < (time - DRUM_TIME_FRAME - 1000)) {
						if (removeList == null) {
							removeList = new ArrayList<>();
						}
						removeList.add(data);
					} else {
						continueFlag = false;
					}
					count++;
				}
				if (removeList != null) {
					series.getData().removeAll(removeList);
				}
			}
		}
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
		chart = new NegativeAreaChart(timeAxis, yaxis);
		chart.setAnimated(false);
		chart.getStyleClass().add("transparent");
		((AreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setLegendVisible(false);
		chart.setLegendSide(Side.RIGHT);
		chart.setHorizontalZeroLineVisible(false);
		chartPane.getChildren().add(chart);
		HBox.setHgrow(chart, Priority.ALWAYS);
		AnchorPane.setBottomAnchor(chart, 0.0);
		AnchorPane.setTopAnchor(chart, 0.0);
		AnchorPane.setLeftAnchor(chart, 0.0);
		AnchorPane.setRightAnchor(chart, 0.0);
		// dirty hack to get other css properties to real series
		chart.getData().add(new Series<>());
		chart.getData().add(rmsSeries);
		// drumchart
		NumberAxis y = (NumberAxis) drumChart.getYAxis();
		y.setUpperBound(triggerList.size() + 0.5);
		y.setTickLabelFormatter(new StringConverter<Number>() {

			@Override
			public String toString(Number object) {
				DrumTrigger trig = null;
				try {
					trig = triggerList.get((int) Math.round((double) object - 1));
				}
				catch (Exception e) {}
				if (trig != null) { return trig.getName(); }
				return null;
			}

			@Override
			public Number fromString(String string) {
				for (DrumTrigger trig : triggerList) {
					if (trig.getName().equals(string)) { return triggerList.indexOf(trig); }
				}
				return null;
			}
		});
	}

	private void setTreshold(double value) {
		double percent = 1 - (Math.abs(value) / 60.0);
		NumberAxis axis = (NumberAxis) chart.getYAxis();
		double height = axis.getHeight() * percent;
		treshold.setPrefHeight(MIN_HEIGHT_TRESHOLD + height);
	}

	private void addTrigger(DrumTrigger trigger) {
		triggerList.add(trigger);
		Parent p = FXMLUtil.loadFXML(DRUM_ITEM_PATH);
		triggerPane.getChildren().add(p);
		DrumTriggerItemController con = (DrumTriggerItemController) FXMLUtil.getController();
		con.setDrumController(this);
		con.setTrigger(trigger);
		if (seriesMap.get(trigger) == null) {
			Series<Number, Number> series = new Series<>();
			seriesMap.put(trigger, series);
			drumChart.getData().add(series);
		}
	}

	public void redrawThreshold() {
		if (activeChartChannel != null) {
			setTreshold(activeChartChannel.getTreshold());
		}
	}

	public void setActiveTrigger(DrumTrigger trigger) {
		if (triggerList.contains(trigger)) {
			activeChartChannel = trigger;
			label.setText(activeChartChannel.getName());
			setTreshold(trigger.getTreshold());
			rmsSeries.getData().clear();
		}
	}

	public void addEntry(DrumTrigger trig, double value) {
		// LOG.info("Adding Drum Entry " + trig.getName() + ", " + value);
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Series<Number, Number> series = seriesMap.get(trig);
				if (series != null) {
					Data<Number, Number> data = new XYChart.Data<>(System.currentTimeMillis(), triggerList.indexOf(trig) + 1);
					series.getData().add(data);
				}
			}
		});
	}

	public void show() {
		Stage stage = (Stage) btnSetup.getScene().getWindow();
		if (!stage.isShowing()) {
			stage.show();
			stage.requestFocus();
		}
	}

	@Override
	public void pause(boolean pause) {
		paused = pause;
	}

	@Override
	public boolean isPaused() {
		return paused;
	}

	@Override
	public ArrayList<Node> getHeader() {
		return null;
	}
}
