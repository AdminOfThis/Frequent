package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import gui.pausable.PausableView;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.VuMeter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public class VectorScopeViewController implements Initializable, PausableView {

	private static final Logger	LOG		= Logger.getLogger(VectorScopeViewController.class);
	private boolean				pause	= false;
	@FXML
	private AnchorPane			chartPane;
	@FXML
	private ChoiceBox<Channel>	cmbChannel1, cmbChannel2;
	@FXML
	private HBox				box1, box2;
	@FXML
	private HBox				paneParent;
	@FXML
	private HBox				bottomPane;
	@FXML
	private Slider				zoomSlider, decaySlider;
	@FXML
	private HBox				boxZoom, boxDecay;
	private VectorScope			vectorScope;
	private VuMeter				vu1, vu2;
	private Channel				c1, c2;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		vectorScope = new VectorScope();
		vectorScope.setParentPausable(this);
		chartPane.getChildren().add(vectorScope);
		AnchorPane.setBottomAnchor(vectorScope, .0);
		AnchorPane.setTopAnchor(vectorScope, .0);
		AnchorPane.setLeftAnchor(vectorScope, .0);
		AnchorPane.setRightAnchor(vectorScope, .0);
		vu1 = new VuMeter(null, Orientation.HORIZONTAL);
		vu1.showLabels(false);
		vu1.setRotate(180.0);
		vu1.setParentPausable(this);
		bottomPane.getChildren().add(vu1);
		HBox.setHgrow(vu1, Priority.SOMETIMES);
		vu2 = new VuMeter(null, Orientation.HORIZONTAL);
		vu2.showLabels(false);
		vu2.setParentPausable(this);
		bottomPane.getChildren().add(vu2);
		HBox.setHgrow(vu2, Priority.SOMETIMES);
		zoomSlider.valueProperty().addListener(e -> vectorScope.setMax(zoomSlider.getValue()));
		decaySlider.valueProperty().addListener(e -> vectorScope.setDecay(decaySlider.getValue()));

		// updating channels
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		StringConverter<Channel> converter = new StringConverter<Channel>() {

			@Override
			public String toString(Channel object) {
				if (object == null) {
					LOG.info("");
					return "- NONE -";
				}
				return object.getName();
			}

			@Override
			public Channel fromString(String string) {
				return null;
			}
		};
		cmbChannel1.setConverter(converter);
		cmbChannel2.setConverter(converter);
		// adding listener
		cmbChannel1.valueProperty().addListener(e -> {
			Channel cNew = cmbChannel1.getValue();
			if (c1 != cNew) {
				c1 = cNew;
				vectorScope.setChannels(cNew, cmbChannel2.getValue());
				vu1.setChannel(cNew);
			}
		});
		cmbChannel2.valueProperty().addListener(e -> {
			Channel cNew = cmbChannel2.getValue();
			if (c2 != cNew) {
				c2 = cNew;
				vectorScope.setChannels(cmbChannel1.getValue(), cNew);
				vu2.setChannel(cNew);
			}
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
		ArrayList<Node> list = new ArrayList<>();

		list.add(box1);
		list.add(box2);
		// Pane p = new Pane();
		// HBox.setHgrow(p, Priority.ALWAYS);
		// list.add(p);
		list.add(boxZoom);
		list.add(boxDecay);
		return list;
	}

	@Override
	public void refresh() {
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel1.setValue(vectorScope.getChannel1());
			cmbChannel2.setValue(vectorScope.getChannel2());
		}
	}
}
