package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.ASIOController;
import gui.FXMLUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import main.Main;

/**
 * 
 * @author AdminOfThis
 *
 */
public class SettingsController implements Initializable {

	private static final Number[] BUFFERS = new Number[] { 64, 128, 256, 512, 1024, 2048 };

	@FXML
	private BorderPane root;

	@FXML
	private Button btnCancel, btnSave;

	@FXML
	private ComboBox<Number> chbBuffer;

	@FXML
	private ComboBox<String> chbDevice;

	@FXML
	private ToggleGroup startUp, startUpPanel;

	@FXML
	private RadioButton rBtnPanelLast, rBtnPanelSpecific;

	@FXML
	private FlowPane flwPanel;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		checkFXML();
		root.setStyle(Main.getStyle());
		// FXMLUtil.setIcon((Stage) root.getScene().getWindow(), Main.getLogoPath());
		FXMLUtil.setStyleSheet(root);
		// controls
		flwPanel.disableProperty().bind(rBtnPanelSpecific.selectedProperty().not());
		// Init data
		chbBuffer.getItems().addAll(BUFFERS);
		chbDevice.getItems().addAll(ASIOController.getPossibleDrivers());
		if (ASIOController.getInstance() != null) {
			chbBuffer.setValue(ASIOController.getInstance().getBufferSize());
			chbDevice.setValue(ASIOController.getInstance().getDevice());
		}

		initSpecificPanel();
	}

	private void checkFXML() {
		assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btnCancel != null : "fx:id=\"btnCancel\" was not injected: check your FXML file 'Settings.fxml'.";
		assert btnSave != null : "fx:id=\"btnSave\" was not injected: check your FXML file 'Settings.fxml'.";
		assert chbBuffer != null : "fx:id=\"chbBuffer\" was not injected: check your FXML file 'Settings.fxml'.";
		assert chbDevice != null : "fx:id=\"chbDevice\" was not injected: check your FXML file 'Settings.fxml'.";
		assert startUp != null : "fx:id=\"startUp\" was not injected: check your FXML file 'Settings.fxml'.";
		assert rBtnPanelLast != null : "fx:id=\"rBtnLast\" was not injected: check your FXML file 'Settings.fxml'.";
		assert rBtnPanelSpecific != null : "fx:id=\"rBtnSpecific\" was not injected: check your FXML file 'Settings.fxml'.";
		assert flwPanel != null : "fx:id=\"flwPanel\" was not injected: check your FXML file 'Settings.fxml'.";
		assert startUpPanel != null : "fx:id=\"startUpPanel\" was not injected: check your FXML file 'Settings.fxml'.";

	}

	private void initSpecificPanel() {
		flwPanel.getChildren().clear();
		for (String panel : MainController.getInstance().getPanels()) {
			RadioButton button = new RadioButton(panel);
			button.setToggleGroup(startUpPanel);
			flwPanel.getChildren().add(button);
		}
		startUpPanel.getToggles().get(1).setSelected(true);;
	}

	@FXML
	private void cancel(ActionEvent e) {
		close();
	}

	@FXML
	private void save(ActionEvent e) {

		close();
	}

	private void close() {
		Stage stage = (Stage) btnCancel.getScene().getWindow();
		stage.close();
	}
}
