package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import control.ASIOController;
import data.Group;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GroupController implements Initializable {

	@FXML
	private VBox					root;

	private static GroupController	instance;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
	}

	public static GroupController getInstance() {
		return instance;
	}

	public void refresh() {
		if (ASIOController.getInstance() != null) {
			for (Group g : ASIOController.getInstance().getGroupList()) {
				root.getChildren().add(new Label(g.getName()));
			}
		}
	}

}
