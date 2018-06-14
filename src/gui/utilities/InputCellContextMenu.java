package gui.utilities;

import java.util.Optional;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Group;
import data.Input;
import gui.controller.GroupController;
import gui.controller.MainController;
import gui.utilities.controller.InputCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public abstract class InputCellContextMenu extends ContextMenu {

	private static final Logger	LOG			= Logger.getLogger(InputCellContextMenu.class);

	private static final int	COLORS		= 8;

	private MenuItem			name		= new MenuItem("Rename");
	private MenuItem			newGroup	= new MenuItem("New Group");
	private Menu				colorMenu	= new Menu("Color");


	private Input				input;

	public InputCellContextMenu(final Input in) {
		super();
		input = in;
		if (input != null) {
			// NAME
			name.setOnAction(e -> {
				TextInputDialog dialog = new TextInputDialog();
				Optional<String> result = dialog.showAndWait();
				if (result.isPresent()) {
					input.setName(result.get());
				}
			});

			for (int i = 0; i < COLORS; i++) {
				double hue = (360.0 / COLORS) * i;
				Color color = Color.hsb(hue, 1.0, 1.0);
				Circle circle = new Circle(5.0);
				String colorHex = toRGBCode(color);
				circle.setStyle("-fx-fill: " + colorHex);
				MenuItem item = new MenuItem("Color #" + i);
				item.setGraphic(circle);
				colorMenu.getItems().add(item);
				item.setOnAction(e -> {
					LOG.info("Changing color of " + input.getName() + " to " + colorHex);
					input.setColor(colorHex);
					MainController.getInstance().refresh();
					GroupController.getInstance().refresh();
				});
			}

			newGroup.setOnAction(e -> {
				TextInputDialog newGroupDialog = new TextInputDialog();
				Optional<String> result = newGroupDialog.showAndWait();
				if (result.isPresent()) {
					Group g = new Group(result.get());
					LOG.info("Created new group: " + g.getName());
					ASIOController.getInstance().addGroup(g);
					if (input != null && input instanceof Channel) {
						((Channel) in).setGroup(g);
					}
					GroupController.getInstance().refresh();
				}
			});

			getItems().add(name);
			getItems().add(colorMenu);
			getItems().add(newGroup);
		}
	}

	public static String toRGBCode(Color color) {
		int red = (int) (color.getRed() * 255);
		int green = (int) (color.getGreen() * 255);
		int blue = (int) (color.getBlue() * 255);
		return String.format("#%02X%02X%02X", red, green, blue);
	}

}
