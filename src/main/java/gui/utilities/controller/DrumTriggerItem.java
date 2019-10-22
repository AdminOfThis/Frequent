package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.ASIOController;
import data.Channel;
import data.DrumTrigger;
import gui.FXMLUtil;
import gui.utilities.AutoCompleteComboBoxListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import main.Constants;

public class DrumTriggerItem extends AnchorPane implements Initializable {

	// private static final Logger LOG =
	// Logger.getLogger(DrumTriggerItem.class);
	private static final String DRUM_ITEM_PATH = "/fxml/utilities/DrumTriggerItem.fxml";
	@FXML
	private VBox root;
	@FXML
	private Label label;
	@FXML
	private ComboBox<Channel> combo;
	@FXML
	private Slider slider;
	@FXML
	private AnchorPane waveFormPane;
	private WaveFormChart chart;
	private DrumTrigger trigger;

	public DrumTriggerItem(final DrumTrigger trigger) {
		this.trigger = trigger;
		Parent p = FXMLUtil.loadFXML(getClass().getResource(DRUM_ITEM_PATH), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, .0);
		AnchorPane.setBottomAnchor(p, .0);
		AnchorPane.setLeftAnchor(p, .0);
		AnchorPane.setRightAnchor(p, .0);
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		setStyle("-fx-border-color: blue");
		chart = new SymmetricWaveFormChart();
//		chart.maxHeightProperty().bind(chart.prefHeightProperty().divide(2));
		// Should be own subclass of WaveformChart
		chart.showTreshold(true);
		waveFormPane.getChildren().add(chart);
		AnchorPane.setTopAnchor(chart, .0);
//		AnchorPane.setBottomAnchor(chart, 000.0);
		waveFormPane.heightProperty().addListener((e, oldV, newV) -> AnchorPane.setBottomAnchor(chart, waveFormPane.getHeight() / 2.0));

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
		combo.setConverter(Constants.CHANNEL_CONVERTER);
		slider.setMin(Constants.FFT_MIN);
		slider.valueProperty().addListener(e -> {
			trigger.setTreshold(slider.getValue());
			chart.setThreshold(Math.abs(slider.getValue()));
		});

		waveFormPane.minHeightProperty().bind(chart.heightProperty().multiply(2));

		slider.prefHeightProperty().bind(chart.getYAxis().heightProperty());
		slider.minHeightProperty().bind(chart.getYAxis().heightProperty());
		slider.maxHeightProperty().bind(chart.getYAxis().heightProperty());
		new AutoCompleteComboBoxListener<>(combo);
		if (trigger != null) {
			label.setText(trigger.getName());
		}
	}
}
