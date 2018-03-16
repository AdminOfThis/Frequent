package gui.controller;

import java.io.IOException;
import java.net.URL;
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
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import main.Main;

public class IOChooserController implements Initializable {

	private static final Logger	LOG	= Logger.getLogger(IOChooserController.class);
	@FXML
	private ListView<String>	listIO;
	@FXML
	private Button				btnStart, btnQuit;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		listIO.getItems().setAll(ASIOController.getInputDevices());
		// Quit Button
		btnQuit.setOnAction(e -> Main.quit());
		btnStart.disableProperty().bind(listIO.getSelectionModel().selectedItemProperty().isNull());
		// Debug
	}

	@FXML
	private void start(ActionEvent e) {
		String selectedIO = listIO.getSelectionModel().getSelectedItem();
		LOG.info("Loading Driver \"" + selectedIO + "\"");
		loadMain(selectedIO);
		// }
		Stage stage = null;
		try {
			stage = (Stage) listIO.getScene().getWindow();
		}
		catch (Exception ex) {}
		if (stage != null) {
			stage.close();
		}
	}

	public void startDebug() {
		if (!listIO.getItems().isEmpty()) {
			listIO.getSelectionModel().select(0);
			LOG.info("DEBUG, Starting with index 0: " + listIO.getItems().get(0));
			start(new ActionEvent());
		}
	}

	private void loadMain(String ioName) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../gui/Main.fxml"));
		try {
			Parent p = loader.load();
			MainController controller = loader.getController();
			if (ioName != null) {
				controller.initIO(ioName);
			}
			Stage stage;
			try {
				stage = (Stage) listIO.getScene().getWindow();
			}
			catch (Exception e) {
				stage = new Stage();
			}
			stage.setResizable(true);
			stage.setScene(new Scene(p));
			stage.show();
		}
		catch (IOException e) {
			LOG.error("Unable to load Main GUI", e);
			Main.quit();
		}
	}
}
