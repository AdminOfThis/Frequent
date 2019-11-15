package gui.dialog;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import gui.FXMLUtil;
import gui.controller.MainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import main.Main;

public class IOChooserController implements Initializable {

	private static final Logger LOG = LogManager.getLogger(IOChooserController.class);
	@FXML
	private Parent root;
	@FXML
	private ChoiceBox<String> listIO;
	@FXML
	private Button btnStart, btnQuit;
	@FXML
	private Label label;
	private Scene mainScene;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		Collection<String> ioList = ASIOController.getPossibleDrivers();
		LOG.info("Loaded " + ioList.size() + " possible drivers");
		label.setText(ioList.size() + " Driver(s)");
		listIO.getItems().setAll(ioList);
		if (listIO.getItems().size() > 0) {
			listIO.getSelectionModel().select(0);
		}
		// Quit Button
		btnQuit.setOnAction(e -> ((Stage) btnQuit.getScene().getWindow()).close());
		btnStart.disableProperty().bind(listIO.getSelectionModel().selectedItemProperty().isNull());
		// Debug
		FXMLUtil.setStyleSheet(root);
	}

	@FXML
	private void start(ActionEvent e) {
		String selectedIO = null;
		if (!Main.isDebug()) {
			selectedIO = listIO.getSelectionModel().getSelectedItem();
		} else if (!listIO.getItems().isEmpty()) {
			selectedIO = listIO.getItems().get(0);
		}
		LOG.info("Loading Main-Window with selected Driver \"" + selectedIO + "\"");
		launchMain(selectedIO);
		// }
		Stage stage = null;
		try {
			stage = (Stage) listIO.getScene().getWindow();
		} catch (Exception ex) {
		}
		if (stage != null) {
			stage.close();
		}
		e.consume();
	}

	private void launchMain(String selectedIO) {
		if (selectedIO != null && !selectedIO.isEmpty()) {
			MainController.getInstance().initIO(selectedIO);
		}
		Stage stage = (Stage) listIO.getScene().getWindow();
		stage.setScene(mainScene);
		stage.setResizable(true);
		stage.centerOnScreen();
	}

	public void startDebug() {
		// if (!listIO.getItems().isEmpty()) {
		LOG.warn("Starting without selected driver, for DEBUG purposes only!");
		start(new ActionEvent());
		// }
	}

	public void setMainScene(Scene scene) {
		mainScene = scene;
	}
}
