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
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

	private static Logger		LOG;
	public static final String	TITLE			= "Frequent";
	private static final String	VERSION			= "0.0.5";
	private static final String	LOG_CONFIG_FILE	= "./log4j.ini";
	private static final String	GUI_IO_CHOOSER	= "/gui/gui/IOChooser.fxml";
	private static final String	LOGO			= "./../lib/logo_26.png";
	private static String		style			= "";
	private static boolean		debug			= false, fast = false;

	public static void main(String[] args) {
		initLogger();
		pasrseArgs(args);
		launch(args);
	}

	/**
	 * checks the start parameters for debug keyword and sets the debug flag to true if found
	 * 
	 * @param args
	 */
	private static void pasrseArgs(String[] args) {
		for (String arg : args) {
			if (arg.equalsIgnoreCase("-debug")) {
				debug = true;
				LOG.info("Enabling debug settings");
				FileIO.setCurrentDir(new File("."));
			} else if (arg.equalsIgnoreCase("-fast")) {
				LOG.info("Enabling fast UI elements");
				fast = true;
			} else if (arg.toLowerCase().startsWith("-style=")) {
				LOG.info("Loading custom style");
				try {
					style = arg.substring(arg.indexOf("=") + 1);
					int index = -1;
					int count = 0;
					for (String a : args) {
						if (a.equals(arg)) {
							index = count;
							break;
						}
						count++;
					}
					if (index < 0) {
						LOG.warn("Error while loading style from commandline");
						continue;
					}
					index++;
					String a = args[index];
					while (a != null && !a.startsWith("-")) {
						style = style + " " + a;
						index++;
						if (index >= args.length) {
							break;
						}
						a = args[index];
					}
					LOG.info("Loaded style as: " + style);
				}
				catch (Exception e) {
					LOG.warn("Unable to load style from commandline");
					LOG.debug("", e);
				}
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
		try {
			primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(LOGO)));
		}
		catch (Exception e) {
			LOG.error("Unable to load logo", e);
		}
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

	public static String getStyle() {
		return style;
	}

	public static boolean isFast() {
		return fast;
	}
}
