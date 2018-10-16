package data;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.DataHolder;
import control.TimeKeeper;
import gui.controller.MainController;
import gui.controller.TimeKeeperController;

public abstract class FileIO {

	private static final String								PROPERTIES_FILE	= "./frequent.properties";
	private static final Logger								LOG				= Logger.getLogger(FileIO.class);
	public static final String								ENDING			= ".fre";
	// files
	private static File										currentDir		= new File(System.getProperty("user.home"));
	private static File										currentFile;
	private static List<DataHolder<? extends Serializable>>	holderList		= new ArrayList<>();
	private static Properties								properties;

	public static void registerSaveData(DataHolder<? extends Serializable> holder) {
		if (!holderList.contains(holder)) {
			holderList.add(holder);
		}
	}

	public static void writeProperties(String key, String value) {
		if (properties == null) {
			loadProperties();
		}
		properties.put(key, value);
		saveProperties();
	}

	public static String readPropertiesString(String key, String defaultValue) {
		if (properties == null) {
			loadProperties();
		}
		return properties.getProperty(key, defaultValue);
	}

	public static Properties loadProperties() {
		if (properties == null) {
			properties = new Properties();
		}
		try {
			File file = new File(PROPERTIES_FILE);
			if (file.exists()) {
				properties.load(new FileInputStream(file));
			}
		} catch (Exception e) {
			LOG.warn("Unable to load properties", e);
			LOG.debug("", e);
		}
		return properties;
	}

	private static boolean saveProperties() {
		try {
			properties.store(new FileOutputStream(new File(PROPERTIES_FILE)), "");
		} catch (Exception e) {
			LOG.warn("Unable to save preferences");
			LOG.debug("", e);
			return false;
		}
		return true;
	}

	public static boolean open(File file) {
		if (file != null) {
			if (!file.getName().endsWith(ENDING)) {
				LOG.warn("Unable to load file " + file.getName());
			} else {
				currentFile = file;
				List<Serializable> result = null;
				currentDir = file.getParentFile();
				LOG.info("Trying to open file " + file.getName());
				result = openFile(file);
				if (result != null && !result.isEmpty()) {
					for (DataHolder<?> h : holderList) {
						h.clear();
					}
					handleResult(result);
					MainController.getInstance().refresh();
					TimeKeeperController.getInstance().refresh();
					MainController.getInstance().setTitle(file.getName());
					return true;
				} else {
					LOG.warn("Nothing loaded");
				}
			}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void handleResult(List<Serializable> result) {
		ArrayList<Group> groupList = new ArrayList<>();
		HashMap<Class, Integer> counterMap = new HashMap<>();
		for (Object o : result) {
			if (counterMap.get(o.getClass()) == null) {
				counterMap.put(o.getClass(), 0);
			}
			counterMap.put(o.getClass(), counterMap.get(o.getClass()) + 1);
			// finding right controller
			DataHolder holder = null;
			if (o instanceof Cue) {
				holder = TimeKeeper.getInstance();
			} else if (o instanceof Channel) {
				holder = ASIOController.getInstance();
			} else if (o instanceof Group) {
				groupList.add((Group) o);
			}
			// adding
			if (holder != null) {
				holder.add(o);
			}
		}
		for (Group g : groupList) {
			if (ASIOController.getInstance() != null) {
				ASIOController.getInstance().add(g);
			}
		}
		LOG.info("= Loading statistics: ");
		for (Entry<Class, Integer> o : counterMap.entrySet()) {
			LOG.info("    " + o.getKey().getSimpleName() + ": " + o.getValue());
		}
	}

	private static List<Serializable> openFile(final File file) {
		ArrayList<Serializable> result = new ArrayList<>();
		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(file));
			Object o;
			while ((o = stream.readObject()) != null) {
				if (o instanceof Serializable) {
					result.add((Serializable) o);
				}
			}
		} catch (FileNotFoundException e) {
			LOG.warn("File not found");
			LOG.debug("", e);
		} catch (EOFException e) {
		} catch (IOException e) {
			LOG.warn("Unable to read file");
			LOG.debug("", e);
		} catch (ClassNotFoundException e) {
			LOG.warn("Class not found");
			LOG.debug("", e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				LOG.error("Unable to close file stream");
				LOG.debug("", e);
			}
		}
		LOG.info("Successfully loaded " + result.size() + " object(s)");
		return result;
	}

	public static File getCurrentDir() {
		return currentDir;
	}

	public static File getCurrentFile() {
		return currentFile;
	}

	private static List<Serializable> collectData() {
		ArrayList<Serializable> result = new ArrayList<>();
		for (DataHolder<? extends Serializable> h : holderList) {
			result.addAll(h.getData());
		}
		return result;
	}

	public static boolean unsavedChanges() {
		List<Serializable> newResult = collectData();
		if (!newResult.isEmpty()) {
			if (currentFile == null || !currentFile.exists()) {
				LOG.info("Program has not yet saved");
				return true;
			}
			List<Serializable> saveFile = openFile(currentFile);
			if (!newResult.equals(saveFile)) {
				LOG.info("Program has unsaved changes");
				return true;
			}
		}
		return false;
	}

	public static boolean save(File file) {
		return save(collectData(), file);
	}

	public static boolean save(List<Serializable> objects, File file) {
		LOG.info("Saving " + objects.size() + " object(s) to " + file.getPath());
		currentDir = file.getParentFile();
		currentFile = file;
		if (!currentFile.getName().endsWith(ENDING)) {
			currentFile = new File(currentFile.getPath() + ENDING);
		}
		boolean result = true;
		ObjectOutputStream stream = null;
		try {
			stream = new ObjectOutputStream(new FileOutputStream(file));
			for (Serializable o : objects) {
				stream.writeObject(o);
			}
		} catch (IOException e) {
			LOG.warn("Unable to write file");
			LOG.debug("", e);
			result = false;
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				LOG.error("Unable to close file stream");
				LOG.debug("", e);
			}
		}
		LOG.info("Successful saved");
		return result;
	}

	public static void setCurrentDir(File file) {
		currentDir = file;
	}

	public static boolean compareAndNullCheck(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}
}
