package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import gui.pausable.PausableView;
import gui.utilities.controller.VectorScope;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.AnchorPane;

public class VectorScopeViewController implements Initializable, PausableView {

	private static final Logger	LOG		= Logger.getLogger(VectorScopeViewController.class);
	private boolean				pause	= false;
	@FXML
	private AnchorPane			chartPane;
	@FXML
	private ChoiceBox<Channel>	cmbChannel1, cmbChannel2;
	@FXML
	private AnchorPane			chartChannel1, chartChannel2;
	private VectorScope			vectorScope;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		vectorScope = new VectorScope();
		vectorScope.setParentPausable(this);
		chartPane.getChildren().add(vectorScope);
		AnchorPane.setBottomAnchor(vectorScope, .0);
		AnchorPane.setTopAnchor(vectorScope, .0);
		AnchorPane.setLeftAnchor(vectorScope, .0);
		AnchorPane.setRightAnchor(vectorScope, .0);
		// updating channels
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		// adding listener
		ChangeListener<Channel> changeListener = new ChangeListener<Channel>() {

			@Override
			public void changed(ObservableValue<? extends Channel> observable, Channel oldValue, Channel newValue) {
				vectorScope.setChannels(cmbChannel1.getValue(), cmbChannel2.getValue());
			}
		};
		cmbChannel1.valueProperty().addListener(changeListener);
		cmbChannel2.valueProperty().addListener(changeListener);
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

	@Override
	public void refresh() {}
}
