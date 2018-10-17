package gui.utilities.controller;

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


	public VuMeterStereo(Input channel1, Input channel2, Orientation o) {
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

	public void setChannel1(Input channel) {
		setChannel(meter1, channel);
	}

	public void setChannel2(Input channel) {
		setChannel(meter2, channel);
	}

	private void setChannel(VuMeter meter, Input in) {
		meter.setChannel(in);
	}
}
