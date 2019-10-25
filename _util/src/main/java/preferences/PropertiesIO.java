package preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

/**
 * 
 * @author AdminOfThis
 *
 */
public abstract class PropertiesIO {

	private static final Logger LOG = LogManager.getLogger(PropertiesIO.class);
	private static String savePath;
	private static Properties properties = new Properties();

	public static void setSavePath(String save) {
		savePath = save;
	}

	/**
	 * Saves the specified properties to a file
	 * 
	 * @param pref The properties to save
	 * @param file The file to which the properties get saved. Get's overwritten if
	 *             already existing
	 * @return true if saved successful, false otherwise
	 */
	public static boolean saveProperties(Properties pref, File file) {
		try {
			pref.store(new FileOutputStream(file), null);
			return true;
		} catch (IOException e) {
			LOG.warn("Saving the properties failed");
			return false;
		}
	}

	/**
	 * Tries to load properties from the specified file
	 * 
	 * @param file The file from which the properties should be loaded
	 *
	 */
	public static Properties loadProperties(File file) {
		if (file != null && file.exists() && file.isFile()) {

			try {
				Properties result = new Properties();
				result.load(new FileInputStream(file));
				properties = result;
				if (!properties.isEmpty()) {
					LOG.info(properties.size() + " Properties loaded");
				}
			} catch (IOException e) {
				properties = new Properties();
			}
		} else {
			properties = new Properties();
		}

		return properties;

	}

	public static Properties loadProperties() {
		File file = new File(savePath);
		return loadProperties(file);
	}

	public static Properties getProperties() {
		return properties;
	}

	public static String getProperty(String key) {
		if (properties.containsKey(key)) {
			return properties.get(key).toString();
		} else {
			return null;
		}
	}

	public static boolean getBooleanProperty(String key) {
		String value = getProperty(key);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return false;
	}

	public static void setProperty(String key, Object value) {
		setProperty(key, value, true);

	}

	public static void setProperty(String key, Object value, boolean save) {
		try {
			properties.put(key, value);
			if (save) {
				saveProperties();
			}
		} catch (Exception e) {
			LOG.warn("Problem saving properties", e);
		}
	}

	public static void saveProperties() {
		PropertiesIO.saveProperties(properties, new File(savePath));
	}

}
