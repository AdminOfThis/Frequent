package gui.utilities.controller;

import java.util.Objects;

import data.Input;
import javafx.geometry.Orientation;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VuMeterStereo extends AnchorPane {

	private Pane	root;
	private VuMeter	meter1, meter2;

	public VuMeterStereo(final Input channel1, final Input channel2, final Orientation o) {
		if (o == Orientation.HORIZONTAL) {
			root = new VBox();
		} else {
			root = new HBox();
		}
		getChildren().add(root);
		AnchorPane.setBottomAnchor(root, 0.0);
		AnchorPane.setTopAnchor(root, 0.0);
		AnchorPane.setLeftAnchor(root, 0.0);
		AnchorPane.setRightAnchor(root, 0.0);
		meter1 = new VuMeter(channel1, o);
		meter2 = new VuMeter(channel2, o);
		HBox.setHgrow(meter1, Priority.SOMETIMES);
		HBox.setHgrow(meter2, Priority.SOMETIMES);
		VBox.setVgrow(meter1, Priority.SOMETIMES);
		VBox.setVgrow(meter2, Priority.SOMETIMES);
		root.getChildren().addAll(meter1, meter2);
	}

	public void setChannel1(final Input channel) {
		meter1.setChannel(channel);
	}

	public void setChannel2(final Input channel) {
		meter2.setChannel(channel);
	}

	public void setChannels(final Input c1, final Input c2) {
		if (Objects.equals(c1, meter1.getInput())) {
			meter2.setChannel(c2);
		} else if (Objects.equals(c1, meter2.getInput())) {
			meter1.setChannel(c2);
		} else if (Objects.equals(c2, meter1.getInput())) {
			meter2.setChannel(c1);
		} else if (Objects.equals(c2, meter2.getInput())) {
			meter1.setChannel(c1);
		} else {
			meter1.setChannel(c1);
			meter2.setChannel(c2);
		}
	}
}
