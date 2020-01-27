package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.InputListener;
import data.Channel;
import data.Group;
import data.Input;
import gui.FXMLUtil;
import gui.pausable.PausableView;
import gui.utilities.controller.VuMeterMono;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import main.Constants;
import main.Main;

public class GroupViewController implements Initializable, PausableView {

	private static final Logger LOG = LogManager.getLogger(GroupViewController.class);
	private static final long TIME_FRAME = 600000L;
	private static GroupViewController instance;
	public static GroupViewController getInstance() {
		return instance;
	}
	@FXML
	private SplitPane root;
	@FXML
	private SplitPane groupPane;
	@FXML
	private LineChart<Number, Number> chart;
	@FXML
	private ToggleButton tglTimed;
	private Map<Group, Map<Long, Double>> pendingMap = Collections.synchronizedMap(new HashMap<>());
	private Map<Group, Series<Number, Number>> groupSeriesMap = Collections.synchronizedMap(new HashMap<>());

	private boolean pause = true;

	@Override
	public ArrayList<Region> getHeader() {
		ArrayList<Region> res = new ArrayList<>();
		res.add(tglTimed);
		return res;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		instance = this;
		initChart();
	}

	@Override
	public boolean isPaused() {
		return pause;
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
		groupPane.getItems().clear();
		boolean redrawChart = ASIOController.getInstance().getGroupList().size() != chart.getData().size();
		if (!redrawChart) {
			for (Group g : ASIOController.getInstance().getGroupList()) {
				boolean found = false;
				for (Series<Number, Number> s : chart.getData()) {
					if (s.getName().equals(g.getName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					redrawChart = true;
					break;
				}
			}
		}
		if (redrawChart) {
			chart.getData().clear();
		}
		ArrayList<Group> groupList = ASIOController.getInstance().getGroupList();
		for (Group g : groupList) {
			redrawGroup(redrawChart, groupList, g);
		}
		// smoothing out splitPane
		int divCount = groupPane.getDividerPositions().length;
		double equalSize = 1.0 / (divCount + 1);
		double divPosValues[] = new double[divCount];
		for (int count = 1; count < divCount + 1; count++) {
			divPosValues[count - 1] = equalSize * count;
		}
		groupPane.setDividerPositions(divPosValues);
	}

	@Override
	public void setSelectedChannel(Input in) {
		// nothing do do, shows all channels anyway
	}

	private void addNewData(Map<Long, Double> pendingMap, final Series<Number, Number> series, final Group group) {
		ArrayList<Data<Number, Number>> dataList = new ArrayList<>();
		for (Entry<Long, Double> entry : pendingMap.entrySet()) {
			try {
				double level = entry.getValue();
				long time = entry.getKey();
				if (!series.getNode().getStyle().equals("-fx-stroke: " + group.getColor())) {
					String color = group.getColor();
					if (color == null) {
						color = "-fx-accent";
					}
					series.getNode().setStyle("-fx-stroke: " + color);
				}
				double leveldB = Channel.percentToDB(level);
				leveldB = Math.max(leveldB, Constants.FFT_MIN);
				Data<Number, Number> data = new Data<>(time, leveldB);
				dataList.add(data);
			} catch (Exception e) {
				LOG.warn("", e);
			}
		}
		pendingMap.clear();
		series.getData().addAll(dataList);

	}

	private Node createChannelVu(Input c) {
		VuMeterMono channelMeter = new VuMeterMono(c, Orientation.VERTICAL);
		channelMeter.setParentPausable(this);
		channelMeter.setMinWidth(40.0);
		HBox.setHgrow(channelMeter, Priority.ALWAYS);
		return channelMeter;
	}

	private Node createGroupVu(Group g) {
		VuMeterMono groupMeter = new VuMeterMono(g, Orientation.VERTICAL);
		groupMeter.setParentPausable(this);
		groupMeter.setMinWidth(40.0);
		groupMeter.setPadding(new Insets(0, 20, 0, 0));
		HBox.setHgrow(groupMeter, Priority.ALWAYS);

		return groupMeter;
	}

	private void initChart() {
		root.getItems().remove(chart);
		tglTimed.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			if (newValue) {
				root.getItems().add(chart);
			} else {
				root.getItems().remove(chart);
			}
		});
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(0.0);
		yAxis.setLowerBound(Constants.FFT_MIN);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				new Thread(() -> update()).start();
			}
		};
		timer.start();
	}

	private void redrawChart(final Group group) {
		Series<Number, Number> series = new Series<>();
		groupSeriesMap.put(group, series);
		series.setName(group.getName());
		chart.getData().add(series);
		synchronized (pendingMap) {
			// pendingMap for new group added
			if (pendingMap.get(group) == null) {
				pendingMap.put(group, Collections.synchronizedMap(new LinkedHashMap<Long, Double>()));
			} else {
				pendingMap.get(group).clear();
			}
		}

		group.addListener(new InputListener() {

			@Override
			public void colorChanged(String newColor) {}

			@Override
			public void levelChanged(Input input, double level, long time) {
				synchronized (pendingMap.get(group)) {
					pendingMap.get(group).put(time, level);
				}
			}

			@Override
			public void nameChanged(String name) {}
		});
	}

	private void redrawGroup(final boolean redrawChart, final ArrayList<Group> groupList, final Group g) {
		// groups
		if (g.getColor() == null || g.getColor().isEmpty()) {
			g.setColor(FXMLUtil.deriveColor(Main.getAccentColor(), groupList.indexOf(g) + 1, groupList.size() + 1));
		}

		// individual channels
		HBox groupBox = new HBox();
		groupBox.setSpacing(5.0);
		ScrollPane scroll = new ScrollPane(groupBox);
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		groupPane.getItems().add(scroll);

		SplitPane.setResizableWithParent(scroll, false);
		for (Channel c : g.getChannelList()) {
			groupBox.getChildren().add(createChannelVu(c));

		}
		groupBox.getChildren().add(0, createGroupVu(g));
		// adding chart series
		if (redrawChart) {
			redrawChart(g);
		}
	}

	private void update() {
		if (!isPaused()) {
			NumberAxis xAxis = (NumberAxis) chart.getXAxis();
			for (Group g : pendingMap.keySet()) {
				Series<Number, Number> series = groupSeriesMap.get(g);
				synchronized (pendingMap.get(g)) {
					addNewData(pendingMap.get(g), series, g);
				}
				FXMLUtil.updateAxis(xAxis, TIME_FRAME, ASIOController.getInstance().getTime());
				FXMLUtil.removeOldData((long) xAxis.getLowerBound(), series);
			}
		}
	}
}
