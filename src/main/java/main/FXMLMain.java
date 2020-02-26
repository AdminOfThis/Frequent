package main;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.FileIO;
import gui.FXMLUtil;
import gui.MainGUI;
import gui.controller.MainController;
import gui.dialog.ConfirmationDialog;
import gui.dialog.IOChooserController;
import gui.dialog.InformationDialog;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import main.Constants.WINDOW_OPEN;
import preferences.PropertiesIO;

/**
 * 
 * @author AdminOfThis
 *
 */
public class FXMLMain extends MainGUI {

	private static final int TERMINATE_TIMEOUT = 5000;

	private static final Logger LOG = LogManager.getLogger(FXMLMain.class);

	private static final String GUI_IO_CHOOSER = "/fxml/dialog/IOChooser.fxml";
	private static final String GUI_MAIN_PATH = "/fxml/Main.fxml";
	private static final String LOGO = "/logo/logo_64.png";

	private static FXMLMain instance;

	private Scene loginScene;
	private Scene mainScene;
	private IOChooserController loginController;

	public static FXMLMain getInstance() {
		return instance;
	}

	@Override
	public void init() throws Exception {
		if (!Main.isInitialized()) {
			Main.initialize();
		}
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

	/**
	 * 
	 * @return The relative path to the logo of the application
	 */
	public static String getLogoPath() {
		return LOGO;
	}

	/**
	 * Shows a dialog that there is already an instance of the application running
	 */
	public static void showAlreadyRunningDialog() {
		// this will prepare JavaFX toolkit and environment
		new JFXPanel();
		Platform.runLater(() -> {
			InformationDialog dialog = new InformationDialog("Application is already running", true);
			dialog.setText("Another instance of this application is already running");
			dialog.setSubText("Please use the other instance,\r\nor terminate the other instance and launch the application again.");
			dialog.show();
			dialog.getDialogPane().getScene().getWindow().centerOnScreen();
		});
	}

	/**
	 * checks if the application can be closed
	 */
	public boolean askForClose() {
		boolean result = true;
		try {
			if (!Main.isDebug() && PropertiesIO.getBooleanProperty(Constants.SETTING_WARN_UNSAVED_CHANGES)) {
				LOG.info("Checking for unsaved changes");
				if (FileIO.unsavedChanges()) {
					LOG.info("Unsaved changes found");
					ConfirmationDialog dialog = new ConfirmationDialog("Save changes before exit?", true);
					Optional<ButtonType> diaresult = dialog.showAndWait();
					if (!diaresult.isPresent()) {
						result = false;
					}
					if (diaresult.get() == ButtonType.YES) {
						boolean saveResult = MainController.getInstance().save(new ActionEvent());
						if (!saveResult) {
							LOG.info("Saving cancelled");
							result = false;
						}
					} else if (diaresult.get() == ButtonType.NO) {
						LOG.info("Closing without saving unfinished changes");
					} else if (diaresult.get() == ButtonType.CANCEL) {
						LOG.info("Cancelled closing of program");
						result = false;
					}
				}
			}

			result = true;
		} catch (Exception e) {
			LOG.error("Problem closing window", e);
		}
		return result;
	}

	/**
	 * Returns the initialized main scene of the program
	 * 
	 * @return mainScene the main Scene of the application
	 */
	public Scene getScene() {
		return mainScene;
	}

	/**
	 * Sets the progress in the preloader
	 * 
	 * @param prog the progress of loading the main window
	 */
	public void setProgress(final double prog) {
		notifyPreloader(new Preloader.ProgressNotification(prog));
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		LOG.info("Showing IOChooser");
		primaryStage.setScene(loginScene);
		primaryStage.setOnCloseRequest(e -> {

			new Thread(() -> {
				LOG.info("Close requested");
				writePos(primaryStage);
				if (FXMLMain.getInstance().askForClose()) {
					finish();
				} else {
					MainController.getInstance().setStatus("Close cancelled");
					e.consume();
				}
			}).start();
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
	 * Terminates the application. Should always be the last function called
	 */
	private void finish() {
		LOG.info("Stopping GUI");
		Platform.exit();
		LOG.info("Stopping AudioDriver");
		ASIOController.getInstance().shutdown();
		LOG.info("Waiting for unfinished threads");
		try {
			boolean running = false;
			// Wait until all Threads that are not daemons terminate
			long timeStart = System.currentTimeMillis();
			while (running && (System.currentTimeMillis() - timeStart < TERMINATE_TIMEOUT)) {
				Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
				for (Thread thread : threadSet) {
					if ((thread.isAlive() && !thread.isDaemon())) {
						running = false;
						break;
					}
				}
				Thread.yield();
			}
			if ((System.currentTimeMillis() - timeStart >= TERMINATE_TIMEOUT)) {
				LOG.info("Waiting for finishing Threads timed out, will close forcefully");
			}
		} catch (Exception e) {
			LOG.error("Unable to wait until all threads finish");
		}
		LOG.info("Bye");
		System.exit(0);
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
			FXMLMain.getInstance().askForClose();
		}
		return null;
	}

	private void writePos(final Stage stage) {
		try {
			String value = WINDOW_OPEN.DEFAULT.toString();
			if (PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN) != null) {
				value = PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN).split(",")[0];
			}
			if (value.contains(",")) {
				value = value.split(",")[0];
			}

			if (Objects.equals(Constants.WINDOW_OPEN.DEFAULT, Constants.WINDOW_OPEN.valueOf(value)) || Objects.equals(Constants.WINDOW_OPEN.MAXIMIZED, Constants.WINDOW_OPEN.valueOf(value))) {
				if (stage.isMaximized()) {
					value = Constants.WINDOW_OPEN.MAXIMIZED.toString();
				} else {
					value = Constants.WINDOW_OPEN.DEFAULT.toString();
				}
				if (value != null && !value.isEmpty()) {
					value += ",";
				}
				value = value += Math.round(stage.getWidth()) + "," + Math.round(stage.getHeight()) + "," + Math.round(stage.getX()) + "," + Math.round(stage.getY()) + "," + Boolean.toString(stage.isFullScreen());
				PropertiesIO.setProperty(Constants.SETTING_WINDOW_OPEN, value);
			}
		} catch (Exception e) {
			LOG.error("Problem writing window position", e);
		}

	}

}
