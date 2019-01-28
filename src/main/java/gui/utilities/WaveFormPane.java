package gui.utilities;

import java.util.Arrays;
import java.util.Objects;

import control.ASIOController;
import control.InputListener;
import data.Channel;
import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import main.Main;

public class WaveFormPane extends ResizableCanvas implements PausableComponent, InputListener {

	private Input		input;
	private boolean		pause		= true;
	private Pausable	parent;
	private double[]	waveData	= new double[1024];

	/**
	 * Constructor
	 * 
	 * @param width
	 * @param height
	 */
	public WaveFormPane() {
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				if (!isPaused()) {
					paintWaveForm();
				}
			}
		};
		timer.start();
	}

	/**
	 * Paint the WaveForm
	 */
	public void paintWaveForm() {
		// Draw a Background Rectangle
		// Draw the waveform
		getGraphicsContext2D().setFill(Color.web(Main.getBaseColor()).deriveColor(50, 0, -50, 0));
		getGraphicsContext2D().setStroke(Color.web(Main.getAccentColor()));
		if (waveData != null) {
			getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
			double[] dataCopy = Arrays.copyOf(waveData, waveData.length);
			for (int i = 0; i < dataCopy.length; i++) {
				double value = (dataCopy[i] * getHeight() / 1.5);
				if (Channel.percentToDB(dataCopy[i]) >= Constants.RED) {
					getGraphicsContext2D().setStroke(Color.RED);
				} else if (Channel.percentToDB(dataCopy[i]) >= Constants.YELLOW) {
					getGraphicsContext2D().setStroke(Color.YELLOW);
				} else {
					getGraphicsContext2D().setStroke(Color.web(Main.getAccentColor()));
				}
				double y1 = ((getHeight() - 2.0 * value) / 2.0);
				double y2 = (y1 + 2.0 * value);
				getGraphicsContext2D().strokeLine(((double) i) / (waveData.length - 1) * getWidth(), y1, ((double) i) / (waveData.length - 1) * getWidth(), y2);
			}
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause || (parent != null && parent.isPaused()) || input == null || ASIOController.getInstance() == null;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parent = parent;
	}

	public void setChannel(Input channel) {
		if (this.input != null) {
			this.input.removeListener(this);
		}
		getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
		this.input = channel;
		if (this.input != null) {
			this.input.addListener(this);
		}
	}

	@Override
	public void levelChanged(Input input, double level, long time) {
		if (waveData.length != Math.floor(getWidth()) / 2) {
			waveData = Arrays.copyOf(waveData, (int) Math.floor(getWidth()) / 2);
		}
		if (Objects.equals(input, this.input) && ASIOController.getInstance() != null && waveData != null && waveData.length > 10) {
			for (int i = 0; i < waveData.length - 1; i++) {
				waveData[i] = waveData[i + 1];
			}
			waveData[waveData.length - 1] = level;
		}
	}
}
