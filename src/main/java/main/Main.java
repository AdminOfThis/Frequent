package main;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import data.FileIO;
import gui.FXMLUtil;
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
	private static String[] log4jArgs = new String[3];
	private static boolean noLog = true;
	private static boolean development = false;
	private static Logger LOG = LogManager.getLogger(Main.class);
	private static boolean debug = false, fast = false;

	/**
	 * The main method of the programm. Starts with parsing arguments, then launches
	 * the GUI
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		try {

			Thread.setDefaultUncaughtExceptionHandler(Constants.EMERGENCY_EXCEPTION_HANDLER);
			initialize();
			LOG.info(" === " + getReadableTitle() + " ===");
			if (parseArgs(args)) {

				loadProperties();
				initColors();
				initLog4jParams();
				if (createRunningLockFile()) {
					System.setProperty("javafx.preloader", PreLoader.class.getName());
					Application.launch(FXMLMain.class, args);
				} else {
					LOG.info("Application is already running, unable to run multiple instances");
					FXMLMain.showAlreadyRunningDialog();
				}
			}
		} catch (Exception exception) {
			LOG.fatal("Fatal uncaught exception: ", exception);
		} catch (Error error) {
			LOG.fatal("Fatal uncaught error: ", error);
		}
	}

	@SuppressWarnings("resource")
	private static boolean createRunningLockFile() {
		try {
			final File file = new File(Constants.LOCK_FILE);
			final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
			final FileLock fileLock = randomAccessFile.getChannel().tryLock();
			if (fileLock != null) {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							fileLock.release();
							randomAccessFile.close();
							file.delete();
						} catch (Exception e) {
							// log.error("Unable to remove lock file: " + lockFile, e);
						}
					}
				});
				return true;
			}
		} catch (Exception e) {
			LOG.error("Unable to create and/or lock file", e);
		}
		return false;
	}

	private static void initLog4jParams() {
		setErrorReporting(!development && PropertiesIO.getBooleanProperty(Constants.SETTING_ERROR_REPORTING, true), false);
		log4jArgs[Constants.LOG4J_INDEX_VERSION] = version;
		if (development) {
			LOG.info("Set environment to development");
			log4jArgs[Constants.LOG4J_INDEX_ENVIRONMENT] = "development";
		} else {
			log4jArgs[Constants.LOG4J_INDEX_ENVIRONMENT] = "production";
		}

		StringBuilder sb = new StringBuilder();
		for (String s : log4jArgs) {
			sb.append(s + ",");
		}
		String args = sb.substring(0, sb.length() - 1);
		LOG.debug("Set log4j args to: " + args);
		MainMapLookup.setMainArguments(log4jArgs);
	}

	public static void initialize() {
		title = getFromManifest(TITLE_KEY, POM_TITLE);
		version = getFromManifest(VERSION_KEY, "Local Build");
	}

	/**
	 * checks the start parameters for keywords and sets the debug flag to true if
	 * found
	 *
	 * @param args
	 * @return true if the programm should continue, false if it should terminate
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
			} else if (arg.equalsIgnoreCase("-nolog")) {
				noLog = false;
			} else if (arg.equalsIgnoreCase("-dev") || arg.equalsIgnoreCase("-development")) {
				LOG.info("Starting in development mode prevents external logging!");
				development = true;
			} else if (arg.toLowerCase().startsWith("-base=")) {
				color_base = arg.replace("-base=", "");
			} else if (arg.toLowerCase().startsWith("-accent=")) {
				color_accent = arg.replace("-accent=", "");
			} else if (arg.toLowerCase().startsWith("-focus-color=")) {
				color_focus = arg.replace("-focus-color=", "");
			} else if (arg.toLowerCase().startsWith("-style=")) {
				LOG.info("Loading custom style");
				parseStyle(arg, args);
			}
		}
		return result;
	}

	private static void parseStyle(String arg, String[] args) {
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
				return;
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

	public static String getFromManifest(final String key, final String def) {
		try {
			Enumeration<URL> resources = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				try {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					if (POM_TITLE.equalsIgnoreCase(manifest.getMainAttributes().getValue("Specification-Title")))
						// check that this is your manifest and do what you need
						// or get the next one
						return manifest.getMainAttributes().getValue(key);
				} catch (IOException e) {
					LOG.warn(e);
				}
			}
		} catch (Exception e) {
			LOG.warn("Unable to read version from manifest", e);
		}
		return def;
	}

	private static void loadProperties() {
		PropertiesIO.setSavePath(propertiesPath);
		PropertiesIO.loadProperties();

	}

	public static void initColors() {
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
		if (style == null || style.isEmpty()) {
			initColors();
		}
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

	public static void setDebug(boolean value) {
		debug = value;

	}

	public static boolean isErrorReporting() {
		return noLog;
	}

	public static void setErrorReporting(boolean noLog) {
		setErrorReporting(noLog, true);
	}

	private static void setErrorReporting(boolean noLog, boolean save) {
		Main.noLog = noLog;
		if (!noLog) {
			LOG.info("Disabled external logging");
			log4jArgs[Constants.LOG4J_INDEX_REPORTING] = "false";

		} else {
			log4jArgs[Constants.LOG4J_INDEX_REPORTING] = "true";
		}
		if (save) {
			MainMapLookup.setMainArguments(log4jArgs);
		}
		PropertiesIO.setProperty(Constants.SETTING_ERROR_REPORTING, Boolean.toString(noLog));
	}

}
