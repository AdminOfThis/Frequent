package gui.utilities;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Group;
import data.Input;
import gui.controller.MainController;
import javafx.beans.value.ChangeListener;
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
	private Menu				pairingMenu	= new Menu("Pairing");
	private CheckMenuItem		noPair		= new CheckMenuItem("Unpaired");

	public ChannelCellContextMenu(final Channel in) {
		super(in);
		if (in != null) {
			resetName.setOnAction(e -> {
				in.setName(in.getChannel().getChannelName());
			});
			hide.setOnAction(e -> hideAllSelected());
			showHidden.setOnAction(e -> MainController.getInstance().setShowHidden(showHidden.isSelected()));
			getItems().add(resetName);
			getItems().add(new SeparatorMenuItem());
			getItems().add(hide);
			getItems().add(showHidden);
			getItems().add(new SeparatorMenuItem());
			// groups
			getItems().add(groupMenu);
			getItems().add(pairingMenu);
			pairingMenu.getItems().add(noPair);
			pairingMenu.getItems().add(new SeparatorMenuItem());
			noPair.selectedProperty().addListener((obs, old, newValue) -> {
				if (newValue) {
					in.setStereoChannel(null);
				}
			});
			//
			setOnShowing(e -> refreshData(in));
			setAutoHide(true);
			newGroup.setOnAction(e -> {
				TextInputDialog newGroupDialog = new TextInputDialog();
				Optional<String> result = newGroupDialog.showAndWait();
				if (result.isPresent()) {
					Group g = new Group(result.get());
					LOG.info("Created new group: " + g.getName());
					ASIOController.getInstance().addGroup(g);
					groupAllSelected(g);
					MainController.getInstance().refresh();
				}
			});
		}
	}

	private void groupAllSelected(final Group g) {
		ArrayList<Input> list = MainController.getInstance().getSelectedChannels();
		for (Input i : list) {
			if (i instanceof Channel) {
				Channel c = (Channel) i;
				if (g != null) {
					g.addChannel(c);
				} else {
					c.getGroup().removeChannel(c);
				}
			}
		}
		MainController.getInstance().refresh();
	}

	private void hideAllSelected() {
		ArrayList<Input> list = MainController.getInstance().getSelectedChannels();
		Channel c = null;
		int i = 0;
		while (c == null) {
			if (list.get(i) instanceof Channel) {
				c = (Channel) list.get(i);
			}
		}
		if (c != null) {
			boolean hide = !c.isHidden();
			for (Input in : list) {
				if (in instanceof Channel) {
					Channel c2 = (Channel) in;
					c2.setHidden(hide);
				}
			}
		}
	}

	private void refreshData(final Channel channel) {
		showHidden.setSelected(MainController.getInstance().isShowHidden());
		groupMenu.getItems().clear();
		groupMenu.getItems().add(newGroup);
		groupMenu.getItems().add(new SeparatorMenuItem());
		ToggleGroup toggle = new ToggleGroup();
		for (Group g : ASIOController.getInstance().getGroupList()) {
			RadioMenuItem groupMenuItem = new RadioMenuItem(g.getName());
			toggle.getToggles().add(groupMenuItem);
			groupMenuItem.setSelected(g.getChannelList().contains(channel));
			groupMenuItem.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
				if (newValue) {
					groupAllSelected(g);
				} else {
					groupAllSelected(null);
				}
				MainController.getInstance().refresh();
			});
			groupMenu.getItems().add(groupMenuItem);
		}
		ToggleGroup channelGroup = new ToggleGroup();
		for (Channel c : ASIOController.getInstance().getInputList()) {
			if (!channel.equals(c)) {
				RadioMenuItem channelCheck = new RadioMenuItem(c.getName());
				channelGroup.getToggles().add(channelCheck);
				pairingMenu.getItems().add(channelCheck);
				channelCheck.setSelected(c.equals(channel.getStereoChannel()));
				channelCheck.selectedProperty().addListener((obs, old, newValue) -> {
					if (newValue) {
						LOG.info("Setting Stereo Channel of " + channel.getName() + " to " + c.getName());
						channel.setStereoChannel(c);
						MainController.getInstance().refresh();
					}
				});
			}
		}
	}
}
