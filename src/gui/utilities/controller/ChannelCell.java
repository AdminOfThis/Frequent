package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Channel;
import data.Group;
import data.Input;
import gui.utilities.ChannelCellContextMenu;
import gui.utilities.FXMLUtil;
import gui.utilities.GroupCellContextMenu;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

public class ChannelCell extends ListCell<Input> implements Initializable {

	private static final Logger	LOG			= Logger.getLogger(ChannelCell.class);
	private static final String	FXML_PATH	= "/gui/utilities/gui/ChannelCell.fxml";
	// private static final int COLORS = 8;
	@FXML
	private AnchorPane			chartPane;
	@FXML
	private Label				lblNumber;
	private Input				input;

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
		itemProperty().addListener((obs, oldItem, newItem) -> {
			if (newItem == null) {
				this.setContextMenu(null);
			} else {
				if (getItem() instanceof Channel) {
					this.setContextMenu(new ChannelCellContextMenu((Channel) getItem()));
				} else if (getItem() instanceof Group) {
					this.setContextMenu(new GroupCellContextMenu((Group) getItem()));
				}
			}
		});
	}

	private void initContextMenu() {
		setOnContextMenuRequested(e -> {
			ContextMenu menu;
			if (input instanceof Channel) {
				menu = new ChannelCellContextMenu((Channel) input);
			} else {
				menu = new GroupCellContextMenu((Group) input);
			}
			menu.show(this, e.getScreenX(), e.getScreenY());
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}

	@Override
	protected void updateItem(Input item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			update(null);
			input = null;
		} else if (!item.equals(input)) {
			update(item);
			input = item;
		} else if (isEditing()) {
			TextField field = new TextField();
			this.setGraphic(field);
			field.setOnAction(e -> {
				getItem().setName(field.getText());
				setText(field.getText());
				setContentDisplay(ContentDisplay.TEXT_ONLY);
			});
		}
	}

	private void update(Input item) {
		Node meter = new VuMeter(input, Orientation.HORIZONTAL);
		lblNumber.setText("");
		if (item == null || item.getColor() == null) {
			this.setStyle("");
		} else {
			this.setStyle("-fx-accent: " + item.getColor());
		}
		if (item == null) {
		} else {
			if (item instanceof Channel) {
				Channel channel = (Channel) item;
				lblNumber.setText(Integer.toString(channel.getChannel().getChannelIndex()));
				if (channel.getStereoChannel() != null) {
					meter = new VuMeterStereo(channel, channel.getStereoChannel(), Orientation.HORIZONTAL);
				}
			}
		}
		chartPane.getChildren().clear();
		chartPane.getChildren().add(meter);
		AnchorPane.setTopAnchor(meter, 0.0);
		AnchorPane.setBottomAnchor(meter, 0.0);
		AnchorPane.setLeftAnchor(meter, 0.0);
		AnchorPane.setRightAnchor(meter, 0.0);
	}

	public static String toRGBCode(Color color) {
		int red = (int) (color.getRed() * 255);
		int green = (int) (color.getGreen() * 255);
		int blue = (int) (color.getBlue() * 255);
		return String.format("#%02X%02X%02X", red, green, blue);
	}
}
