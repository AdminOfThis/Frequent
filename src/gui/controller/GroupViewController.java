package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;

import control.ASIOController;
import control.InputListener;
import data.Channel;
import data.Group;
import gui.pausable.PausableView;
import gui.utilities.controller.VuMeter;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import main.Main;

public class GroupViewController implements Initializable, PausableView {

	private static final Logger			LOG		= Logger.getLogger(GroupViewController.class);
	@FXML
	private SplitPane					root;
	@FXML
	private SplitPane					rootSplit;
	@FXML
	private SplitPane					groupPane;
	@FXML
	private HBox						vuPane;
	@FXML
	private LineChart<Number, Number>	chart;
	@FXML
	private ToggleButton				tglTimed;
	private boolean						pause	= true;
	private static GroupViewController	instance;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		initChart();
	}

	private void initChart() {
		root.getItems().remove(chart);
		tglTimed.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					root.getItems().add(chart);
				} else {
					root.getItems().remove(chart);
				}
			}
		});
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(0.0);
		yAxis.setLowerBound(RTAViewController.FFT_MIN);
	}

	public static GroupViewController getInstance() {
		return instance;
	}

	@Override
	public void refresh() {
		if (ASIOController.getInstance() != null) {
			vuPane.getChildren().clear();
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
	}

	private void redrawGroup(boolean redrawChart, ArrayList<Group> groupList, Group g) {
		// groups
		// if (g.getColor() == null || g.getColor().isEmpty()) {
		g.setColor(MainController.deriveColor(Main.getAccentColor(), groupList.indexOf(g) + 1,
		        groupList.size() + 1));
		// }
		VuMeter groupMeter = new VuMeter(g, Orientation.VERTICAL);
		groupMeter.setParentPausable(this);
		groupMeter.setMinWidth(40.0);
		vuPane.getChildren().add(groupMeter);
		HBox.setHgrow(groupMeter, Priority.ALWAYS);
		// individual channels
		HBox groupBox = new HBox();
		groupBox.setSpacing(5.0);
		ScrollPane scroll = new ScrollPane(groupBox);
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		groupPane.getItems().add(scroll);
		SplitPane.setResizableWithParent(scroll, false);
		for (Channel c : g.getChannelList()) {
			VuMeter channelMeter = new VuMeter(c, Orientation.VERTICAL);
			channelMeter.setParentPausable(this);
			channelMeter.setMinWidth(40.0);
			VBox.setVgrow(channelMeter, Priority.ALWAYS);
			groupBox.getChildren().add(channelMeter);
			HBox.setHgrow(channelMeter, Priority.ALWAYS);
		}
		// adding chart series
		if (redrawChart) {
			Series<Number, Number> series = new Series<>();
			series.setName(g.getName());
			chart.getData().add(series);
			Legend legend = (Legend) chart.lookup(".chart-legend");
			for (LegendItem i : legend.getItems()) {
				if (i.getText().equals(g.getName())) {
					if (g.getColor() == null) {
						i.setSymbol(new Rectangle(10, 4));
						i.getSymbol().setStyle("-fx-fill: -fx-accent");
					} else {
						i.setSymbol(new Rectangle(10, 4, Color.web(g.getColor())));
					}
				}
			}
			NumberAxis xAxis = (NumberAxis) chart.getXAxis();
			// adding listener to group for chart
			g.addListener(new InputListener() {

				@Override
				public void levelChanged(double level) {
					Platform.runLater(() -> {
						if (!series.getNode().getStyle().equals("-fx-stroke: " + g.getColor())) {
							String color = g.getColor();
							if (color == null) {
								color = "-fx-accent";
							}
							series.getNode().setStyle("-fx-stroke: " + color);
							for (LegendItem i : legend.getItems()) {
								if (i.getText().equals(g.getName())) {
									i.setSymbol(new Rectangle(10, 4));
									i.getSymbol().setStyle("-fx-fill: " + color);
								}
							}
						}
						double leveldB = Channel.percentToDB(level);
						leveldB = Math.max(leveldB, RTAViewController.FFT_MIN);
						long time = System.currentTimeMillis();
						series.getData().add(new Data<Number, Number>(time, leveldB));
						// removing old data
						while (series.getData().size() > 500) {
							series.getData().remove(0);
						}
						xAxis.setUpperBound(time + 100);
						xAxis.setLowerBound((long) series.getData().get(0).getXValue());
					});
				}
			});
		}
	}

	@Override
	public void pause(boolean pause) {
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
	public boolean isPaused() {
		return pause;
	}

	@Override
	public ArrayList<Node> getHeader() {
		ArrayList<Node> res = new ArrayList<>();
		res.add(tglTimed);
		return res;
	}
}
