package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import data.Channel;
import data.Group;
import data.Input;
import gui.utilities.FXMLUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.AnchorPane;

public class ChannelCell extends TreeCell<Input> implements Initializable {

	private static final Logger	LOG				= Logger.getLogger(ChannelCell.class);
	private static final String	FXML_PATH		= "/gui/utilities/gui/ChannelCell.fxml";
	private static final double	REFRESH_RATE	= 100.0;
	// time for the chart in milliseconds
	private static final double	TIME_RANGE		= 30000.0;
	@FXML
	private Label				label;
	@FXML
	private AnchorPane			chartPane;
	private Input				input;
	private VuMeter				meter;

	public ChannelCell() {
		super();
		setPadding(Insets.EMPTY);
		Parent p = FXMLUtil.loadFXML(FXML_PATH, this);
		if (p != null) {
			setGraphic(p);
		} else {
			LOG.warn("Unable to load ChannelCell");
		}
	}

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
		if (item instanceof Channel) {
			Channel c = (Channel) item;
			meter.setChannel(c);
			if (item == null || item.getColor() == null) {
				this.setStyle("");
			} else {
				this.setStyle("-fx-accent: " + item.getColor());
			}
		} else if (item instanceof Group) {
			meter.setChannel(null);
			if (item == null || item.getColor() == null) {
				this.setStyle("");
			} else {
				this.setStyle("-fx-background-color: " + item.getColor() + "; -fx-accent: " + item.getColor());
			}
		}
		if (item == null) {
			label.setText(null);
		} else {
			label.setText(item.getName());
		}
	}
}
