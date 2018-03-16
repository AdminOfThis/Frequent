package main;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import control.ASIOController;
import gui.controller.IOChooserController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	private static Logger			LOG;
	public static final String		LOG_CONFIG_FILE	= "./log4j.ini";
	private static final boolean	DEBUG			= true;

	public static void main(String[] args) {
		initLogger();
		launch(args);
	}

	private static void initLogger() {
		try {
			PropertyConfigurator.configure(LOG_CONFIG_FILE);
			LOG = Logger.getLogger(Main.class);
		}
		catch (Exception e) {
			LOG.fatal("Unexpected error while initializing logging", e);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../gui/gui/IOChooser.fxml"));
		Parent parent = loader.load();
		IOChooserController controller = loader.getController();
		primaryStage.setScene(new Scene(parent));
		primaryStage.setOnCloseRequest(e -> Main.quit());
		primaryStage.setTitle("AudioAnalyzer");
		primaryStage.setResizable(false);
		primaryStage.setOnShowing(e -> {
			if (Main.isDebug()) {
				controller.startDebug();
			}
		});
		primaryStage.show();
	}

	public static void close() {
		LOG.info("Stopping GUI");
		Platform.exit();
		LOG.info("Stopping AudioDriver");
		ASIOController.getInstance().shutdown();
		LOG.info("Bye");
		System.exit(0);
	}

	public static void quit() {
		LOG.info("Quitting...");
		LOG.info("Bye");
		System.exit(0);
	}

	public static boolean isDebug() {
		return DEBUG;
	}
}
