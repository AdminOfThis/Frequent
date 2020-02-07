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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.DataHolder;
import control.TimeKeeper;
import gui.controller.MainController;
import gui.controller.TimeKeeperController;

public abstract class FileIO {
	private static final Logger LOG = LogManager.getLogger(FileIO.class);
	public static final String ENDING = ".fre";
	// files
	private static File currentDir = new File(System.getProperty("user.home"));
	private static File currentFile;
	private static List<DataHolder<? extends Serializable>> holderList = new ArrayList<>();

	public static File getCurrentDir() {
		return currentDir;
	}

	public static File getCurrentFile() {
		return currentFile;
	}

	public static boolean open(final File file) {
		if (file != null) {
			if (!file.getName().endsWith(ENDING)) {
				LOG.warn("Unable to load file " + file.getName());
			} else {
				try {
					currentFile = file;
					List<Serializable> result = null;
					currentDir = file.getParentFile();
					LOG.info("Trying to open file " + file.getName());
					result = openFile(file);
					if (result != null && !result.isEmpty()) {
						for (DataHolder<?> h : holderList) {
							if (h != null) {
								h.clear();
							}
						}
						handleResult(result);
						MainController.getInstance().refresh();
						TimeKeeperController.getInstance().refresh();
						MainController.getInstance().setTitle(file.getName());
						return true;
					} else {
						LOG.warn("Nothing loaded");
					}
				} catch (Exception e) {
					LOG.error("Unable to open file", e);
				}
			}
		}
		return false;
	}

	public static void registerDatahandlers() {
		registerSaveData(ASIOController.getInstance());
		registerSaveData(TimeKeeper.getInstance());
		registerSaveData(ColorController.getInstance());
	}

	public static void registerSaveData(final DataHolder<? extends Serializable> holder) {
		if (!holderList.contains(holder)) {
			holderList.add(holder);
		}
	}

	public static boolean save(final File file) {
		return save(collectData(), file);
	}

	public static boolean save(final List<Serializable> objects, final File file) {
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

	public static void setCurrentDir(final File file) {
		currentDir = file;
	}

	public static void unregisterSaveData(DataHolder<?> holder) {
		holderList.remove(holder);
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

	private static List<Serializable> collectData() {
		ArrayList<Serializable> result = new ArrayList<>();
		for (DataHolder<? extends Serializable> h : holderList) {
			if (h != null) {
				result.addAll(h.getData());
			}
		}
		return result;
	}

	private static void handleResult(final List<Serializable> result) {
		// Map only for statistics
		HashMap<Class<?>, Integer> counterMap = new HashMap<>();
		for (Object o : result) {
			try {
				if (counterMap.get(o.getClass()) == null) {
					counterMap.put(o.getClass(), 0);
				}
				counterMap.put(o.getClass(), counterMap.get(o.getClass()) + 1);
				// finding right controller
				for (DataHolder<? extends Serializable> holder : holderList) {
					if (holder != null) {
						holder.add(o);
					}
				}
			} catch (Exception e) {
				LOG.warn("Problem loading object", e);
			}
		}
		try {
			LOG.info("= Loading statistics: ");
			for (Entry<Class<?>, Integer> o : counterMap.entrySet()) {
				LOG.info("    " + o.getKey().getSimpleName() + ": " + o.getValue());
			}
		} catch (Exception e) {
			LOG.warn("Problem while showing statistics", e);
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
			LOG.warn("End of File reached");
			LOG.debug("", e);
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

}
