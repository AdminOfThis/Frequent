package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

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
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

	private static String		color_accent	= "#5EBF23";
	private static String		color_base		= "#1A1A1A";
	private static String		color_focus		= "#7DFF2F";

	private static Logger		LOG;
	private static final String	VERSION_KEY		= "Implementation-Version";
	private static final String	TITLE_KEY		= "Implementation-Title";
	private static final String	LOG_CONFIG_FILE	= "./log4j.ini";
	private static final String	GUI_IO_CHOOSER	= "/fxml/IOChooser.fxml";
	private static final String	GUI_MAIN_PATH	= "/fxml/Main.fxml";
	private static final String	LOGO			= "/logo/logo_64.png";
	private static String		style			= "";
	private static boolean		debug			= false, fast = false;
	private static String		version, title;
	private static Main			instance;

	/**
	 * stops all running threads and terminates the gui
	 */
	public static boolean close() {
		if (!Main.isDebug()) {
			LOG.info("Checking for unsaved changes");
			if (FileIO.unsavedChanges()) {
				ButtonType type = MainController.getInstance().showConfirmDialog("Save changes before exit?");
				if (type == ButtonType.OK) {
					boolean result = MainController.getInstance().save(new ActionEvent());
					if (!result) {
						LOG.info("Saving cancelled");
						return false;
					}
				} else if (type == ButtonType.CANCEL)
					return false;
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

	public static String getAccentColor() {
		return color_accent;
	}

	public static String getBaseColor() {
		return color_base;
	}

	public static String getFocusColor() {
		return color_focus;
	}

	public static String getFromManifest(final String key, final String def) {
		try {
			Enumeration<URL> resources = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				try {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					if (getTitle().equalsIgnoreCase(manifest.getMainAttributes().getValue("Specification-Version")))
						// check that this is your manifest and do what you need
						// or get the next one
						return manifest.getMainAttributes().getValue(key);
				} catch (IOException e) {
					LOG.warn(e);
				}
			}
		} catch (Exception e) {
			LOG.warn("Unable to read version from manifest");
			LOG.debug("", e);
		}
		return def;
	}

	public static Main getInstance() {
		return instance;
	}

	public static String getOnlyTitle() {
		return title;
	}

	public static String getStyle() {
		return style;
	}

	public static String getTitle() {
		return title + " " + version;
	}

	public static String getVersion() {
		return version;
	}

	/**
	 * sets up the Log4j logger ba reading the properties file
	 */
	private static void initLogger() {
		try {
			PropertyConfigurator.configure(LOG_CONFIG_FILE);
			LOG = Logger.getLogger(Main.class);
		} catch (Exception e) {
			LOG.fatal("Unexpected error while initializing logging", e);
		}
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isFast() {
		return fast;
	}

	public static void main(final String[] args) {
		initLogger();
		title = getFromManifest(TITLE_KEY, "Frequent");
		version = getFromManifest(VERSION_KEY, "Local");
		LOG.info(" === " + getTitle() + " ===");
		parseArgs(args);
		setColors();
		LauncherImpl.launchApplication(Main.class, PreLoader.class, args);
	}

	/**
	 * checks the start parameters for keywords and sets the debug flag to true
	 * if found
	 *
	 * @param args
	 */
	private static void parseArgs(final String[] args) {
		for (String arg : args) {
			if (arg.equalsIgnoreCase("-debug")) {
				debug = true;
				LOG.info("Enabling debug settings");
				FileIO.setCurrentDir(new File("."));
			} else if (arg.equalsIgnoreCase("-fast")) {
				LOG.info("Enabling fast UI elements");
				fast = true;
			} else if (arg.toLowerCase().startsWith("-base=")) {
				color_base = arg.replace("-base=", "");
			} else if (arg.toLowerCase().startsWith("-accent=")) {
				color_accent = arg.replace("-accent=", "");
			} else if (arg.toLowerCase().startsWith("-focus-color=")) {
				color_focus = arg.replace("-focus-color=", "");
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

	private static void setColors() {
		style = "-fx-base:" + color_base + "; -fx-accent:" + color_accent + "; -fx-focus-color:" + color_focus;
	}

	private Scene				loginScene;

	private IOChooserController	loginController;

	@Override
	public void init() throws Exception {
		super.init();
		instance = this;
		notifyPreloader(new Preloader.ProgressNotification(0.25));
		Parent parent = FXMLUtil.loadFXML(GUI_IO_CHOOSER);
		loginController = (IOChooserController) FXMLUtil.getController();
		loginScene = new Scene(parent);
		notifyPreloader(new Preloader.ProgressNotification(0.4));
		Scene mainScene = loadMain();
		loginController.setMainScene(mainScene);
		notifyPreloader(new Preloader.ProgressNotification(0.95));
		Thread.sleep(100);
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
			Main.close();
		}
		return null;
	}

	public void setProgress(final double prog) {
		try {
			notifyPreloader(new Preloader.ProgressNotification(prog));
		} catch (Exception e) {
		}
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		LOG.info("Showing IOChooser");
		primaryStage.setScene(loginScene);
		primaryStage.setOnCloseRequest(e -> {
			if (!Main.close()) {
				MainController.getInstance().setStatus("Close cancelled");
				e.consume();
			}
		});
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
}
