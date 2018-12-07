package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.ASIOController;
import data.Channel;
import data.DrumTrigger;
import gui.controller.DrumViewController;
import gui.controller.RTAViewController;
import gui.utilities.AutoCompleteComboBoxListener;
import gui.utilities.DrumTriggerListener;
import gui.utilities.FXMLUtil;
import gui.utilities.controller.WaveFormChart.Style;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

public class DrumTriggerItem extends AnchorPane implements Initializable, DrumTriggerListener {

	// private static final Logger LOG =
	// Logger.getLogger(DrumTriggerItem.class);
	private static final String	DRUM_ITEM_PATH	= "/fxml/utilities/DrumTriggerItem.fxml";

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


	public DrumTriggerItem(DrumTrigger trigger) {

		this.trigger = trigger;
		Parent p = FXMLUtil.loadFXML(DRUM_ITEM_PATH, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, .0);
		AnchorPane.setBottomAnchor(p, .0);
		AnchorPane.setLeftAnchor(p, .0);
		AnchorPane.setRightAnchor(p, .0);
	}


	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		chart = new WaveFormChart(Style.AREA);
		waveFormPane.getChildren().add(chart);

		AnchorPane.setTopAnchor(chart, .0);
		AnchorPane.setBottomAnchor(chart, .0);
		AnchorPane.setLeftAnchor(chart, .0);
		AnchorPane.setRightAnchor(chart, .0);
		combo.setOnShowing(e -> {
			if (ASIOController.getInstance() != null) {
				combo.getItems().setAll(ASIOController.getInstance().getInputList());
			}
		});
		combo.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Channel>) (observable, oldValue, newValue) -> {
			if (trigger != null) {
				trigger.setChannel(newValue);
			}
			if (chart != null) {
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
					return "- NONE -";
				}
				return object.getName();
			}
		});
		slider.setMin(RTAViewController.FFT_MIN);
		slider.valueProperty().addListener(e -> {
			double percent = (1.0 - slider.getValue() / slider.getMin());
			double percentValue = percent * chart.getYAxis().getHeight();
			double value = Math.abs(slider.getBoundsInParent().getMaxY() - chart.getYAxis().getBoundsInParent().getMaxY());
			threshold.setPrefHeight(percentValue + value);
		});
		slider.prefHeightProperty().bind(chart.getYAxis().heightProperty());
		new AutoCompleteComboBoxListener<>(combo);
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
