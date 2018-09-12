package gui.pausable;

import java.util.ArrayList;

import javafx.scene.Node;

public interface PausableView extends Pausable {

	public ArrayList<Node> getHeader();

	public void refresh();
}
