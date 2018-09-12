package gui.gui;

import gui.pausable.Pausable;

public interface PausableComponent extends Pausable {

	public void setParentPausable(Pausable parent);
}
