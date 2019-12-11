package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import control.InputListener;
import data.Channel;
import data.Input;
import gui.FXMLUtil;
import gui.pausable.Pausable;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import main.Constants;

/**
 * 
 * @author AdminOfThis
 *
 */
public class VuMeterMono extends VuMeter implements Initializable, InputListener {

	private static final String FXML_VERTICAL = "/fxml/utilities/VuMeterVertical.fxml";
	private static final String FXML_HORIZONTAL = "/fxml/utilities/VuMeterHorizontal.fxml";
	private static final int PEAK_HOLD = 50;
	@FXML
	private StackPane vuPane;
	@FXML
	private Pane vuPeakPane, vuPeakMeterPane, vuRMSPane, vuLastPeakPane;
	@FXML
	private Label lblPeak, lblTitle;
	private Input channel;
	private double peak = Constants.FFT_MIN;
	private Orientation orientation;
	private boolean pause = false;
	private Pausable parentPausable;
	private SimpleBooleanProperty showLabels = new SimpleBooleanProperty(true);
	private int timeSincePeak = 0;
	private List<double[]> pendingLevelList = Collections.synchronizedList(new ArrayList<double[]>());

	public VuMeterMono(final Input channel, final Orientation o) {
		orientation = o;
		String path;
		if (o.equals(Orientation.HORIZONTAL)) {
			path = FXML_HORIZONTAL;
		} else {
			path = FXML_VERTICAL;
		}
		Parent p = FXMLUtil.loadFXML(getClass().getResource(path), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		setChannel(channel);
		AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(final long now) {
				new Thread(() -> update()).start();
			}
		};
		timer.start();

	}

	public Input getInput() {
		return channel;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		lblPeak.visibleProperty().bind(showLabels);
		lblTitle.visibleProperty().bind(showLabels);
		lblTitle.rotateProperty().bind(rotateProperty().multiply(-1));
		lblPeak.rotateProperty().bind(rotateProperty().multiply(-1));
		lblTitle.prefWidthProperty().addListener(e -> {
			if (lblTitle.prefHeightProperty().get() > prefWidthProperty().get()) {
				lblTitle.setRotate(90.0);
			} else {
				lblTitle.setRotate(0.0);
			}
		});
		lblPeak.setText("");
		if (channel != null) {
			lblTitle.setText(channel.getName());
		} else {
			lblTitle.setText("");
		}
	}

	@Override
	public boolean isPaused() {
		return pause || parentPausable != null && parentPausable.isPaused() || channel == null || pendingLevelList == null;
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		pendingLevelList.add(new double[] { Channel.percentToDB(level), Channel.percentToDB(input.getRMSLevel()) });
	}

	@Override
	public void pause(final boolean pause) {
		this.pause = pause;
	}

	@Override
	public void setChannel(final Input c) {
		if (!Objects.equals(c, channel)) {
			if (channel != null) {
				channel.removeListener(this);
			}
			channel = c;
			if (c != null) {
				setTitle(c.getName());
				c.addListener(this);
				if (c.getColor() != null && !c.getColor().isEmpty()) {
					setStyle("-fx-accent: " + c.getColor());
				} else {
					setStyle("");
				}
			} else {
				setTitle("");
				setOnContextMenuRequested(null);
				if (lblPeak != null) {
					lblPeak.setText("");
				}
				if (lblTitle != null) {
					lblTitle.setText("");
				}
			}
		}
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		parentPausable = parent;
	}

	@Override
	public void setTitle(final String title) {
		if (lblTitle != null) {
			Platform.runLater(() -> lblTitle.setText(title));
		}
	}

	public void showLabels(final boolean b) {
		showLabels.setValue(b);
	}

	private void update() {
		if (!isPaused()) {
			synchronized (pendingLevelList) {
				for (double[] peakArray : pendingLevelList) {
					double peakdB = peakArray[0];
					double rmsdB = peakArray[1];
					if (peak < peakdB || timeSincePeak >= PEAK_HOLD) {
						peak = peakdB;
						timeSincePeak = 0;
					}
					if (timeSincePeak < PEAK_HOLD) {
						timeSincePeak++;
					}
					Platform.runLater(() -> {
						if (orientation == Orientation.VERTICAL) {
							vuPeakPane.setPrefHeight(vuPane.getHeight() * (peakdB + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));
							vuRMSPane.setPrefHeight(vuPane.getHeight() * (rmsdB + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));
							vuLastPeakPane.setPrefHeight(vuPane.getHeight() * (peak + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));

						} else {

							vuPeakPane.setPrefWidth(vuPane.getWidth() * (peakdB + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));
							vuRMSPane.setPrefWidth(vuPane.getWidth() * (rmsdB + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));
							vuLastPeakPane.setPrefWidth(vuPane.getWidth() * (peak + Math.abs(Constants.FFT_MIN)) / Math.abs(Constants.FFT_MIN));

						}
						if (peakdB >= Constants.FFT_MIN) {
							lblPeak.setText(Math.round(peakdB * 10.0) / 10 + "");
						} else {
							lblPeak.setText("-∞");
						}
					});
					if (peakdB >= Constants.YELLOW) {
						if (peakdB >= Constants.RED) {
							vuPeakMeterPane.setStyle("-fx-background-color: red");
						} else {
							vuPeakMeterPane.setStyle("-fx-background-color: yellow");
						}
						Timeline line = new Timeline();
						double duration;
						if (peakdB >= -2.0) {
							duration = 10;
						} else {
							duration = 3;
						}
						KeyFrame frame = new KeyFrame(Duration.seconds(duration), e -> {
							vuPeakPane.setStyle("");
						});
						line.getKeyFrames().add(frame);
						line.playFromStart();
					}
				}
			}
		} else {
			setTitle("");
			Platform.runLater(() -> lblPeak.setText(""));
			if (orientation == Orientation.VERTICAL) {
				vuPeakPane.setPrefHeight(0);
				vuRMSPane.setPrefHeight(0);
				vuLastPeakPane.setPrefHeight(0);
			} else {
				vuPeakPane.setPrefWidth(0);
				vuRMSPane.setPrefWidth(0);
				vuLastPeakPane.setPrefWidth(0);
			}
		}
		pendingLevelList.clear();
	}

	@Override
	public void nameChanged(String name) {
		Platform.runLater(() -> lblTitle.setText(name));
	}

	@Override
	public void colorChanged(String newColor) {}
}
