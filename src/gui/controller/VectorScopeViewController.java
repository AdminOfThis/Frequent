package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import gui.pausable.PausableView;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.WaveFormChart;
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
	private WaveFormChart		chart1, chart2;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		vectorScope = new VectorScope();
		vectorScope.setParentPausable(this);
		chartPane.getChildren().add(vectorScope);
		AnchorPane.setBottomAnchor(vectorScope, .0);
		AnchorPane.setTopAnchor(vectorScope, .0);
		AnchorPane.setLeftAnchor(vectorScope, .0);
		AnchorPane.setRightAnchor(vectorScope, .0);
		chart1 = new WaveFormChart();
		chart1.setParentPausable(this);
		chartChannel1.getChildren().add(chart1);
		AnchorPane.setBottomAnchor(chart1, .0);
		AnchorPane.setTopAnchor(chart1, .0);
		AnchorPane.setLeftAnchor(chart1, .0);
		AnchorPane.setRightAnchor(chart1, .0);
		chart2 = new WaveFormChart();
		chart2.setParentPausable(this);
		chartChannel2.getChildren().add(chart2);
		AnchorPane.setBottomAnchor(chart2, .0);
		AnchorPane.setTopAnchor(chart2, .0);
		AnchorPane.setLeftAnchor(chart2, .0);
		AnchorPane.setRightAnchor(chart2, .0);
		// updating channels
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		// adding listener
		cmbChannel1.valueProperty().addListener(e -> {
			vectorScope.setChannels(cmbChannel1.getValue(), cmbChannel2.getValue());
			chart1.setChannel(cmbChannel1.getValue());
		});
		cmbChannel2.valueProperty().addListener(e -> {
			vectorScope.setChannels(cmbChannel1.getValue(), cmbChannel2.getValue());
			chart2.setChannel(cmbChannel2.getValue());
		});
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
