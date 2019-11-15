package gui.pausable;

import java.util.ArrayList;

import data.Input;
import javafx.scene.layout.Region;

public interface PausableView extends Pausable {

	public ArrayList<Region> getHeader();

	public void refresh();

	public void setSelectedChannel(Input in);
}
