package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.DrumTrigger;
import gui.controller.DrumController;
import gui.utilities.DrumTriggerObserver;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;

public class DrumTriggerItemController implements Initializable, DrumTriggerObserver {

	private static final Logger	LOG		= Logger.getLogger(DrumTrigger.class);
	private static ToggleGroup	group	= new ToggleGroup();
	@FXML
	private Label				label;
	@FXML
	private ComboBox<Channel>	combo;
	@FXML
	private Slider				slider;
	@FXML
	private ToggleButton		view;
	private DrumController		controller;
	private DrumTrigger			trigger;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		group.getToggles().add(view);
		combo.setOnShowing(e -> {
			if (ASIOController.getInstance() != null) {
				combo.getItems().setAll(ASIOController.getInstance().getInputList());
			}
		});
		combo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Channel>() {

			@Override
			public void changed(ObservableValue<? extends Channel> observable, Channel oldValue, Channel newValue) {
				if (trigger != null) {
					trigger.setChannel(newValue);
				}
			}
		});
		combo.setConverter(new StringConverter<Channel>() {

			@Override
			public String toString(Channel object) {
				if (object == null) {
					LOG.info("");
					return "- NONE -";
				}
				return object.getName();
			}

			@Override
			public Channel fromString(String string) {
				return null;
			}
		});
		slider.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (trigger != null) {
					// LOG.info("Setting Treshold on Trigger " +
					// trigger.getName() + " to " + slider.getValue());
					trigger.setTreshold(slider.getValue());
					controller.redrawThreshold();
				}
			}
		});
		view.setOnAction(e -> {
			if (trigger != null) {
				controller.setActiveTrigger(trigger);
			}
		});
	}

	public void setDrumController(DrumController con) {
		this.controller = con;
	}

	public void setTrigger(DrumTrigger trigger) {
		if (this.trigger != null) {
			this.trigger.setObs(null);
		}
		this.trigger = trigger;
		if (this.trigger != null) {
			this.trigger.setObs(this);
		}
		refresh();
	}

	private void refresh() {
		if (trigger == null) {
			label.setText("");
			combo.getSelectionModel().select(null);
			slider.setValue(0.0);
			view.setSelected(false);
		} else {
			label.setText(trigger.getName());
			if (trigger.getChannel() != null) {
				combo.getSelectionModel().select(trigger.getChannel());
			}
			slider.setValue(trigger.getTreshold());
		}
	}

	@Override
	public void tresholdReached(double level, double treshold) {
		controller.addEntry(trigger, level);
	}
}
