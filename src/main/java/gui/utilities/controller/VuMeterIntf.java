package gui.utilities.controller;

import data.Input;
import gui.pausable.PausableComponent;

public interface VuMeterIntf extends PausableComponent {

	public void setChannel(final Input c);

	public void setTitle(String title);
}
