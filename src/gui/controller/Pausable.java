package gui.controller;

public interface Pausable {

	public void pause(boolean pause);

	public boolean isPaused();

	public void setParentPausable(Pausable parent);
}
