package gui.utilities;

import data.Group;
import gui.controller.GroupController;
import javafx.scene.control.MenuItem;

public class GroupCellContextMenu extends InputCellContextMenu {

	private MenuItem deleteGroup = new MenuItem("Delete Group");

	public GroupCellContextMenu(Group in) {

		super(in);
		if (in != null) {
			Group group = (Group) in;
			deleteGroup.setOnAction(e -> {
				group.delete();
				GroupController.getInstance().refresh();
			});
			getItems().add(deleteGroup);
		}
	}

}
