package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import control.ASIOController;
import data.Channel;
import data.Group;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.AutoCompleteComboBoxListener;
import gui.utilities.controller.VectorScope;
import gui.utilities.controller.VuMeterMono;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import main.Constants;

public class VectorScopeViewController implements Initializable, PausableView {

	// private static final Logger LOG =
	// Logger.getLogger(VectorScopeViewController.class);
	private boolean pause = true;
	@FXML
	private AnchorPane chartPane;
	@FXML
	private ComboBox<Channel> cmbChannel1, cmbChannel2;
	@FXML
	private HBox box1, box2;
	@FXML
	private HBox bottomPane;
	@FXML
	private Slider decaySlider;
	@FXML
	private HBox boxDecay;
	private VectorScope vectorScope;
	private VuMeterMono vu1, vu2;
//	private Channel c1, c2;

	@Override
	public ArrayList<Region> getHeader() {
		ArrayList<Region> list = new ArrayList<>();
		list.add(box1);
		list.add(box2);
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
		vu1 = new VuMeterMono(null, Orientation.HORIZONTAL);
		vu1.setRotate(180.0);
		vu1.setParentPausable(this);
		bottomPane.getChildren().add(vu1);
		HBox.setHgrow(vu1, Priority.SOMETIMES);
		vu2 = new VuMeterMono(null, Orientation.HORIZONTAL);
		vu2.setParentPausable(this);
		bottomPane.getChildren().add(vu2);
		HBox.setHgrow(vu2, Priority.SOMETIMES);
		decaySlider.valueProperty().addListener(e -> vectorScope.setDecay(decaySlider.getValue()));
		// updating channels
		cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
		cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		cmbChannel1.setConverter(Constants.CHANNEL_CONVERTER);
		cmbChannel2.setConverter(Constants.CHANNEL_CONVERTER);
		// adding listener
		cmbChannel1.valueProperty().addListener((obs, old, newV) -> {
			if (!Objects.equals(old, newV)) {
				vectorScope.setChannels(newV, cmbChannel2.getValue());
				vu1.setChannel(newV);
			}
		});
		cmbChannel2.valueProperty().addListener((obs, old, newV) -> {
			if (!Objects.equals(old, newV)) {
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
		refreshComboBoxes();
		vectorScope.setDecay(decaySlider.getValue());
		if (MainController.getInstance().getSelectedChannels().size() == 2) {
			cmbChannel1.setValue((Channel) MainController.getInstance().getSelectedChannels().get(0));
			cmbChannel2.setValue((Channel) MainController.getInstance().getSelectedChannels().get(1));
		}
	}

	private void refreshComboBoxes() {
		// refreshing combo boxes
		cmbChannel1.getItems().setAll(ASIOController.getInstance().getInputList());
		cmbChannel2.getItems().setAll(ASIOController.getInstance().getInputList());
		if (Objects.equals(vectorScope.getChannel1(), cmbChannel1.getValue())) {
			cmbChannel1.setValue(vectorScope.getChannel1());
		}
		if (Objects.equals(vectorScope.getChannel2(), cmbChannel2.getValue())) {
			cmbChannel2.setValue(vectorScope.getChannel2());
		}
	}

	@Override
	public void setSelectedChannel(final Input in) {
		if (MainController.getInstance().getSelectedChannels().size() >= 1) {
			for (Input i : MainController.getInstance().getSelectedChannels()) {
				if (i instanceof Channel && ((Channel) i).getStereoChannel() != null) {
					cmbChannel1.setValue((Channel) i);
					cmbChannel2.setValue(((Channel) i).getStereoChannel());
				}
			}
		} else if (cmbChannel1.getValue() == null && cmbChannel2.getValue() == null && MainController.getInstance().getSelectedChannels().size() == 2) {
			for (Input i : MainController.getInstance().getSelectedChannels()) {
				if (i instanceof Group) {
					return;
				}
			}
			cmbChannel1.setValue((Channel) MainController.getInstance().getSelectedChannels().get(0));
			cmbChannel2.setValue((Channel) MainController.getInstance().getSelectedChannels().get(1));
		}
	}
}
