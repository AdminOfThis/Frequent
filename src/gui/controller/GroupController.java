package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import gui.utilities.controller.VuMeter;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupController implements Initializable {

	@FXML
	private SplitPane				root;
	@FXML
	private SplitPane				groupPane;
	@FXML
	private HBox					vuPane;
	private static GroupController	instance;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;

	}

	public static GroupController getInstance() {
		return instance;
	}

	public void refresh() {
		vuPane.getChildren().clear();
		groupPane.getItems().clear();
		// if (ASIOController.getInstance() != null) {

		// TODO

		int maxChannels = 25;

		for (int i = 0; i < 5; i++) {

			// groups
			VuMeter meter = new VuMeter(null, Orientation.VERTICAL);

			VBox meterBox = new VBox(meter, new Label("Group #" + i));
			meterBox.setAlignment(Pos.TOP_CENTER);
			meterBox.setMinWidth(40.0);
			VBox.setVgrow(meter, Priority.ALWAYS);
			vuPane.getChildren().add(meterBox);
			HBox.setHgrow(meterBox, Priority.ALWAYS);

			// individual channels

			HBox groupBox = new HBox();
			groupBox.setSpacing(5.0);
			ScrollPane scroll = new ScrollPane(groupBox);
			scroll.setFitToHeight(true);
			scroll.setFitToWidth(true);
			groupPane.getItems().add(scroll);
			SplitPane.setResizableWithParent(scroll, false);
			int count = (i + 1) * 5;
			VBox first = null;
			for (int j = 0; j < count; j++) {
				VuMeter meter2 = new VuMeter(null, Orientation.VERTICAL);
				VBox meter2Box = new VBox(meter2, new Label("Test #" + j));
				if (first == null) {
					first = meter2Box;
				}
				meter2Box.setAlignment(Pos.TOP_CENTER);
				meter2Box.setMinWidth(40.0);
				VBox.setVgrow(meter2, Priority.ALWAYS);
				groupBox.getChildren().add(meter2Box);
				HBox.setHgrow(meter2Box, Priority.ALWAYS);
			}

			for (int j = groupBox.getChildren().size(); j < maxChannels; j++) {
				Pane pane = new Pane();
				pane.setMinWidth(40.0);
				HBox.setHgrow(pane, Priority.ALWAYS);
				groupBox.getChildren().add(pane);
			}
		}

		// smoothing out splitPane
		int divCount = groupPane.getDividerPositions().length;
		double equalSize = 1.0 / (divCount + 1);
		double divPosValues[] = new double[divCount];
		for (int count = 1; count < divCount + 1; count++) {
			divPosValues[count - 1] = equalSize * count;
		}
		groupPane.setDividerPositions(divPosValues);

		// }
	}

}
