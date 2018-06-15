package gui.utilities;

import control.ASIOController;
import data.Channel;
import data.Group;
import gui.controller.GroupController;
import gui.controller.MainController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;

public class ChannelCellContextMenu extends InputCellContextMenu {

	private MenuItem resetName = new MenuItem("Reset Name");

	public ChannelCellContextMenu(Channel in) {
		super(in);
		if (in != null) {
			Channel channel = (Channel) in;
			resetName.setOnAction(e -> in.setName(((Channel) in).getChannel().getChannelName()));
			getItems().add(1, resetName);

			Menu groupMenu = new Menu("Groups");

			// groups
			getItems().add(groupMenu);

			ToggleGroup toggle = new ToggleGroup();
			for (Group g : ASIOController.getInstance().getGroupList()) {
				RadioMenuItem groupMenuItem = new RadioMenuItem(g.getName());
				groupMenuItem.setToggleGroup(toggle);
				groupMenuItem.setSelected(g.getChannelList().contains(channel));
				groupMenuItem.selectedProperty().addListener(new ChangeListener<Boolean>() {

					@Override
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (channel != null && channel instanceof Channel) {
							if (newValue) {
								g.addChannel(channel);
							} else {
								g.removeChannel(channel);
							}
							MainController.getInstance().refresh();
							GroupController.getInstance().refresh();
						}
					}
				});
				groupMenu.getItems().add(groupMenuItem);
			}

		}
	}
}
