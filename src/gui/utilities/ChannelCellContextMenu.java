package gui.utilities;

import data.Channel;
import javafx.scene.control.MenuItem;

public class ChannelCellContextMenu extends InputCellContextMenu {

	private MenuItem resetName = new MenuItem("Reset Name");

	public ChannelCellContextMenu(Channel in) {
		super(in);
		if (in != null) {
			resetName.setOnAction(e -> in.setName(((Channel) in).getChannel().getChannelName()));
			getItems().add(1, resetName);
		}
	}

}
