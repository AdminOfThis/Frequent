package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import gui.controller.DataHolder;
import gui.controller.MainController;

public abstract class FileIO {

	private static final Logger	LOG			= Logger.getLogger(FileIO.class);

	public static final String	CUE_ENDING	= ".cue";


	// files
	private static File			currentDir	= new File(System.getProperty("user.home"));

	public static void open(File file) {
		if (file != null) {
			List<Serializable> result = null;
			currentDir = file.getParentFile();
			LOG.info("Trying to open file " + file.getName());
			String ending = file.getName().substring(file.getName().lastIndexOf("."));
			switch (ending) {
			case CUE_ENDING:
				result = openFile(file);
			}
			if (result != null && !result.isEmpty()) {
				handleResult(result);
			} else {
				LOG.warn("Nothing loaded");
			}
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void handleResult(List<Serializable> result) {
		for (Object o : result) {

			// finding right controller
			DataHolder holder = null;
			if (o instanceof Cue) {
				holder = MainController.getInstance().getTimeKeeperController();
			} else if (o instanceof Channel) {
				holder = MainController.getInstance();
			}

			// adding
			if (holder != null) {
				holder.add(o);
			}
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

	public static boolean save(List<Serializable> objects, File file) {
		boolean result = true;
		ObjectOutputStream stream = null;
		try {
			stream = new ObjectOutputStream(new FileOutputStream(file));
			for (Serializable o : objects) {
				stream.writeObject(o);
			}
		} catch (IOException e) {
			LOG.warn("Unable to read file");
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
		return result;


	}

}
