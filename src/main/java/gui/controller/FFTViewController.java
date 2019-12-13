package gui.controller;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import control.FFTListener;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.RTACanvas;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class FFTViewController implements Initializable, FFTListener, PausableView {

	@FXML
	private ScrollPane canvasParent;
	private boolean pause = true;
	private RTACanvas canvas;
	@FXML
	private ToggleButton tglPlay;
	@FXML
	private Button btnExport;
	private List<float[]> pendingMap = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		canvas = new RTACanvas(canvasParent);
		canvas.setParentPausable(this);
		canvasParent.setContent(canvas);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				new Thread(() -> update()).start();
			}
		};
		timer.start();
	}

	private void update() {

		synchronized (pendingMap) {
			for (float[] map : pendingMap) {
				canvas.addLine(map);
			}
			pendingMap.clear();
		}
	}

	@Override
	public void newFFT(float[] map) {
		if (!isPaused()) {
			synchronized (pendingMap) {
				pendingMap.add(map);
			}
			canvasParent.setVvalue(1.0);
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@FXML
	private void export(ActionEvent e) {
		// pausing
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
		e.consume();
	}

	@FXML
	private void play(ActionEvent e) {
		canvas.pause(!tglPlay.isSelected());
		if (tglPlay.isSelected()) {
			tglPlay.setText("Pause");
		} else {
			tglPlay.setText("Play");
		}
		e.consume();
	}

	@Override
	public ArrayList<Region> getHeader() {
		ArrayList<Region> result = new ArrayList<>();
		result.add(tglPlay);
		result.add(btnExport);
		return result;
	}

	@Override
	public void refresh() {
		// nothing to do
	}

	@Override
	public void setSelectedChannel(Input in) {
		canvas.reset();
		synchronized (pendingMap) {
			pendingMap.clear();
		}
	}
}