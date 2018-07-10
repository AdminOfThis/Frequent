package main;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sun.javafx.application.LauncherImpl;

import control.ASIOController;
import data.FileIO;
import data.RTAIO;
import gui.controller.IOChooserController;
import gui.controller.MainController;
import gui.preloader.PreLoader;
import gui.utilities.FXMLUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

	private static Logger		LOG;
	public static final String	TITLE			= "Frequent";
	public static final String	VERSION			= "0.2.1";
	private static final String	LOG_CONFIG_FILE	= "./log4j.ini";
	private static final String	GUI_IO_CHOOSER	= "/gui/gui/IOChooser.fxml";
	private static final String	GUI_MAIN_PATH	= "/gui/gui/Main.fxml";

	private static final String	LOGO			= "/res/logo_64.png";
	private static String		style			= "";
	private static boolean		debug			= false, fast = false;

	private Scene				loginScene;
	private IOChooserController	loginController;

	public static void main(String[] args) {
		initLogger();
		parseArgs(args);
		LauncherImpl.launchApplication(Main.class, PreLoader.class, args);
	}

	/**
	 * checks the start parameters for keywords and sets the debug flag to true
	 * if found
	 * 
	 * @param args
	 */
	private static void parseArgs(String[] args) {
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
				} catch (Exception e) {
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
		} catch (Exception e) {
			LOG.fatal("Unexpected error while initializing logging", e);
		}
	}

	@Override
	public void init() throws Exception {
		super.init();
		notifyPreloader(new Preloader.ProgressNotification(0.25));
		Parent parent = FXMLUtil.loadFXML(GUI_IO_CHOOSER);
		loginController = (IOChooserController) FXMLUtil.getController();
		loginScene = new Scene(parent);
		notifyPreloader(new Preloader.ProgressNotification(0.5));
		Scene mainScene = loadMain();
		loginController.setMainScene(mainScene);
		notifyPreloader(new Preloader.ProgressNotification(0.75));
		Thread.sleep(100);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOG.info("Showing IOChooser");
		primaryStage.setScene(loginScene);
		primaryStage.setOnCloseRequest(e -> Main.close());
		primaryStage.setTitle(getTitle());
		primaryStage.setResizable(false);
		try {
			primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(LOGO)));
		} catch (Exception e) {
			LOG.error("Unable to load logo");
			LOG.debug("", e);
		}
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
	public static void close() {
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
	}

	private Scene loadMain() {
		LOG.debug("Loading from: " + GUI_MAIN_PATH);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(GUI_MAIN_PATH));
		try {
			Parent p = loader.load();
//			MainController controller = loader.getController();
			// if (ioName != null) {
			// controller.initIO(ioName);
			// }
			LOG.info("Main Window loaded");
			return new Scene(p);
		} catch (IOException e) {
			LOG.error("Unable to load Main GUI", e);
			Main.close();
		}
		return null;
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

	public static String getTitle() {
		return TITLE + " " + VERSION;
	}
}
