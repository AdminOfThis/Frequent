package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.FFTListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SpectrumTimeController implements Initializable, Pausable, FFTListener {

	private static final Logger	LOG		= Logger.getLogger(SpectrumTimeController.class);
	@FXML
	private VBox				dataPane;

	private boolean				pause	= true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}

	@Override
	public void newFFT(double[][] map) {

		HBox box = new HBox();
		box.setPrefHeight(10.0);
		if (!pause) {
			ArrayList<Pane> paneList = new ArrayList<>();
			for (double[] entry : map) {
				double frequency = entry[0];
				double level = entry[1];
				Pane pane = new Pane(new Label(Math.round(level) + ""));
				HBox.setHgrow(pane, Priority.ALWAYS);
				paneList.add(pane);
			}
			box.getChildren().addAll(paneList);
		}
		dataPane.getChildren().add(box);
	}

	public void setChannel() {

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
	public void setParentPausable(Pausable parent) {
		LOG.error("Uninplemented method called: addParentPausable");
	}

}