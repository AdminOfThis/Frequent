package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.BleedAnalyzer;
import data.Channel;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.AutoCompleteComboBoxListener;
import gui.utilities.Constants;
import gui.utilities.FXMLUtil;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class BleedMonitor extends AnchorPane implements Initializable, PausableComponent {

	private static final Logger	LOG		= Logger.getLogger(BleedMonitor.class);
	private static final String	FXML	= "/fxml/utilities/BleedMonitor.fxml";
	private boolean				pause	= false;
	private Pausable			parent;
	@FXML
	private AnchorPane			vuPane;
	@FXML
	private Pane				bleedPane;
	@FXML
	private VBox				bleedTopPane;
	@FXML
	private ComboBox<Channel>	combo;
	private VuMeterMono			vuMeter;
	private BleedAnalyzer		analyzer;

	public BleedMonitor() {
		analyzer = new BleedAnalyzer();
		analyzer.setParentPausable(this);
		LOG.debug("Creating new BleedMonitor");
		Parent p = FXMLUtil.loadFXML(FXML, this);
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
		updateBleedMonitor(analyzer.getEqual());
	}

	private void updateBleedMonitor(double percent) {
		// making sure percent is between 0 and 1
		percent = Math.min(1.0, percent);
		percent = Math.max(0.0, percent);
		bleedPane.setPrefHeight(percent * bleedTopPane.getHeight());
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		// TODO Auto-generated method stub
		return pause || (parent != null && parent.isPaused());
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parent = parent;
	}
}
