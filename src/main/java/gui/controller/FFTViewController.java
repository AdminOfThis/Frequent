package gui.controller;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;

/**
 * 
 * @author AdminOfThis
 *
 */
public class FFTViewController implements Initializable, FFTListener, PausableView {

	@FXML
	private ScrollPane canvasParent;
	private boolean pause = true;
	private RTACanvas canvas;
	@FXML
	private ToggleButton tglPlay;
	private List<float[]> pendingMap = Collections.synchronizedList(new ArrayList<>());

	@Override
	public ArrayList<Region> getHeader() {
		ArrayList<Region> result = new ArrayList<>();
		result.add(tglPlay);
		return result;
	}

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

	@Override
	public boolean isPaused() {
		return pause;
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

	private void update() {

		synchronized (pendingMap) {
			for (float[] map : pendingMap) {
				canvas.addLine(map);
			}
			pendingMap.clear();
		}
	}
}