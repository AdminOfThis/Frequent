package gui.pausable;

import java.util.ArrayList;

import data.Input;
import javafx.scene.Node;

public interface PausableView extends Pausable {

	public ArrayList<Node> getHeader();

	public void refresh();

	public void setSelectedChannel(Input in);
}
