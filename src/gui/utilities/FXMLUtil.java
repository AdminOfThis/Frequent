package gui.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;

public abstract class FXMLUtil {

	private static final Logger		LOG			= Logger.getLogger(FXMLUtil.class);
	private static final String		STYLE_SHEET	= "/gui/style.css";
	private static Initializable	controller;

	public static Parent loadFXML(final String string) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(FXMLUtil.class.getResource(string));
			parent = loader.load();
			controller = loader.getController();
		} catch (Exception e) {
			LOG.error("Unable to load FXMLFile");
			LOG.info("", e);
		}
		return parent;
	}

	public static Parent loadFXML(final String string, Initializable controller) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(FXMLUtil.class.getResource(string));
			loader.setController(controller);
			parent = loader.load();
			controller = loader.getController();
		} catch (Exception e) {
			LOG.error("Unable to load FXMLFile");
			LOG.debug("", e);
		}
		return parent;
	}

	public static Initializable getController() {
		return controller;
	}

	public static String getStyleValue(String value) {
		String result = "";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(FXMLUtil.class.getResourceAsStream(STYLE_SHEET)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(value)) {
					result = line.split(":")[1];
					break;
				}
			}

		} catch (Exception e) {
			LOG.warn("Unable to load css value " + value);
			LOG.debug("", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.error("Problem closing file reader", e);
				}
			}
		}
		if (result.endsWith(";")) {
			result = result.substring(0, result.length() - 1);
		}
		return result.trim();
	}
}
