package gui.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.FFTListener;
import gui.utilities.ResizableCanvas;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class SpectrumTimeController implements Initializable, Pausable, FFTListener {

	private static final Logger	LOG		= Logger.getLogger(SpectrumTimeController.class);
	@FXML
	private ScrollPane			canvasParent;

	private boolean				pause	= true;

	private ResizableCanvas		canvas;

	@FXML
	private ToggleButton		tglPlay;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		canvas = new ResizableCanvas(canvasParent);
		canvas.setParentPausable(this);
		canvasParent.setContent(canvas);

//		if (ASIOController.getInstance() != null) {
//			ASIOController.getInstance().addFFTListener(this);
//		}
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
		canvas.pause(pause);
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		LOG.error("Uninplemented method called: addParentPausable");
	}

	@FXML
	private void export(ActionEvent e) {
		//pausing 
		if (tglPlay.isSelected()) {
			tglPlay.fire();
		}
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save to");
		chooser.setInitialDirectory(new File("."));
		chooser.getExtensionFilters().add(new ExtensionFilter("PNG", "*.png"));
		chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().get(0));
		File file = chooser.showSaveDialog(canvasParent.getScene().getWindow());
		if (file != null) {
			MainController.getInstance().setStatus("Saving");
			canvas.save(file);
			MainController.getInstance().resetStatus();
		}
	}

	@FXML
	private void play(ActionEvent e) {
		canvas.pause(!tglPlay.isSelected());
		if (tglPlay.isSelected()) {
			tglPlay.setText("Pause");
		} else {
			tglPlay.setText("Play");
		}
	}

}