package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import data.FileIO;
import gui.FXMLUtil;
import gui.preloader.PreLoader;
import javafx.application.Application;
import preferences.PropertiesIO;

/**
 * 
 * @author AdminOfThis
 *
 */
public class Main {
	private static final String DEFAULT_PROPERTIES_PATH = "./settings.conf";
	public static final String LOCALIZATION_FILES = "loc.Strings";

	private static final Logger LOG = LogManager.getLogger(Main.class);

	private static final String POM_TITLE = "Frequent";
	public static final String VERSION_KEY = "Implementation-Version";
	public static final String TITLE_KEY = "Implementation-Title";

	private static String title = "";
	private static String version = "";

	private static String color_accent = "#5EBF23";
	private static String color_base = "#1A1A1A";
	private static String color_focus = "#7DFF2F";
	private static String style = "";
	private static String propertiesPath = DEFAULT_PROPERTIES_PATH;
	private static String[] log4jArgs = new String[3];
	private static boolean externalLog = true;
	private static boolean development = false;
	private static boolean debug = false;
	private static Locale language;
	private static boolean initialized = false;

	/**
	 * The main method of the programm. Starts with parsing arguments, then launches
	 * the GUI
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			long timeStart = System.currentTimeMillis();
			Thread.setDefaultUncaughtExceptionHandler(Constants.EMERGENCY_EXCEPTION_HANDLER);
			initTitle();
			LOG.info(" === " + getReadableTitle() + " ===");
			if (parseArgs(args)) {

				if (initialize()) {

					if (checkIfStart()) {
						long timeDone = System.currentTimeMillis();

						LOG.info("Time until preloader: " + (timeDone - timeStart) + " ms");
						System.setProperty("javafx.preloader", PreLoader.class.getName());
						Application.launch(FXMLMain.class, args);
					} else {
						LOG.info("Application is already running, unable to run multiple instances");
						FXMLMain.showAlreadyRunningDialog();
					}
				} else {
					LOG.warn("Unable to initialize Application");
				}
			}
		} catch (Exception exception) {
			LOG.fatal("Fatal uncaught exception: ", exception);
		} catch (Error error) {
			LOG.fatal("Fatal uncaught error: ", error);
		}
	}

	protected static boolean initialize() {
		boolean result = true;
		initTitle();
		result = result && initLocalization();
		loadProperties();
		initColors();
		initLog4jParams();

		initialized = true;
		return result;
	}

	private static boolean initLocalization() {
		boolean result = false;
		try {
			if (language == null) {
				language = Locale.getDefault();
				LOG.info("No language preference set, using default: \"" + language.getCountry() + "\"");
			}
			LOG.info("Trying to load Localization for: \"" + language.getLanguage() + "\"");
			ResourceBundle bundle = ResourceBundle.getBundle(LOCALIZATION_FILES, language);

			if (bundle != null) {
				if (Objects.equals(language.getLanguage(), bundle.getLocale().getLanguage())) {
					LOG.info("Loaded Language \"" + bundle.getLocale().getLanguage() + "\"");
				} else {
					Locale defaultLang = Locale.ENGLISH;
					LOG.info("Unable to load localization, loading default (" + defaultLang.getLanguage() + ") instead");
					bundle = ResourceBundle.getBundle(LOCALIZATION_FILES, defaultLang);
				}
				FXMLUtil.setResourceBundle(bundle);
			}
			result = true;
		} catch (Exception e) {
			LOG.fatal("Unable to load resource bundle", e);
		}
		return result;
	}

	public static boolean isInitialized() {
		return initialized;
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

	public static String getOnlyTitle() {
		if (title == null || title.isEmpty()) {
			return "";
		}
		return title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
	}

	public static String getReadableTitle() {
		return getOnlyTitle() + " " + getVersion();
	}

	public static String getStyle() {
		if (style == null || style.isEmpty()) {
			initColors();
		}
		return style;
	}

	public static String getVersion() {
		return version;
	}

	public static void initColors() {
		style = "-fx-base:" + color_base + "; -fx-accent:" + color_accent + "; -fx-focus-color:" + color_focus + "; ";
		FXMLUtil.setDefaultStyle(style);
	}

	public static void initTitle() {
		title = getFromManifest(TITLE_KEY, POM_TITLE);
		version = getFromManifest(VERSION_KEY, "Local Build");
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isErrorReporting() {
		return externalLog;
	}

	public static void setDebug(boolean value) {
		debug = value;

	}

	public static void setErrorReporting(boolean noLog) {
		setErrorReporting(noLog, true);
	}

	private static boolean checkIfStart() {
		boolean alreadyRunning = MainUtil.createRunningLockFile(Constants.LOCK_FILE);
		if (Main.isDevelopment()) {
			// continue to launch either way
			LOG.info("Already an instance running, starting anyway because of development mode");
			return true;
		} else {
			return alreadyRunning;
		}
	}

	private static void initLog4jParams() {

		setErrorReporting(!development && PropertiesIO.getBooleanProperty(Constants.SETTING_ERROR_REPORTING, true), false);
		log4jArgs[Constants.LOG4J_INDEX_VERSION] = version;
		if (development) {
			LOG.info("Set environment to development");
			LOG.info("Disabling external error logging due to development mode");
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

	private static boolean isDevelopment() {
		return development;
	}

	private static void loadProperties() {
		PropertiesIO.setSavePath(propertiesPath);
		PropertiesIO.loadProperties();

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
			} else if (arg.equalsIgnoreCase("-nolog")) {
				externalLog = false;
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
				parseStyle(arg, args);
			} else if (arg.toLowerCase().startsWith("-lang=") || arg.toLowerCase().startsWith("-language=")) {
				parseLanguage(arg);
			}
		}
		return result;
	}

	private static void parseLanguage(String arg) {
		try {
			String value = arg.split("=")[1];
			Locale loc = Locale.forLanguageTag(value);
			if (loc != null) {
				language = loc;
				LOG.info("Set language to \"" + language.getLanguage() + "\"");
			}
		} catch (Exception e) {
			LOG.warn("Unable to load Language: \"" + arg + "\"");
		}

	}

	private static void parseStyle(String arg, String[] args) {
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

	private static void setErrorReporting(boolean noLog, boolean save) {
		externalLog = noLog;
		if (!noLog) {
			LOG.info("Disabled external logging");
			log4jArgs[Constants.LOG4J_INDEX_REPORTING] = Boolean.toString(false);

		} else {
			LOG.info("Enabled external logging");
			log4jArgs[Constants.LOG4J_INDEX_REPORTING] = Boolean.toString(true);
		}
		if (save) {
			MainMapLookup.setMainArguments(log4jArgs);
		}
		PropertiesIO.setProperty(Constants.SETTING_ERROR_REPORTING, Boolean.toString(noLog));
	}

	public String getPOMTitle() {
		return POM_TITLE;
	}

	public String getTitle() {
		return title;
	}

}
