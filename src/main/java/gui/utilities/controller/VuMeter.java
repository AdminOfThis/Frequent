package gui.utilities.controller;

import data.Input;
import gui.pausable.PausableComponent;
import javafx.scene.layout.AnchorPane;

public abstract class VuMeter extends AnchorPane implements PausableComponent {

	public abstract void setChannel(final Input c);

	public abstract void setTitle(String title);
}
