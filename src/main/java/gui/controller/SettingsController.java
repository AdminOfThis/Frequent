package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.ASIOController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import main.Main;

public class SettingsController implements Initializable {

	private static final Number[]	BUFFERS	= new Number[] { 64, 128, 256, 512, 1024, 2048 };
	@FXML
	private BorderPane				root;
	@FXML
	private ComboBox<Number>		chbBuffer;
	@FXML
	private ComboBox<String>		chbDevice;
	@FXML
	private Button					btnSave, btnCancel;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		root.setStyle(Main.getStyle());
		// Init data
		chbBuffer.getItems().addAll(BUFFERS);
		chbDevice.getItems().addAll(ASIOController.getPossibleDrivers());
		if (ASIOController.getInstance() != null) {
			chbBuffer.setValue(ASIOController.getInstance().getBufferSize());
			chbDevice.setValue(ASIOController.getInstance().getDevice());
		}
	}
}
