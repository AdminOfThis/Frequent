package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import data.Channel;
import data.LevelObserver;
import gui.controller.FFTController;
import gui.utilities.FXMLUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class VuMeter extends AnchorPane implements Initializable, LevelObserver {


	private static final String	FXML_VERTICAL	= "/gui/utilities/gui/VuMeterVertical.fxml";
	private static final String	FXML_HORIZONTAL	= "/gui/utilities/gui/VuMeterHorizontal.fxml";
	private static final int	REFRESH_RATE	= 25;


	@FXML
	private StackPane			vuPane;
	@FXML
	private Pane				vuPeakPane, vuLastPeakPane;
	@FXML
	private Label				lblPeak;
	@FXML
	private AnchorPane			clippingPane;

	private Channel				channel;
	private Timeline			line;
	private double				peak			= 0;
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
	public void initialize(URL location, ResourceBundle resources) {
		initTimeline();


	}

	@Override
	public void levelChanged(double level) {

	}


	private void initTimeline() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (channel != null) {
				double peakdB = Channel.percentToDB(channel.getLevel() * 1000.0);

				if (peak < peakdB) {
					peak = peakdB;
				}
				vuPeakPane.setStyle("-fx-background-color: linear-gradient( to bottom, -fx-accent, derive(-fx-accent, -50%) );");
				if (orientation == Orientation.VERTICAL) {
					vuPeakPane
						.setPrefHeight(vuPane.getHeight() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
					vuLastPeakPane
						.setPrefHeight(vuPane.getHeight() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
				} else {
					vuPeakPane
						.setPrefWidth(vuPane.getWidth() * (peakdB + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));
					vuLastPeakPane
						.setPrefWidth(vuPane.getWidth() * (peak + Math.abs(FFTController.FFT_MIN)) / Math.abs(FFTController.FFT_MIN));

				}

				lblPeak.setText(Math.round(peakdB * 10.0) / 10.0 + "");
				if (peakdB >= 0.99) {
					clippingPane.setStyle("-fx-background-color: red");
				} else {
					clippingPane.setStyle("");
				}
			}
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Timeline.INDEFINITE);
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}


}
