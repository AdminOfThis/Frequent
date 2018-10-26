package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import control.InputListener;
import data.Channel;
import data.Group;
import data.Input;
import gui.controller.RTAViewController;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.ChannelCellContextMenu;
import gui.utilities.FXMLUtil;
import gui.utilities.GroupCellContextMenu;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class VuMeter extends AnchorPane implements Initializable, InputListener, PausableComponent {

	private static final String		FXML_VERTICAL	= "/fxml/utilities/VuMeterVertical.fxml";
	private static final String		FXML_HORIZONTAL	= "/fxml/utilities/VuMeterHorizontal.fxml";
	private static final int		PEAK_HOLD		= 50;
	@FXML
	private StackPane				vuPane;
	@FXML
	private Pane					vuPeakPane, vuLastPeakPane;
	@FXML
	private Label					lblPeak, lblTitle;
	private Input					channel;
	private double					peak			= RTAViewController.FFT_MIN;
	private Orientation				orientation;
	private boolean					pause			= false;
	private Pausable				parentPausable;
	private SimpleBooleanProperty	showLabels		= new SimpleBooleanProperty(true);
	private int						timeSincePeak	= 0;
	private List<Double>			pendingLevelList;

	public VuMeter(final Input channel, final Orientation o) {
		orientation = o;
		String path;
		if (o.equals(Orientation.HORIZONTAL)) {
			path = FXML_HORIZONTAL;
		} else {
			path = FXML_VERTICAL;
		}
		Parent p = FXMLUtil.loadFXML(path, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		pendingLevelList = Collections.synchronizedList(new ArrayList<Double>());
		setChannel(channel);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				update();
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
	public void levelChanged(final double level) {
		pendingLevelList.add(Channel.percentToDB(level));
	}

	@Override
	public void pause(final boolean pause) {
		this.pause = pause;
	}

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
				setOnContextMenuRequested(e -> {
					ContextMenu menu = null;
					if (c instanceof Channel) {
						menu = new ChannelCellContextMenu((Channel) c);
					} else if (c instanceof Group) {
						menu = new GroupCellContextMenu((Group) c);
					}
					menu.show(vuPane, e.getScreenX(), e.getScreenY());
				});
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

	public void setTitle(final String title) {
		if (lblTitle != null) {
			lblTitle.setText(title);
		}
	}

	public void showLabels(final boolean b) {
		showLabels.setValue(b);
	}

	private void update() {
		if (!isPaused()) {
			synchronized (pendingLevelList) {
				for (Double peakdB : pendingLevelList) {
					if (peak < peakdB || timeSincePeak >= PEAK_HOLD) {
						peak = peakdB;
						timeSincePeak = 0;
					}
					if (timeSincePeak < PEAK_HOLD) {
						timeSincePeak++;
					}
					if (orientation == Orientation.VERTICAL) {
						vuPeakPane.setPrefHeight(
							vuPane.getHeight() * (peakdB + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
						vuLastPeakPane.setPrefHeight(
							vuPane.getHeight() * (peak + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
					} else {
						vuPeakPane.setPrefWidth(
							vuPane.getWidth() * (peakdB + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
						vuLastPeakPane.setPrefWidth(
							vuPane.getWidth() * (peak + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
					}
					if (peakdB >= RTAViewController.FFT_MIN) {
						lblPeak.setText(Math.round(peakdB * 10.0) / 10 + "");
					} else {
						lblPeak.setText("-\u221E");
					}
					if (peakdB > -5.0) {
						if (peakdB >= -1.0) {
							vuPeakPane.setStyle("-fx-background-color: red");
						} else {
							vuPeakPane.setStyle("-fx-background-color: yellow");
						}
						Timeline line = new Timeline();
						KeyFrame frame = new KeyFrame(Duration.seconds(5.0), e -> {
							vuPeakPane.setStyle("");
						});
						line.getKeyFrames().add(frame);
						line.playFromStart();
					}
				}
			}
		} else {
			if (orientation == Orientation.VERTICAL) {
				vuPeakPane.setPrefHeight(0);
				vuLastPeakPane.setPrefHeight(0);
			} else {
				vuPeakPane.setPrefWidth(0);
				vuLastPeakPane.setPrefWidth(0);
			}
		}
		pendingLevelList.clear();
	}
}
