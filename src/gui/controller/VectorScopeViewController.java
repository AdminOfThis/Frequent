package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import gui.pausable.PausableView;
import gui.utilities.controller.VectorScope;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class VectorScopeViewController implements Initializable, PausableView {

	private static final Logger	LOG		= Logger.getLogger(VectorScopeViewController.class);
	private boolean				pause	= false;
	@FXML
	private BorderPane			root;
	private VectorScope			vectorScope;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		vectorScope = new VectorScope();
		vectorScope.setParentPausable(this);
		root.setCenter(vectorScope);
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public ArrayList<Node> getHeader() {
		return null;
	}
}
