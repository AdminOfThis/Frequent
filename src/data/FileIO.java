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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.DataHolder;
import control.TimeKeeper;
import gui.controller.MainController;

public abstract class FileIO {

	private static final Logger			LOG			= Logger.getLogger(FileIO.class);
	public static final String			ENDING		= ".fre";
	// files
	private static File					currentDir	= new File(System.getProperty("user.home"));
	private static File					currentFile;
	private static List<DataHolder<?>>	holderList	= new ArrayList<>();

	public static void registerSaveData(DataHolder<?> holder) {
		if (!holderList.contains(holder)) {
			holderList.add(holder);
		}
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

	@SuppressWarnings("unchecked")
	private static List<Serializable> collectData() {
		ArrayList<Serializable> result = new ArrayList<>();
		for (DataHolder<?> h : holderList) {
			result.addAll((Collection<? extends Serializable>) h.getData());
		}
		return result;
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
}
