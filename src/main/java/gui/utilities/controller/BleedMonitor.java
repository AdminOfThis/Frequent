package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.BleedAnalyzer;
import data.Channel;
import gui.FXMLUtil;
import gui.controller.BleedViewController;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.AutoCompleteComboBoxListener;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import main.Constants;

public class BleedMonitor extends AnchorPane implements Initializable, PausableComponent {

	private static final Logger			LOG			= LogManager.getLogger(BleedMonitor.class);
	private static final String			FXML		= "/fxml/utilities/BleedMonitor.fxml";
	private boolean						pause		= false;
	private Pausable					parent;
	@FXML
	private AnchorPane					vuPane;
	@FXML
	private Pane						bleedPane;
	@FXML
	private VBox						bleedTopPane;
	@FXML
	private ComboBox<Channel>			combo;
	@FXML
	private HBox						root;
	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series1		= new Series<>();
	private Series<Number, Number>		series2		= new Series<>();
	private boolean						maximized	= false;
	private VuMeterMono					vuMeter;
	private BleedAnalyzer				analyzer;

	public BleedMonitor() {
		analyzer = new BleedAnalyzer();
		analyzer.setParentPausable(this);
		LOG.debug("Creating new BleedMonitor");
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				if (!isPaused()) {
					update();
				}
			}
		};
		timer.start();
		this.setOnMouseClicked(e -> {
			if (e.getClickCount() == 1) {
				boolean currentMax = maximized;
				BleedViewController.getInstance().minimizeAll();
				maximize(!currentMax);
			}
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		new AutoCompleteComboBoxListener<>(combo);
		combo.setConverter(Constants.CHANNEL_CONVERTER);
		combo.valueProperty().addListener(e -> {
			vuMeter.setChannel(combo.getValue());
			analyzer.setSecondaryChannel(combo.getValue());
		});
		refresh();
		bleedPane.setPrefHeight(.0);
		vuMeter = new VuMeterMono(null, Orientation.VERTICAL);
		vuMeter.setParentPausable(this);
		vuPane.getChildren().add(vuMeter);
		AnchorPane.setBottomAnchor(vuMeter, .0);
		AnchorPane.setTopAnchor(vuMeter, .0);
		AnchorPane.setLeftAnchor(vuMeter, .0);
		AnchorPane.setRightAnchor(vuMeter, .0);
		chart.getData().add(series1);
		chart.getData().add(series2);
		maximize(false);
	}

	public void setChannel(Channel channel) {
		vuMeter.setChannel(channel);
		analyzer.setSecondaryChannel(channel);
	}

	public void setPrimaryChannel(Channel c) {
		analyzer.setPrimaryChannel(c);
	}

	public void refresh() {
		if (ASIOController.getInstance() != null) {
			combo.getItems().addAll(ASIOController.getInstance().getInputList());
		}
	}

	private void update() {
		series1.getData().clear();
		series2.getData().clear();
		ArrayList<Data<Number, Number>> toDo1 = new ArrayList<>();
		float[] newData1 = Arrays.copyOf(analyzer.getNewData1(), analyzer.getNewData1().length);
		for (int i = 0; i < ASIOController.getInstance().getBufferSize(); i++) {
			toDo1.add(new Data<>(i, newData1[newData1.length - ASIOController.getInstance().getBufferSize() + i]));
		}
		series1.getData().addAll(toDo1);
		float[] newData2 = Arrays.copyOf(analyzer.getNewData2(), analyzer.getNewData2().length);
		ArrayList<Data<Number, Number>> toDo2 = new ArrayList<>();
		for (int i = 0; i < ASIOController.getInstance().getBufferSize(); i++) {
			toDo2.add(new Data<>(i, newData2[newData2.length - ASIOController.getInstance().getBufferSize() + i]));
		}
		series2.getData().addAll(toDo2);
		updateBleedMonitor(analyzer.getEqual());
	}

	private void updateBleedMonitor(final double percent) {
		// making sure percent is between 0 and 1
		double percentCorr = Math.min(1.0, Math.max(0.0, percent));
		bleedPane.setPrefHeight(percentCorr * bleedTopPane.getHeight());
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		// TODO Auto-generated method stub
		return pause || (parent != null && parent.isPaused()) || ASIOController.getInstance() == null;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parent = parent;
	}

	public void maximize(boolean value) {
		maximized = value;
		if (maximized) {
			HBox.setHgrow(this, Priority.ALWAYS);
			if (!root.getChildren().contains(chart)) {
				root.getChildren().add(chart);
			}
		} else {
			HBox.setHgrow(this, Priority.NEVER);
			root.getChildren().remove(chart);
		}
	}
}
