package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.AutoCompleteComboBoxListener;
import gui.utilities.controller.BleedMonitor;
import gui.utilities.controller.VuMeterMono;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import main.Constants;

public class BleedViewController implements Initializable, PausableView {

	private static final Logger			LOG		= LogManager.getLogger(BleedViewController.class);
	private boolean						pause	= true;
	@FXML
	private HBox						topBox;
	@FXML
	private AnchorPane					primaryVuPane;
	@FXML
	private HBox						content;
	@FXML
	private ComboBox<Channel>			primaryCombo;
	private VuMeterMono					primaryMeter;
	private static BleedViewController	instance;

	public static BleedViewController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		LOG.info("Initializing BleedView");
		primaryMeter = new VuMeterMono(null, Orientation.VERTICAL);
		primaryMeter.setTitle("Primary");
		primaryVuPane.getChildren().add(primaryMeter);
		AnchorPane.setBottomAnchor(primaryMeter, .0);
		AnchorPane.setTopAnchor(primaryMeter, .0);
		AnchorPane.setLeftAnchor(primaryMeter, .0);
		AnchorPane.setRightAnchor(primaryMeter, .0);
		if (ASIOController.getInstance() != null) {
			primaryCombo.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		primaryCombo.setConverter(Constants.CHANNEL_CONVERTER);
		primaryCombo.valueProperty().addListener(e -> {
			primaryMeter.setChannel(primaryCombo.getValue());
			for (Node n : content.getChildren()) {
				if (n instanceof BleedMonitor) {
					((BleedMonitor) n).setPrimaryChannel(primaryCombo.getValue());
				}
			}
		});
		new AutoCompleteComboBoxListener<>(primaryCombo);
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public ArrayList<Node> getHeader() {
		ArrayList<Node> topList = new ArrayList<>();
		topList.addAll(topBox.getChildren());
		return topList;
	}

	@Override
	public void refresh() {
		if (ASIOController.getInstance() != null) {
			primaryCombo.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		for (Node n : content.getChildren()) {
			if (n instanceof BleedMonitor) {
				((BleedMonitor) n).refresh();
			}
		}
	}

	@Override
	public void setSelectedChannel(Input in) {
		// do nothing
	}

	@FXML
	private void addSecondary(ActionEvent e) {
		if (content.getChildren().size() < 5) {
			BleedMonitor monitor = new BleedMonitor();
			monitor.setParentPausable(this);
			HBox.setHgrow(monitor, Priority.NEVER);
			monitor.setPrimaryChannel(primaryCombo.getValue());
			content.getChildren().add(monitor);
		}
		e.consume();
	}

	public Channel getPrimary() {
		return primaryCombo.getValue();
	}

	public void minimizeAll() {
		for (Node n : content.getChildren()) {
			if (n instanceof BleedMonitor) {
				((BleedMonitor) n).maximize(false);
			}
		}
	}
}
