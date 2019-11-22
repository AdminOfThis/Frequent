package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.FileIO;
import gui.FXMLUtil;
import gui.MainGUI;
import gui.preloader.PreLoader;
import javafx.application.Application;
import preferences.PropertiesIO;

public class Main {
	private static final String DEFAULT_PROPERTIES_PATH = "./settings.conf";

	private static final String POM_TITLE = "Frequent";
	private static final String VERSION_KEY = "Implementation-Version";
	private static final String TITLE_KEY = "Implementation-Title";

	private static String title = "";
	private static String version = "";

	private static String color_accent = "#5EBF23";
	private static String color_base = "#1A1A1A";
	private static String color_focus = "#7DFF2F";
	private static String style = "";
	private static String propertiesPath = DEFAULT_PROPERTIES_PATH;
	private static Logger LOG = LogManager.getLogger(Main.class);
	private static boolean debug = false, fast = false;

	/**
	 * The main method of the programm. Starts with parsing arguments, then launches
	 * the GUI
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) {
		try {
			Thread.setDefaultUncaughtExceptionHandler(Constants.EMERGENCY_EXCEPTION_HANDLER);
			initialize(POM_TITLE);
			LOG.info(" === " + getReadableTitle() + " ===");
			if (parseArgs(args)) {
				setColors();
				loadProperties();
				System.setProperty("javafx.preloader", PreLoader.class.getName());
				Application.launch(FXMLMain.class, args);
			}
		} catch (Exception exception) {
			LOG.fatal("Fatal uncaught exception: ", exception);
		} catch (Error error) {
			LOG.fatal("Fatal uncaught error: ", error);
		}
	}

	public static void initialize(String pomTitle) {
		title = getFromManifest(TITLE_KEY, POM_TITLE, pomTitle);
		version = getFromManifest(VERSION_KEY, "Local Build", pomTitle);
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

	public static String getFromManifest(final String key, final String def, String pomTitle) {
		try {
			Enumeration<URL> resources = MainGUI.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				try {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					if (pomTitle.equalsIgnoreCase(manifest.getMainAttributes().getValue("Specification-Title")))
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

	private static void loadProperties() {
		PropertiesIO.setSavePath(propertiesPath);
		PropertiesIO.loadProperties();

	}

	private static void setColors() {
		style = "-fx-base:" + color_base + "; -fx-accent:" + color_accent + "; -fx-focus-color:" + color_focus + "; ";
		FXMLUtil.setDefaultStyle(style);
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

	public static String getStyle() {
		return style;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isFast() {
		return fast;
	}

	public String getPOMTitle() {
		return POM_TITLE;
	}

	public static String getReadableTitle() {
		return getOnlyTitle() + " " + getVersion();
	}

	public String getTitle() {
		return title;
	}

	public static String getVersion() {
		return version;
	}

	public static String getOnlyTitle() {
		if (title == null || title.isEmpty()) {
			return "";
		}
		return title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
	}

}
