package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import data.Channel;
import data.LevelObserver;
import gui.controller.FFTController;
import gui.utilities.FXMLUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class VuMeter extends AnchorPane implements Initializable, LevelObserver {

	private static final String	FXML_VERTICAL	= "/gui/utilities/gui/VuMeterVertical.fxml";
	private static final String	FXML_HORIZONTAL	= "/gui/utilities/gui/VuMeterHorizontal.fxml";
	private static final double	DB_PEAK_FALLOFF	= 0.02;
	@FXML
	private StackPane			vuPane;
	@FXML
	private Pane				vuPeakPane, vuLastPeakPane;
	@FXML
	private Label				lblPeak;
	@FXML
	private AnchorPane			clippingPane;
	private Channel				channel;
	private double				peak			= FFTController.FFT_MIN;
	private Orientation			orientation;

	public VuMeter(Channel channel, Orientation o) {
		this.orientation = o;
		this.channel = channel;
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

	@Override
	public void initialize(URL location, ResourceBundle resources) {}

	@Override
	public void levelChanged(double level) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				if (channel != null) {
					double peakdB = Channel.percentToDB(channel.getLevel() * 1000.0);
					if (peak < peakdB) {
						peak = peakdB;
					}
					if (orientation == Orientation.VERTICAL) {
						vuPeakPane.setPrefHeight(vuPane.getHeight() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
						vuLastPeakPane.setPrefHeight(vuPane.getHeight() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
					} else {
						vuPeakPane.setPrefWidth(vuPane.getWidth() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
						vuLastPeakPane.setPrefWidth(vuPane.getWidth() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
					}
					if (peakdB >= FFTController.FFT_MIN) {
						lblPeak.setText(Math.round(peakdB * 10.0) / 10 + "");
					} else {
						lblPeak.setText("-\u221E");
					}
					if (peakdB >= -0.5) {
						clippingPane.setStyle("-fx-background-color: red");
					} else if (peakdB >= -2.0) {
						clippingPane.setStyle("-fx-background-color: yellow");
					} else {
						clippingPane.setStyle("");
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

	public void setChannel(Channel c) {
		if (this.channel != null) {
			this.channel.removeObserver(this);
		}
		this.channel = c;
		if (c != null) {
			c.addObserver(this);
		}
	}

	public Channel getChannel() {
		return channel;
	}
}
