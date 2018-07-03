package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.FFTListener;
import gui.utilities.ResizableCanvas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class SpectrumTimeController implements Initializable, Pausable, FFTListener {

	private static final Logger	LOG		= Logger.getLogger(SpectrumTimeController.class);
	@FXML
	private ScrollPane			canvasParent;

	private boolean				pause	= true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ResizableCanvas can = new ResizableCanvas(canvasParent);
		canvasParent.setContent(can);
		can.widthProperty().bind(canvasParent.widthProperty());

		if (ASIOController.getInstance() != null) {
			ASIOController.getInstance().addFFTListener(this);
		}
	}

	@Override
	public void newFFT(double[][] map) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				HBox box = new HBox();
				box.setPrefHeight(10.0);
				if (!pause) {
					ArrayList<Pane> paneList = new ArrayList<>();
					for (double[] entry : map) {
						double level = entry[1];
						Pane pane = new Pane(new Label(Math.round(level) + ""));
						HBox.setHgrow(pane, Priority.ALWAYS);
						paneList.add(pane);
					}
					box.getChildren().addAll(paneList);
				}
				// canvasParent.getChildren().add(box);

			}

		});

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