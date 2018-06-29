package gui.utilities;

import control.ASIOController;
import data.Channel;
import data.Group;
import gui.controller.GroupController;
import gui.controller.MainController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;

public class ChannelCellContextMenu extends InputCellContextMenu {

	private MenuItem		resetName	= new MenuItem("Reset Name");
	private CheckMenuItem	hide		= new CheckMenuItem("Hide");
	private CheckMenuItem	showHidden	= new CheckMenuItem("Show Hidden");
	private Menu			groupMenu	= new Menu("Groups");


	public ChannelCellContextMenu(Channel in) {
		super(in);
		if (in != null) {
			resetName.setOnAction(e -> in.setName(((Channel) in).getChannel().getChannelName()));
			hide.setOnAction(e -> MainController.getInstance().hideAllSelected());
			showHidden.setOnAction(e -> MainController.getInstance().setShowHidden(showHidden.isSelected()));

			getItems().add(resetName);
			getItems().add(new SeparatorMenuItem());
			getItems().add(hide);
			getItems().add(showHidden);
			getItems().add(new SeparatorMenuItem());
			// groups
			getItems().add(groupMenu);
			setOnShowing(e -> refreshData(in));
		}
	}

	private void refreshData(Channel channel) {

		showHidden.setSelected(MainController.getInstance().isShowHidden());
		ToggleGroup toggle = new ToggleGroup();
		toggle.getToggles().clear();
		groupMenu.getItems().clear();
		for (Group g : ASIOController.getInstance().getGroupList()) {
			RadioMenuItem groupMenuItem = new RadioMenuItem(g.getName());
			groupMenuItem.setToggleGroup(toggle);
			groupMenuItem.setSelected(g.getChannelList().contains(channel));
			groupMenuItem.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					MainController.getInstance().groupAllSelected(g);
					GroupController.getInstance().refresh();
				}
			});
			groupMenu.getItems().add(groupMenuItem);
		}
	}
}
