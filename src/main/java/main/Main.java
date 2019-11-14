package main;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.FileIO;
import data.RTAIO;
import dialog.ConfirmationDialog;
import gui.FXMLUtil;
import gui.MainGUI;
import gui.controller.IOChooserController;
import gui.controller.MainController;
import gui.preloader.PreLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import preferences.PropertiesIO;

public class Main extends MainGUI {

	private static final String POM_TITLE = "Frequent";
	private static final String GUI_IO_CHOOSER = "/fxml/IOChooser.fxml";
	private static final String GUI_MAIN_PATH = "/fxml/Main.fxml";
	private static final String LOGO = "/logo/logo_64.png";
	private static final String DEFAULT_PROPERTIES_PATH = "./settings.conf";

	private static String color_accent = "#5EBF23";
	private static String color_base = "#1A1A1A";
	private static String color_focus = "#7DFF2F";
	private static String style = "";
	private static String propertiesPath = DEFAULT_PROPERTIES_PATH;
	private static Logger LOG = LogManager.getLogger(Main.class);
	private static boolean debug = false, fast = false;
	private static Main instance;
	private Scene loginScene;
	private Scene mainScene;
	private IOChooserController loginController;

	/**
	 * The main method of the programm. Starts with parsing arguments, then launches
	 * the GUI
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		MainGUI.initialize(POM_TITLE);
		setTitle(POM_TITLE);
		LOG.info(" === " + getReadableTitle() + " ===");
		if (parseArgs(args)) {
			setColors();
			loadProperties();
			System.setProperty("javafx.preloader", PreLoader.class.getName());
			Application.launch(Main.class, args);
		}
	}

	@Override
	public void init() throws Exception {
		super.init();
		instance = this;
		notifyPreloader(new Preloader.ProgressNotification(0.1));
		Parent parent = FXMLUtil.loadFXML(Main.class.getResource(GUI_IO_CHOOSER));
		FXMLUtil.setStyleSheet(parent);
		parent.setStyle(Main.getStyle());
		loginController = (IOChooserController) FXMLUtil.getController();
		loginScene = new Scene(parent);
		notifyPreloader(new Preloader.ProgressNotification(0.2));
		mainScene = loadMain();
		loginController.setMainScene(mainScene);
		notifyPreloader(new Preloader.ProgressNotification(0.95));
	}

	/**
	 * checks the start parameters for keywords and sets the debug flag to true if
	 * found
	 *
	 * @param args
	 * @return true if the programm shoudl continue, false if it should terminate
	 */
	private static boolean parseArgs(final String[] args) {
		boolean result = true;
		for (String arg : args) {
			if (args.length == 1 && arg.equalsIgnoreCase("-version")) {
				LOG.info(getReadableTitle());
				System.out.println(getReadableTitle());
				result = false;
			} else if (arg.equalsIgnoreCase("-debug")) {
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
		return result;
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
			Main.getInstance().close();
		}
		return null;
	}

	private static void loadProperties() {
		PropertiesIO.setSavePath(propertiesPath);
		PropertiesIO.loadProperties();

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
			if (!Main.getInstance().close()) {
				MainController.getInstance().setStatus("Close cancelled");
				e.consume();
			}
		});
		primaryStage.setTitle(getReadableTitle());
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

	private static void setColors() {
		style = "-fx-base:" + color_base + "; -fx-accent:" + color_accent + "; -fx-focus-color:" + color_focus + "; ";
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

	public static Main getInstance() {
		return instance;
	}

	public static String getStyle() {
		return style;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isFast() {
		return fast;
	}

	public Scene getScene() {
		return mainScene;
	}

	@Override
	public String getPOMTitle() {
		return POM_TITLE;
	}

	public static String getLogoPath() {
		return LOGO;
	}

}
