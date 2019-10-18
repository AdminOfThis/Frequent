package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import main.Constants;
import main.Constants.RESTORE_PANEL;
import preferences.PropertiesIO;

/**
 * 
 * @author AdminOfThis
 *
 */
public class SettingsController implements Initializable {

	private static final Logger LOG = LogManager.getLogger(SettingsController.class);

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
	private RadioButton rBtnPanelNothing, rBtnPanelLast, rBtnPanelSpecific;

	@FXML
	private FlowPane flwPanel;
	@FXML
	private CheckBox chkRestoreLastFile, chkWarnUnsavedChanges;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		checkFXML();

		// FXMLUtil.setIcon((Stage) root.getScene().getWindow(), Main.getLogoPath());

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
		loadValues();
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
		startUpPanel.getToggles().get(1).setSelected(true);
		;
	}

	private void loadValues() {

		loadRestorePanel();
		loadFile();
	}

	private void loadRestorePanel() {
		setSettingSecure(() -> rBtnPanelNothing.setSelected(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.NOTHING.name())), Constants.SETTING_RESTORE_PANEL);
		setSettingSecure(() -> rBtnPanelLast.setSelected(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.LAST.name())), Constants.SETTING_RESTORE_PANEL);
		if (PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL) != null && PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.SPECIFIC.name())) {
			rBtnPanelSpecific.setSelected(true);
			if (PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC) != null) {
				for (Toggle t : startUpPanel.getToggles()) {
					ToggleButton btn = (ToggleButton) t;
					if (btn.getText().equals(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC))) {
						btn.setSelected(true);
						break;
					}
				}
			}
		}
	}

	private void loadFile() {
		setSettingSecure(() -> chkRestoreLastFile.setSelected(Boolean.parseBoolean(PropertiesIO.getProperty(Constants.SETTING_RELOAD_LAST_FILE))), Constants.SETTING_RELOAD_LAST_FILE);
		setSettingSecure(() -> chkWarnUnsavedChanges.setSelected(Boolean.parseBoolean(PropertiesIO.getProperty(Constants.SETTING_WARN_UNSAVED_CHANGES))), Constants.SETTING_WARN_UNSAVED_CHANGES);

	}

	private void setSettingSecure(Runnable r, String setting) {
		try {
			r.run();
		} catch (Exception e) {
			LOG.warn("Unable to load Setting \"" + setting + "\"");
		}
	}

	@FXML
	private void cancel(ActionEvent e) {
		close();
	}

	@FXML
	private void save(ActionEvent e) {

		// Panel restore
		if (rBtnPanelNothing.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.NOTHING.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, Integer.toString(-1), false);
		} else if (rBtnPanelLast.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.LAST.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, Integer.toString(-1), false);
		} else if (rBtnPanelSpecific.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.SPECIFIC.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, ((ToggleButton) startUpPanel.getSelectedToggle()).getText(), false);
		} else {
			LOG.error("State should not be reached");
		}
		// File/saving
		PropertiesIO.setProperty(Constants.SETTING_RELOAD_LAST_FILE, Boolean.toString(chkRestoreLastFile.isSelected()), false);
		PropertiesIO.setProperty(Constants.SETTING_WARN_UNSAVED_CHANGES, Boolean.toString(chkWarnUnsavedChanges.isSelected()), false);

		PropertiesIO.saveProperties();
		close();
	}

	private void close() {
		Stage stage = (Stage) btnCancel.getScene().getWindow();
		stage.close();
	}
}
