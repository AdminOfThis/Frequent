package gui.utilities.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Group;
import data.Input;
import gui.controller.GroupController;
import gui.controller.MainController;
import gui.utilities.FXMLUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ChannelCell extends TreeCell<Input> implements Initializable {

	private static final Logger	LOG			= Logger.getLogger(ChannelCell.class);
	private static final String	FXML_PATH	= "/gui/utilities/gui/ChannelCell.fxml";
	private static final int	COLORS		= 8;
	@FXML
	private Label				label;
	@FXML
	private AnchorPane			chartPane;
	private Input				input;
	private VuMeter				meter;
	private ContextMenu			contextMenu	= new ContextMenu();

	public ChannelCell() {
		super();
		setPadding(Insets.EMPTY);
		Parent p = FXMLUtil.loadFXML(FXML_PATH, this);
		if (p != null) {
			setGraphic(p);
		} else {
			LOG.warn("Unable to load ChannelCell");
		}
		// context Menu
		initContextMenu();
		emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
			if (isNowEmpty) {
				this.setContextMenu(null);
			} else {
				this.setContextMenu(contextMenu);
			}
		});
	}

	private void initContextMenu() {
		// adding colorPicker
		Menu colorMenu = new Menu("Color");
		contextMenu.getItems().add(colorMenu);
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
				LOG.info("Changing color of " + getItem().getName() + " to " + colorHex);
				getItem().setColor(colorHex);
				MainController.getInstance().refresh();
				GroupController.getInstance().refresh();
			});
		}
		Menu groupMenu = new Menu("Groups");
		MenuItem newGroupMenu = new MenuItem("New Group");
		newGroupMenu.setOnAction(newGroup);
		// groups
		contextMenu.getItems().add(groupMenu);
		contextMenu.setOnShowing(e -> {
			groupMenu.getItems().clear();
			groupMenu.getItems().add(newGroupMenu);
			groupMenu.getItems().add(new SeparatorMenuItem());
			ToggleGroup toggle = new ToggleGroup();
			for (Group g : ASIOController.getInstance().getGroupList()) {
				RadioMenuItem groupMenuItem = new RadioMenuItem(g.getName());
				groupMenuItem.setToggleGroup(toggle);
				groupMenuItem.selectedProperty().addListener(new ChangeListener<Boolean>() {

					@Override
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (getItem() != null && getItem() instanceof Channel) {
							Channel channel = (Channel) getItem();
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
			Input item = this.getItem();
			if (item != null && item instanceof Channel) {
				for (MenuItem g : groupMenu.getItems()) {
					if (g instanceof RadioMenuItem) {
						String text = g.getText();
						Group group = ((Channel) item).getGroup();
						if (group != null && text.equals(group.getName())) {
							((RadioMenuItem) g).setSelected(true);
							break;
						} else {
							((RadioMenuItem) g).setSelected(false);
						}
					}
				}
			}
		});
	}

	private EventHandler<ActionEvent> newGroup = new EventHandler<ActionEvent>() {

		@Override
		public void handle(ActionEvent event) {
			TextInputDialog newGroupDialog = new TextInputDialog();
			Optional<String> result = newGroupDialog.showAndWait();
			if (result.isPresent()) {
				ASIOController.getInstance().addGroup(new Group(result.get()));
				GroupController.getInstance().refresh();
			}
		}
	};

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initVuMeter();
	}

	private void initVuMeter() {
		meter = new VuMeter(null, Orientation.HORIZONTAL);
		// meter.setRotate(90.0);
		chartPane.getChildren().add(meter);
		AnchorPane.setTopAnchor(meter, 0.0);
		AnchorPane.setBottomAnchor(meter, 0.0);
		AnchorPane.setLeftAnchor(meter, -18.0);
		AnchorPane.setRightAnchor(meter, 0.0);
	}

	@Override
	protected void updateItem(Input item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			update(null);
			input = null;
		} else if (input != item) {
			update(item);
			input = item;
		}
	}

	private void update(Input item) {
		meter.setChannel(item);
		if (item == null || item.getColor() == null) {
			this.setStyle("");
		} else {
			this.setStyle("-fx-accent: " + item.getColor());
		}
		if (item == null) {
			label.setText(null);
		} else {
			label.setText(item.getName());
		}
	}

	public static String toRGBCode(Color color) {
		int red = (int) (color.getRed() * 255);
		int green = (int) (color.getGreen() * 255);
		int blue = (int) (color.getBlue() * 255);
		return String.format("#%02X%02X%02X", red, green, blue);
	}
}
