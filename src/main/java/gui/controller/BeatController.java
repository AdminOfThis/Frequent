package gui.controller;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import data.Channel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

public class BeatController implements Initializable {

	@FXML
	private VBox				boxValue;

	private double				bpm;

	private Map<Channel, Long>	map	= Collections.synchronizedMap(new HashMap<>());

	public void addValue(final Channel c, final long value) {
		synchronized (map) {

			map.put(c, value);

		}
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {

	}

	public void setBPM(final double bpm) {
		this.bpm = bpm;
	}

}
