package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import control.ASIOController;
import data.Channel;
import gui.pausable.PausableView;
import gui.utilities.AutoCompleteComboBoxListener;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.VuMeter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public class VectorScopeViewController implements Initializable, PausableView {

	// private static final Logger LOG =
	// Logger.getLogger(VectorScopeViewController.class);
	private boolean				pause	= false;
	@FXML
	private AnchorPane			chartPane;
	@FXML
	private ComboBox<Channel>	cmbChannel1, cmbChannel2;
	@FXML
	private HBox				box1, box2;
	@FXML
	private HBox				paneParent;
	@FXML
	private HBox				bottomPane;
	@FXML
	private Slider				decaySlider;
	@FXML
	private HBox				boxDecay;
	private VectorScope			vectorScope;
	private VuMeter				vu1, vu2;
	private Channel				c1, c2;

	@Override
	public ArrayList<Node> getHeader() {
		ArrayList<Node> list = new ArrayList<>();
		list.add(box1);
		list.add(box2);
		// Pane p = new Pane();
		// HBox.setHgrow(p, Priority.ALWAYS);
		// list.add(p);
		list.add(boxDecay);
		return list;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		vectorScope = new VectorScope();
		vectorScope.setParentPausable(this);
		chartPane.getChildren().add(vectorScope);
		AnchorPane.setBottomAnchor(vectorScope, .0);
		AnchorPane.setTopAnchor(vectorScope, .0);
		AnchorPane.setLeftAnchor(vectorScope, .0);
		AnchorPane.setRightAnchor(vectorScope, .0);
		vu1 = new VuMeter(null, Orientation.HORIZONTAL);
		vu1.setRotate(180.0);
		vu1.showLabels(false);
		vu1.setParentPausable(this);
		bottomPane.getChildren().add(vu1);
		HBox.setHgrow(vu1, Priority.SOMETIMES);
		vu2 = new VuMeter(null, Orientation.HORIZONTAL);
		vu2.showLabels(false);
		vu2.setParentPausable(this);
		bottomPane.getChildren().add(vu2);
		HBox.setHgrow(vu2, Priority.SOMETIMES);
		decaySlider.valueProperty().addListener(e -> vectorScope.setDecay(decaySlider.getValue()));
		// updating channels
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		}
		StringConverter<Channel> converter = new StringConverter<Channel>() {

			@Override
			public Channel fromString(final String string) {
				for (Channel c : cmbChannel1.getItems()) {
					if (Objects.equals(c.getName(), string))
						return c;
				}
				for (Channel c : cmbChannel2.getItems()) {
					if (Objects.equals(c.getName(), string))
						return c;
				}
				return null;
			}

			@Override
			public String toString(final Channel object) {
				if (object == null)
					return "- NONE -";
				return object.getName();
			}
		};
		cmbChannel1.setConverter(converter);
		cmbChannel2.setConverter(converter);
		// adding listener
		cmbChannel1.valueProperty().addListener((obs, old, newV) -> {
			if (!Objects.equals(c1, newV)) {
				c1 = newV;
				vectorScope.setChannels(newV, cmbChannel2.getValue());
				vu1.setChannel(newV);
			}
		});
		cmbChannel2.valueProperty().addListener((obs, old, newV) -> {
			if (!Objects.equals(c2, newV)) {
				c2 = newV;
				vectorScope.setChannels(cmbChannel1.getValue(), newV);
				vu2.setChannel(newV);
			}
		});
		// AUTOCOmplete
		new AutoCompleteComboBoxListener<>(cmbChannel1);
		new AutoCompleteComboBoxListener<>(cmbChannel2);
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void pause(final boolean pause) {
		this.pause = pause;
	}

	@Override
	public void refresh() {
		if (ASIOController.getInstance() != null) {
			cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
			cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
			if (Objects.equals(vectorScope.getChannel1(), cmbChannel1.getValue())) {
				cmbChannel1.setValue(vectorScope.getChannel1());
			}
			if (Objects.equals(vectorScope.getChannel2(), cmbChannel2.getValue())) {
				cmbChannel2.setValue(vectorScope.getChannel2());
			}
		}
	}
}
