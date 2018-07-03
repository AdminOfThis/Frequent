package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.FFTListener;
import gui.utilities.ResizableCanvas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;

public class SpectrumTimeController implements Initializable, Pausable, FFTListener {

	private static final Logger	LOG		= Logger.getLogger(SpectrumTimeController.class);
	@FXML
	private ScrollPane			canvasParent;

	private boolean				pause	= true;

	private ResizableCanvas		canvas;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		canvas = new ResizableCanvas(canvasParent);
		canvasParent.setContent(canvas);

		if (ASIOController.getInstance() != null) {
			ASIOController.getInstance().addFFTListener(this);
		}
	}

	@Override
	public void newFFT(double[][] map) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				canvas.addLine(map);
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