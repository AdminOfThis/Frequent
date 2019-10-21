package gui.utilities.controller;

import java.util.Objects;

import data.Channel;
import data.Input;
import gui.pausable.Pausable;
import javafx.geometry.Orientation;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VuMeterStereo extends AnchorPane implements VuMeterIntf {

	private Pane		root;
	private VuMeterMono	meter1, meter2;
	private Pausable	parentPausable;
	private boolean		pause;

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
		meter1 = new VuMeterMono(channel1, o);
		meter2 = new VuMeterMono(channel2, o);
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

	@Override
	public void setParentPausable(final Pausable parent) {
		parentPausable = parent;
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause || parentPausable != null && parentPausable.isPaused();
	}

	@Override
	public void setChannel(Input c) {
		meter1.setChannel(c);
		if (((Channel) c).getStereoChannel() != null) {
			meter2.setChannel(((Channel) c).getStereoChannel());
		}
	}

	@Override
	public void setTitle(String title) {
		// TODO Auto-generated method stub
	}
}
