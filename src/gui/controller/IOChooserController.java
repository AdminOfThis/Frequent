package gui.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import main.Main;

public class IOChooserController implements Initializable {

	private static final String	GUI_MAIN_PATH	= "/gui/gui/Main.fxml";
	private static final Logger	LOG				= Logger.getLogger(IOChooserController.class);
	@FXML
	private Parent				root;
	@FXML
	private ChoiceBox<String>	listIO;
	@FXML
	private Button				btnStart, btnQuit;
	@FXML
	private Label				label;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		root.setStyle(Main.getStyle());
		Collection<String> ioList = ASIOController.getInputDevices();
		LOG.info("Loaded " + ioList.size() + " possible drivers");
		label.setText(ioList.size() + " Driver(s)");
		listIO.getItems().setAll(ioList);
		if (listIO.getItems().size() > 0) {
			listIO.getSelectionModel().select(0);
		}
		// Quit Button
		btnQuit.setOnAction(e -> Main.close());
		btnStart.disableProperty().bind(listIO.getSelectionModel().selectedItemProperty().isNull());
		// Debug
	}

	@FXML
	private void start(ActionEvent e) {
		String selectedIO = listIO.getSelectionModel().getSelectedItem();
		LOG.info("Loading Main-Window with selected Driver \"" + selectedIO + "\"");
		loadMain(selectedIO);
		// }
		Stage stage = null;
		try {
			stage = (Stage) listIO.getScene().getWindow();
		} catch (Exception ex) {
		}
		if (stage != null) {
			stage.close();
		}
	}

	public void startDebug() {
		// if (!listIO.getItems().isEmpty()) {
		listIO.getSelectionModel().select(0);
		if (!listIO.getItems().isEmpty()) {
			LOG.info("DEBUG, Starting with index 0: " + listIO.getItems().get(0));
		} else {
			LOG.warn("Starting without selected driver, for DEBUG purposes only!");
		}
		start(new ActionEvent());
		// }
	}

	private void loadMain(String ioName) {
		LOG.debug("Loading from: " + GUI_MAIN_PATH);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(GUI_MAIN_PATH));
		try {
			Parent p = loader.load();
			MainController controller = loader.getController();
			if (ioName != null) {
				controller.initIO(ioName);
			}
			Stage stage;
			try {
				stage = (Stage) listIO.getScene().getWindow();
			} catch (Exception e) {
				stage = new Stage();
			}
			stage.setResizable(true);
			stage.setScene(new Scene(p));
			stage.centerOnScreen();
			stage.show();
			LOG.info("Main Window loaded");
		} catch (IOException e) {
			LOG.error("Unable to load Main GUI", e);
			Main.close();
		}
	}
}
