package gui.utilities;


import java.util.Optional;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Group;
import gui.controller.MainController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;

public class ChannelCellContextMenu extends InputCellContextMenu {

	private static final Logger	LOG			= Logger.getLogger(ChannelCellContextMenu.class);

	private MenuItem			resetName	= new MenuItem("Reset Name");
	private CheckMenuItem		hide		= new CheckMenuItem("Hide");

	private CheckMenuItem		showHidden	= new CheckMenuItem("Show Hidden");
	private MenuItem			newGroup	= new MenuItem("New Group");

	private Menu				groupMenu	= new Menu("Groups");

	public ChannelCellContextMenu(Channel in) {
		super(in);
		if (in != null) {
			resetName.setOnAction(e -> {
				in.setName(in.getChannel().getChannelName());
			});
			hide.setOnAction(e -> MainController.getInstance().hideAllSelected());
			showHidden.setOnAction(e -> MainController.getInstance().setShowHidden(showHidden.isSelected()));
			getItems().add(resetName);
			getItems().add(new SeparatorMenuItem());
			getItems().add(hide);
			getItems().add(showHidden);
			getItems().add(new SeparatorMenuItem());
			// groups
			getItems().add(groupMenu);
			groupMenu.getItems().add(newGroup);
			groupMenu.getItems().add(new SeparatorMenuItem());
			setOnShowing(e -> refreshData(in));
			setAutoHide(true);
			newGroup.setOnAction(e -> {
				TextInputDialog newGroupDialog = new TextInputDialog();
				Optional<String> result = newGroupDialog.showAndWait();
				if (result.isPresent()) {
					Group g = new Group(result.get());
					LOG.info("Created new group: " + g.getName());
					ASIOController.getInstance().addGroup(g);
					if (in != null && in instanceof Channel) {
						g.addChannel((Channel) in);
					}
					MainController.getInstance().refresh();
				}
			});
		}
	}

	private void refreshData(Channel channel) {
		showHidden.setSelected(MainController.getInstance().isShowHidden());
		groupMenu.getItems().clear();

		ToggleGroup toggle = new ToggleGroup();
		for (Group g : ASIOController.getInstance().getGroupList()) {
			RadioMenuItem groupMenuItem = new RadioMenuItem(g.getName());
			toggle.getToggles().add(groupMenuItem);
			groupMenuItem.setSelected(g.getChannelList().contains(channel));
			groupMenuItem.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						g.addChannel(channel);
					} else {
						g.removeChannel(channel);
					}
					MainController.getInstance().refresh();
				}
			});
			groupMenu.getItems().add(groupMenuItem);
		}
	}
}
