package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.LevelListener;
import data.Channel;
import data.Group;
import data.Input;
import gui.controller.FFTController;
import gui.controller.Pausable;
import gui.utilities.ChannelCellContextMenu;
import gui.utilities.FXMLUtil;
import gui.utilities.GroupCellContextMenu;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class VuMeter extends AnchorPane implements Initializable, LevelListener, Pausable {

	private static final String	FXML_VERTICAL	= "/gui/utilities/gui/VuMeterVertical.fxml";
	private static final String	FXML_HORIZONTAL	= "/gui/utilities/gui/VuMeterHorizontal.fxml";
	private static final double	DB_PEAK_FALLOFF	= 0.015;
	@FXML
	private StackPane			vuPane;
	@FXML
	private Pane				vuPeakPane, vuLastPeakPane;
	@FXML
	private Label				lblPeak, lblTitle;
	private Input				channel;
	private double				peak			= FFTController.FFT_MIN;
	private Orientation			orientation;
	private boolean				pause			= false;
	private Pausable			parentPausable;

	public VuMeter(Input channel, Orientation o) {
		this.orientation = o;
		setChannel(channel);
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
	}

	public void setTitle(String title) {
		lblTitle.setText(title);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblTitle.prefWidthProperty().addListener(e -> {
			if (lblTitle.prefHeightProperty().get() > this.prefWidthProperty().get()) {
				lblTitle.setRotate(90.0);
			} else {
				lblTitle.setRotate(0.0);
			}
		});
		lblPeak.setText("");
	}

	@Override
	public void levelChanged(double level) {
		if (!isPaused()) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					if (channel != null) {
						double peakdB = Channel.percentToDB(channel.getLevel() * 1000.0);
						if (peak < peakdB) {
							peak = peakdB;
						}
						if (orientation == Orientation.VERTICAL) {
							vuPeakPane.setPrefHeight(
								vuPane.getHeight() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
							vuLastPeakPane.setPrefHeight(
								vuPane.getHeight() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
						} else {
							vuPeakPane.setPrefWidth(
								vuPane.getWidth() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
							vuLastPeakPane.setPrefWidth(
								vuPane.getWidth() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
						}
						if (peakdB >= FFTController.FFT_MIN) {
							lblPeak.setText(Math.round(peakdB * 10.0) / 10 + "");
						} else {
							lblPeak.setText("-\u221E");
						}
						if (peakdB >= -0.5) {
							vuPane.setStyle("-fx-background-color: red");
						} else if (peakdB >= -2.0) {
							vuPane.setStyle("-fx-background-color: yellow");
						} else {
							vuPane.setStyle("");
						}
						if (peak == 0.0) {
							peak = -1.0;
						}
						peak = (1 + DB_PEAK_FALLOFF) * peak;
					} else {
						if (orientation == Orientation.VERTICAL) {
							vuPeakPane.setPrefHeight(0);
							vuLastPeakPane.setPrefHeight(0);
						} else {
							vuPeakPane.setPrefWidth(0);
							vuLastPeakPane.setPrefWidth(0);
						}
					}
				}
			});
		}
	}

	public void setChannel(Input c) {
		if (this.channel != null) {
			this.channel.removeObserver(this);
		}
		this.channel = c;
		if (c != null) {
			c.addObserver(this);
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
}
