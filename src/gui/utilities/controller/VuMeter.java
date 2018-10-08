package gui.utilities.controller;

import java.net.URL;
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
import javafx.application.Platform;
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

public class VuMeter extends AnchorPane implements Initializable, InputListener, PausableComponent {

	private static final String		FXML_VERTICAL	= "/gui/utilities/gui/VuMeterVertical.fxml";
	private static final String		FXML_HORIZONTAL	= "/gui/utilities/gui/VuMeterHorizontal.fxml";
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

	public VuMeter(Input channel, Orientation o) {
		this.orientation = o;
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
		setChannel(channel);
	}

	public void setTitle(String title) {
		if (lblTitle != null) {
			lblTitle.setText(title);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblPeak.visibleProperty().bind(showLabels);
		lblTitle.visibleProperty().bind(showLabels);
		lblTitle.prefWidthProperty().addListener(e -> {
			if (lblTitle.prefHeightProperty().get() > this.prefWidthProperty().get()) {
				lblTitle.setRotate(90.0);
			} else {
				lblTitle.setRotate(0.0);
			}
		});
		lblPeak.setText("");
		lblTitle.setText("");
	}

	@Override
	public void levelChanged(double level, Input in) {
		if (!isPaused()) {
			Platform.runLater(() -> {
				if (channel != null) {
					double peakdB = Channel.percentToDB(channel.getLevel());
					if (peak < peakdB || timeSincePeak >= PEAK_HOLD) {
						peak = peakdB;
						timeSincePeak = 0;
					}
					if (timeSincePeak < PEAK_HOLD) {
						timeSincePeak++;
					}
					if (orientation == Orientation.VERTICAL) {
						vuPeakPane.setPrefHeight(vuPane.getHeight() * (peakdB + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
						vuLastPeakPane.setPrefHeight(vuPane.getHeight() * (peak + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
					} else {
						vuPeakPane.setPrefWidth(vuPane.getWidth() * (peakdB + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
						vuLastPeakPane.setPrefWidth(vuPane.getWidth() * (peak + Math.abs(RTAViewController.FFT_MIN)) / Math.abs(RTAViewController.FFT_MIN));
					}
					if (peakdB >= RTAViewController.FFT_MIN) {
						lblPeak.setText(Math.round(peakdB * 10.0) / 10 + "");
					} else {
						lblPeak.setText("-\u221E");
					}
					if (peakdB >= -0.5) {
						vuPane.setStyle("-fx-background-color: red");
					} else if (peakdB >= -5.0) {
						vuPane.setStyle("-fx-background-color: yellow");
					} else {
						vuPane.setStyle("");
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
			});
		}
	}

	public void setChannel(Input c) {
		if (this.channel != null) {
			this.channel.removeListener(this);
		}
		this.channel = c;
		if (c != null) {
			setTitle(c.getName());
			c.addListener(this);
			if (c.getColor() != null && !c.getColor().isEmpty()) {
				this.setStyle("-fx-accent: " + c.getColor());
			} else {
				this.setStyle("");
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

	public Input getInput() {
		return channel;
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause || (parentPausable != null && parentPausable.isPaused());
	}

	@Override
	public void setParentPausable(Pausable parent) {
		parentPausable = parent;
	}

	public void showLabels(boolean b) {
		showLabels.setValue(b);
	}
}
