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
	private static final String	FXML_PATH	= "/fxml/utilities/ChannelCell.fxml";

	public static String toRGBCode(final Color color) {
		int red = (int) (color.getRed() * 255);
		int green = (int) (color.getGreen() * 255);
		int blue = (int) (color.getBlue() * 255);
		return String.format("#%02X%02X%02X", red, green, blue);
	}

	@FXML
	private AnchorPane	chartPane;
	@FXML
	private Label		lblNumber;
	private Input		input;

	private VuMeterIntf	meter;

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
				setContextMenu(null);
			} else {
				if (getItem() instanceof Channel) {
					setContextMenu(new ChannelCellContextMenu((Channel) getItem()));
				} else if (getItem() instanceof Group) {
					setContextMenu(new GroupCellContextMenu((Group) getItem()));
				}
			}
		});
	}

	private void changeMeter(final Input channel) {
		boolean refresh = false;
		if (channel != null && channel instanceof Channel && ((Channel) channel).getStereoChannel() != null) {
			// stereo
			if (meter == null || meter instanceof VuMeterMono) {
				meter = new VuMeterStereo(channel, ((Channel) channel).getStereoChannel(), Orientation.HORIZONTAL);
				refresh = true;
			}
		} else {
			// monoVu
			if (meter == null || meter instanceof VuMeterStereo) {
				meter = new VuMeterMono(channel, Orientation.HORIZONTAL);
				refresh = true;
			}
		}
		if (refresh) {
			chartPane.getChildren().clear();
			chartPane.getChildren().add((Node) meter);
			AnchorPane.setTopAnchor((Node) meter, 0.0);
			AnchorPane.setBottomAnchor((Node) meter, 0.0);
			AnchorPane.setLeftAnchor((Node) meter, 0.0);
			AnchorPane.setRightAnchor((Node) meter, 0.0);
		} else {
			meter.setChannel(channel);
		}
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

	// private EventHandler<ActionEvent> newGroup = new
	// EventHandler<ActionEvent>() {
	//
	// @Override
	// public void handle(ActionEvent event) {
	// TextInputDialog newGroupDialog = new TextInputDialog();
	// Optional<String> result = newGroupDialog.showAndWait();
	// if (result.isPresent()) {
	// Group g = new Group(result.get());
	// LOG.info("Created new group: " + g.getName());
	// ASIOController.getInstance().addGroup(g);
	// Input in = ChannelCell.this.getItem();
	// if (in != null && in instanceof Channel) {
	// g.addChannel((Channel) in);
	// }
	// MainController.getInstance().refresh();
	// }
	// }
	// };
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
	}

	private void update(final Input item) {
		changeMeter(item);
		lblNumber.setText("");
		if (item == null || item.getColor() == null) {
			setStyle("");
		} else {
			setStyle("-fx-accent: " + item.getColor());
		}
		if (item == null) {
			meter.setTitle(null);
		} else {
			meter.setTitle(item.getName());
			if (item instanceof Channel) {
				lblNumber.setText(Integer.toString(((Channel) item).getChannel().getChannelIndex() + 1));
			}
		}
	}

	@Override
	protected void updateItem(final Input item, final boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			update(null);
			input = null;
		} else if (!item.equals(input)) {
			update(item);
			input = item;
		} else if (isEditing()) {
			TextField field = new TextField();
			setGraphic(field);
			field.setOnAction(e -> {
				getItem().setName(field.getText());
				setText(field.getText());
				setContentDisplay(ContentDisplay.TEXT_ONLY);
			});
		}
	}
}
