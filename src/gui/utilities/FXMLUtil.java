package gui.utilities;

import org.apache.log4j.Logger;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;

public abstract class FXMLUtil {

	private static final Logger		LOG	= Logger.getLogger(FXMLUtil.class);
	// private static final String GUI_FOLDER = "/gui/gui/";
	private static Initializable	controller;

	public static Parent loadFXML(final String string) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(FXMLUtil.class.getResource(string));
			parent = loader.load();
			controller = loader.getController();
		}
		catch (Exception e) {
			LOG.error("Unable to load FXMLFile");
			LOG.debug("", e);
		}
		return parent;
	}

	public static Initializable getController() {
		return controller;
	}
}
