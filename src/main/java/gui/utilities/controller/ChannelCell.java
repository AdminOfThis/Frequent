package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.Channel;
import data.Group;
import data.Input;
import com.github.adminofthis.util.gui.FXMLUtil;
import gui.pausable.Pausable;
import gui.utilities.ChannelCellContextMenu;
import gui.utilities.GroupCellContextMenu;
import javafx.application.Platform;
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

public class ChannelCell extends ListCell<Input> implements Initializable {

	private static final Logger LOG = LogManager.getLogger(ChannelCell.class);
	private static final String FXML_PATH = "/fxml/utilities/ChannelCell.fxml";
	@FXML
	private AnchorPane chartPane;
	@FXML
	private Label lblNumber;
	private Input input;
	private VuMeter meter;
	private Pausable pausable;
	private Parent graphic;

	public ChannelCell() {
		super();
		getStyleClass().add("vuMeter-background");
		setPadding(new Insets(1, 5, 1, 5));
		graphic = FXMLUtil.loadFXML(getClass().getResource(FXML_PATH), this);
		if (graphic != null) {
			setGraphic(graphic);
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

	public ChannelCell(Pausable p) {
		this();
		pausable = p;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		// nothing to do
	}

	private void changeMeter(final Input channel) {
		boolean refresh = false;
		if (meter != null) {
			// trash old meter
			meter.pause(true);
			meter.setParentPausable(null);
			meter.setChannel(null);
		}
		if (channel != null && channel instanceof Channel && ((Channel) channel).getStereoChannel() != null) {
			// stereo
			if (meter == null || meter instanceof VuMeterMono) {
				meter = new VuMeterStereo(channel, ((Channel) channel).getStereoChannel(), Orientation.HORIZONTAL);
				refresh = true;
				meter.setParentPausable(pausable);
			}
		} else {
			// monoVu
			if (meter == null || meter instanceof VuMeterStereo) {
				meter = new VuMeterMono(channel, Orientation.HORIZONTAL);
				refresh = true;
				meter.setParentPausable(pausable);
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

//		setOnContextMenuRequested(e -> {
		ContextMenu menu;
		if (input instanceof Channel) {
			menu = new ChannelCellContextMenu((Channel) input);
		} else {
			menu = new GroupCellContextMenu((Group) input);
		}
//			menu.show(this, e.getScreenX(), e.getScreenY());
//		});
		setContextMenu(menu);
	}

	private void update(final Input item) {
		changeMeter(item);
		Platform.runLater(() -> lblNumber.setText(""));
		if (item == null || item.getColor() == null) {
			setStyle("");
		} else {
			setStyle("-fx-accent: " + item.getColor());
		}
		if (item == null) {
			meter.setTitle(null);
			meter.pause(true);
		} else {
			meter.pause(false);
			meter.setTitle(item.getName());
			if (item instanceof Channel) {
				if (((Channel) item).getChannel() != null) {
					Platform.runLater(() -> lblNumber.setText(Integer.toString(((Channel) item).getChannel().getChannelIndex() + 1)));
				}
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
