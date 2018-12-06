package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.DrumTrigger;
import gui.controller.DrumViewController;
import gui.utilities.DrumTriggerListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

public class DrumTriggerItemController implements Initializable, DrumTriggerListener {

	private static final Logger	LOG	= Logger.getLogger(DrumTrigger.class);
	@FXML
	private Label				label;
	@FXML
	private ComboBox<Channel>	combo;
	@FXML
	private Slider				slider;
	@FXML
	private AnchorPane			waveFormPane;
	@FXML
	private Pane				threshold;
	private WaveFormChart		chart;
	private DrumViewController	controller;
	private DrumTrigger			trigger;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		combo.setOnShowing(e -> {
			if (ASIOController.getInstance() != null) {
				combo.getItems().setAll(ASIOController.getInstance().getInputList());
			}
		});
		combo.getSelectionModel().selectedItemProperty()
		        .addListener((ChangeListener<Channel>) (observable, oldValue, newValue) -> {
			        if (trigger != null) {
				        trigger.setChannel(newValue);
				        chart.setChannel(newValue);
			        }
		        });
		combo.setConverter(new StringConverter<Channel>() {

			@Override
			public Channel fromString(final String string) {
				return null;
			}

			@Override
			public String toString(final Channel object) {
				if (object == null) {
					LOG.info("");
					return "- NONE -";
				}
				return object.getName();
			}
		});

		chart = new WaveFormChart();
		waveFormPane.getChildren().add(chart);
		AnchorPane.setBottomAnchor(chart, .0);
		AnchorPane.setTopAnchor(chart, .0);
		AnchorPane.setLeftAnchor(chart, .0);
		AnchorPane.setRightAnchor(chart, .0);

		threshold.prefHeightProperty()
		        .bind(slider.valueProperty().divide(slider.maxProperty()).multiply(waveFormPane.heightProperty()));

	}

	private void refresh() {
		if (trigger == null) {
			label.setText("");
			combo.getSelectionModel().select(null);
			slider.setValue(0.0);
		} else {
			label.setText(trigger.getName());
			if (trigger.getChannel() != null) {
				combo.getSelectionModel().select(trigger.getChannel());
			}
			slider.setValue(trigger.getTreshold());
		}
	}

	public void setDrumController(final DrumViewController con) {
		controller = con;
	}

	public void setTrigger(final DrumTrigger trigger) {
		if (this.trigger != null) {
			this.trigger.setObs(null);
		}
		this.trigger = trigger;
		if (this.trigger != null) {
			this.trigger.setObs(this);
		}
		refresh();
	}

	@Override
	public void tresholdReached(final double level, final double treshold) {
		controller.addEntry(trigger, level);
	}
}
