package main;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import control.ASIOController;
import data.FileIO;
import gui.controller.IOChooserController;
import gui.utilities.FXMLUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	private static Logger		LOG;
	public static final String	TITLE			= "Frequent";
	private static final String	VERSION			= "0.0.1";
	private static final String	LOG_CONFIG_FILE	= "./log4j.ini";
	private static final String	GUI_IO_CHOOSER	= "IOChooser.fxml";
	private static boolean		debug			= false;

	public static void main(String[] args) {
		initLogger();
		setDebug(args);
		launch(args);
	}

	/**
	 * checks the start parameters for debug keyword and sets the debug flag to true if found
	 * 
	 * @param args
	 */
	private static void setDebug(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("-debug")) {
				debug = true;
				LOG.info("Enabling debug settings");
				FileIO.setCurrentDir(new File("."));
				break;
			}
		}
	}

	/**
	 * sets up the Log4j logger ba reading the properties file
	 */
	private static void initLogger() {
		try {
			PropertyConfigurator.configure(LOG_CONFIG_FILE);
			LOG = Logger.getLogger(Main.class);
			LOG.info("=== Starting Frequent ===");
		}
		catch (Exception e) {
			LOG.fatal("Unexpected error while initializing logging", e);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOG.info("Showing IOChooser");
		Parent parent = FXMLUtil.loadFXML(GUI_IO_CHOOSER);
		IOChooserController controller = (IOChooserController) FXMLUtil.getController();
		primaryStage.setScene(new Scene(parent));
		primaryStage.setOnCloseRequest(e -> Main.close());
		primaryStage.setTitle(TITLE + " " + VERSION);
		primaryStage.setResizable(false);
		primaryStage.setOnShowing(e -> {
			if (Main.isDebug()) {
				controller.startDebug();
			}
		});
		primaryStage.show();
	}

	/**
	 * stops all running threads and terminates the gui
	 */
	public static void close() {
		LOG.info("Stopping GUI");
		Platform.exit();
		if (ASIOController.getInstance() != null) {
			LOG.info("Stopping AudioDriver");
			ASIOController.getInstance().shutdown();
		}
		LOG.info("Bye");
		System.exit(0);
	}

	public static boolean isDebug() {
		return debug;
	}
}
