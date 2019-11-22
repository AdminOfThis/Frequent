package main;

import java.io.IOException;
import java.util.Optional;

import control.ASIOController;
import data.FileIO;
import data.RTAIO;
import gui.FXMLUtil;
import gui.MainGUI;
import gui.controller.MainController;
import gui.dialog.ConfirmationDialog;
import gui.dialog.IOChooserController;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import preferences.PropertiesIO;

public class FXMLMain extends MainGUI {

	private static final String GUI_IO_CHOOSER = "/fxml/dialog/IOChooser.fxml";
	private static final String GUI_MAIN_PATH = "/fxml/Main.fxml";
	private static final String LOGO = "/logo/logo_64.png";

	private static FXMLMain instance;

	private Scene loginScene;
	private Scene mainScene;
	private IOChooserController loginController;

	@Override
	public void init() throws Exception {
		super.init();
		if (FXMLUtil.getDefaultStyle().isEmpty()) {
			Main.initColors();
		}
		instance = this;
		notifyPreloader(new Preloader.ProgressNotification(0.1));
		Parent parent = FXMLUtil.loadFXML(Main.class.getResource(GUI_IO_CHOOSER));
		loginController = (IOChooserController) FXMLUtil.getController();
		FXMLUtil.setStyleSheet(parent);
		loginScene = new Scene(parent);
		notifyPreloader(new Preloader.ProgressNotification(0.2));
		mainScene = loadMain();
		loginController.setMainScene(mainScene);
		notifyPreloader(new Preloader.ProgressNotification(0.95));
	}

	private Scene loadMain() {
		LOG.debug("Loading from: " + GUI_MAIN_PATH);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(GUI_MAIN_PATH));
		try {
			Parent p = loader.load();
			// MainController controller = loader.getController();
			// if (ioName != null) {
			// controller.initIO(ioName);
			// }
			LOG.info("Main Window loaded");
			return new Scene(p);
		} catch (IOException e) {
			LOG.error("Unable to load Main GUI", e);
			FXMLMain.getInstance().close();
		}
		return null;
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		LOG.info("Showing IOChooser");
		primaryStage.setScene(loginScene);
		primaryStage.setOnCloseRequest(e -> {
			if (!FXMLMain.getInstance().close()) {
				MainController.getInstance().setStatus("Close cancelled");
				e.consume();
			}
		});
		primaryStage.setTitle(Main.getReadableTitle());
		primaryStage.setResizable(false);
		FXMLUtil.setIcon(primaryStage, LOGO);
		primaryStage.setOnShowing(e -> {
			if (Main.isDebug()) {
				loginController.startDebug();
			}
		});
		primaryStage.show();
	}

	/**
	 * stops all running threads and terminates the gui
	 */
	@Override
	public boolean close() {

		if (!Main.isDebug() && PropertiesIO.getBooleanProperty(Constants.SETTING_WARN_UNSAVED_CHANGES)) {
			LOG.info("Checking for unsaved changes");
			if (FileIO.unsavedChanges()) {
				LOG.info("Unsaved changes found");
				ConfirmationDialog dialog = new ConfirmationDialog("Save changes before exit?", true);
				Optional<ButtonType> result = dialog.showAndWait();
				if (!result.isPresent()) {
					return false;
				}
				if (result.get() == ButtonType.YES) {
					boolean saveResult = MainController.getInstance().save(new ActionEvent());
					if (!saveResult) {
						LOG.info("Saving cancelled");
						return false;
					}
				} else if (result.get() == ButtonType.NO) {
					LOG.info("Closing without saving unfinished changes");
				} else if (result.get() == ButtonType.CANCEL) {
					LOG.info("Cancelled closing of program");
					return false;
				}
			}
		}
		LOG.info("Stopping GUI");
		Platform.exit();
		if (ASIOController.getInstance() != null) {
			LOG.info("Stopping AudioDriver");
			ASIOController.getInstance().shutdown();
		}
		LOG.info("Deleting RTA file");
		RTAIO.deleteFile();
		LOG.info("Bye");
		System.exit(0);
		return true;
	}

	/******************* GETTERS AND SETTERS ****************/

	public static FXMLMain getInstance() {
		return instance;
	}

	public void setProgress(final double prog) {
		notifyPreloader(new Preloader.ProgressNotification(prog));
	}

	public Scene getScene() {
		return mainScene;
	}

	public static String getLogoPath() {
		return LOGO;
	}

}
