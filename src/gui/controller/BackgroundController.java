package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import gui.utilities.FXMLUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.TilePane;

public class BackgroundController implements Initializable {

	private static final Logger	LOG						= Logger.getLogger(BackgroundController.class);

	private static final String	BACKGROUND_ITEM_PATH	= "./../utilities/gui/BackgroundItem.fxml";

	@FXML
	private TilePane			flow;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.info("Loading Backgrond");
		for (int i = 0; i < 63; i++) {
			Parent p = FXMLUtil.loadFXML(BACKGROUND_ITEM_PATH);
			flow.getChildren().add(p);
		}
	}

}
