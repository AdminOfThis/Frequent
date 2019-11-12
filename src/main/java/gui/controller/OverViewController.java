package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.Input;
import gui.pausable.PausableView;
import gui.utilities.controller.VuMeter;
import gui.utilities.controller.VuMeterMono;
import gui.utilities.controller.VuMeterStereo;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public class OverViewController implements Initializable, PausableView {

	private static final double GAP = 10.0;
	private static final Orientation ORIENTATION = Orientation.VERTICAL;
	private static final int DEFAULT_CHANNELS_PER_ROW = 8;
	private static final int MIN_CHANNELS_PER_ROW = 4;
	private static final int MAX_CHANNELS_PER_ROW = 16;

	@FXML
	private BorderPane root;
	@FXML
	private ToggleButton tglShowHidden;
	@FXML
	private GridPane flow;

	private boolean pause = true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		root.setCenter(flow);
		tglShowHidden.selectedProperty().bindBidirectional(MainController.getInstance().showHiddenProperty());

	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public ArrayList<Node> getHeader() {
		ArrayList<Node> result = new ArrayList<>();
		result.add(tglShowHidden);
		return result;
	}

	@Override
	public void refresh() {
		flow.getChildren().clear();
		flow = new GridPane();
		flow.setHgap(GAP);
		flow.setVgap(GAP);
		root.setCenter(flow);
		if (ASIOController.getInstance() != null) {
			// total number of channels to display
			ArrayList<Channel> channelsToDisplay = new ArrayList<>();
			for (Channel c : ASIOController.getInstance().getInputList()) {
				if ((!c.isHidden() || MainController.getInstance().isShowHidden()) && (!c.isStereo() || c.isLeftChannel())) {
					channelsToDisplay.add(c);
				}
			}
			int channels = channelsToDisplay.size();

			if (channels > 0) {
				int rows = calculateRows(channels);

				// split evenly in Lists with desired size
				List<List<Channel>> partitionList = ListUtils.partition(channelsToDisplay, (int) Math.ceil(channels / (double) rows));

				for (int rowIndex = 0; rowIndex < partitionList.size(); rowIndex++) {
					// Getting the matching row list
					List<Channel> rowList = partitionList.get(rowIndex);
					for (int columnIndex = 0; columnIndex < rowList.size(); columnIndex++) {
						// And the channel on that column
						Channel c = rowList.get(columnIndex);
						// create VuMeter
						VuMeter meter;
						if (c.isStereo()) {
							meter = new VuMeterStereo(c, c.getStereoChannel(), ORIENTATION);
						} else {
							meter = new VuMeterMono(c, ORIENTATION);
						}
						meter.setParentPausable(this);
						flow.add(meter, columnIndex, rowIndex);
					}
				}
				for (int i = flow.getColumnConstraints().size(); i < flow.getColumnCount(); i++) {
					ColumnConstraints constraint = new ColumnConstraints();
					constraint.setMinWidth(30.0);
					constraint.setFillWidth(true);
					constraint.setPercentWidth(-1);
					constraint.setPrefWidth(Region.USE_COMPUTED_SIZE);
					constraint.setHgrow(Priority.SOMETIMES);
					flow.getColumnConstraints().add(constraint);

				}

				for (int i = flow.getRowConstraints().size(); i < flow.getRowCount(); i++) {
					RowConstraints constraint = new RowConstraints();
					constraint.setMinHeight(50.0);
					constraint.setFillHeight(true);
					constraint.setPercentHeight(-1);
					constraint.setPrefHeight(Region.USE_COMPUTED_SIZE);
					constraint.setVgrow(Priority.SOMETIMES);
					flow.getRowConstraints().add(constraint);
				}

			}
		}
	}

	private static int calculateRows(int channels) {
		int rows = -1;
		int channelsPerRow = DEFAULT_CHANNELS_PER_ROW;
		int count = 0;
		while (rows <= 0 || Math.ceil(channels / (double) rows) < MIN_CHANNELS_PER_ROW || Math.ceil(channels / (double) rows) > MAX_CHANNELS_PER_ROW) {
			// terminate condition, to prevent forever looping
			count++;
			if (count > 20) {
				rows = 1;
				break;
			}
			// on the first run, skip adjustments
			if (rows >= 0) {
				// if not enough channels per row, double channels per row

				if (Math.ceil(channels / (double) rows) > MAX_CHANNELS_PER_ROW) {
					channelsPerRow = channelsPerRow / 2;
				}
				// if too many channels per row, half channels per row
				if (Math.ceil(channels / (double) rows) < MIN_CHANNELS_PER_ROW) {
					channelsPerRow = channelsPerRow * 2;
				}
			}
			// results in x number of rows, defined by Channels per row

			rows = (int) Math.ceil(channels / (double) channelsPerRow);

		}
		System.out.println(channels + " " + rows);
		return rows;
	}

	@Override
	public void setSelectedChannel(Input in) {

	}

}
