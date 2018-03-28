package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.NearestToneInfo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class TunerController implements Initializable {

	private static final Logger	LOG				= Logger.getLogger(TunerController.class);
	private static final double	REFRESH_RATE	= 50;
	private static final double	MULTIPLIER		= 2.0;
	@FXML
	private VBox				root;
	@FXML
	private Label				lblTone, lblFreq;
	@FXML
	private Pane				paneTuneL, paneTuneR;
	private Timeline			line;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.info("Loading Tuner");
		paneTuneL.setMaxWidth(0.0);
		paneTuneR.setMaxWidth(0.0);
		line = new Timeline();
		line.getKeyFrames().add(new KeyFrame(Duration.millis(REFRESH_RATE), event -> {
			if (ASIOController.getInstance() != null) {
				double freq = ASIOController.getInstance().getBaseFrequency();
				lblFreq.setText(Math.round(freq * 10.0) / 10.0 + " Hz");
				NearestToneInfo info = NearestToneInfo.findNearestTone(freq);
				double dev = info.getDeviationInCents();
				if (!info.isDeviationPositive()) {
					dev = dev * -1.0;
				}
				if (dev < 0) {
					paneTuneL.setMaxWidth(Math.abs(dev) * MULTIPLIER);
					paneTuneR.setMaxWidth(0.0);
				} else {
					paneTuneR.setMaxWidth(Math.abs(dev) * MULTIPLIER);
					paneTuneL.setMaxWidth(0.0);
				}
				lblTone.setText(info.getName());
			}
		}));
		line.setCycleCount(Timeline.INDEFINITE);
		line.playFromStart();
	}

	public void show(boolean newValue) {
		root.setVisible(newValue);
		root.setManaged(newValue);
	}
}
