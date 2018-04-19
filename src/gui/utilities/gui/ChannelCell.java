package gui.utilities.gui;

import java.util.ArrayList;

import data.Channel;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeBackgroundAreaChart;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class ChannelCell extends ListCell<Channel> {

	private static final double							REFRESH_RATE	= 50.0;

	// time for the chart in milliseconds
	private static final double							TIME_RANGE		= 30000.0;

	private BorderPane									pane			= new BorderPane();
	private Label										label			= new Label();
	private AnchorPane									chartPane		= new AnchorPane();
	private NegativeBackgroundAreaChart<Number, Number>	volumeChart;
	private Timeline									line;
	private Series<Number, Number>						series			= new Series<>();
	private Channel										channel;


	public ChannelCell() {
		initChart();
		initTimeline();
	}


	private void initChart() {
		// init list Cell
		NumberAxis x = new NumberAxis();
		x.setAutoRanging(false);
		Axis<Number> y = new LogarithmicAxis(-80.0, 0.0);
		volumeChart = new NegativeBackgroundAreaChart<>(x, y);
		volumeChart.getData().add(series);
		chartPane.getChildren().add(volumeChart);
		AnchorPane.setTopAnchor(volumeChart, .0);
		AnchorPane.setBottomAnchor(volumeChart, .0);
		AnchorPane.setRightAnchor(volumeChart, .0);
		AnchorPane.setLeftAnchor(volumeChart, .0);
		pane.setTop(label);
		pane.setCenter(chartPane);
		setGraphic(pane);
	}

	private void initTimeline() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (channel != null) {
				series.getData().add(new Data<Number, Number>(System.currentTimeMillis(), channel.getLevel()));
			}
			ArrayList<Data<Number, Number>> removeList = null;
			for (Data<Number, Number> d : series.getData()) {
				if ((double) d.getXValue() < TIME_RANGE) {
					if (removeList == null) {
						removeList = new ArrayList<>();
					}
					removeList.add(d);
				}
			}
			if (removeList != null) {
				series.getData().removeAll(removeList);
			}

		});
		line.getKeyFrames().add(frame);

	}

	@Override
	protected void updateItem(Channel item, boolean empty) {
		channel = item;
		super.updateItem(item, empty);
		if (empty) {
			update(null);
		} else {
			update(item);
		}
	}

	private void update(Channel item) {
		if (item == null) {
			label.setText(null);
			series.getData().clear();
			line.stop();
		} else {
			label.setText(item.getName());
			line.playFromStart();
		}

	}

}

