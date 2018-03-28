package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
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
				lblFreq.setText(ASIOController.getInstance().getBaseFrequency() + " Hz");
				lblFreq.setText("What do i know");
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
