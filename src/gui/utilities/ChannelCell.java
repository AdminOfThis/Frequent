package gui.utilities;

import java.util.ArrayList;

import data.Channel;
import data.Input;
import gui.controller.FFTController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ChannelCell extends TreeCell<Input> {

	private static final double							REFRESH_RATE	= 50.0;
	// time for the chart in milliseconds
	private static final double							TIME_RANGE		= 30000.0;
	private StackPane									pane			= new StackPane();
	private Label										label			= new Label();
	private AnchorPane									chartPane		= new AnchorPane();
	private NegativeBackgroundAreaChart<Number, Number>	volumeChart;
	private Timeline									line;
	private Series<Number, Number>						series			= new Series<>();
	private Input										input;

	public ChannelCell() {
		label.setStyle("-fx-font-size: 15px");
		pane.getChildren().add(chartPane);
		pane.getChildren().add(label);
		StackPane.setAlignment(label, Pos.TOP_LEFT);
		setGraphic(pane);
		initChart();
		initTimeline();
	}

	private void initChart() {
		// init list Cell
		chartPane.setMinHeight(10);
		chartPane.setPrefHeight(45);
		chartPane.setMinWidth(10);
		chartPane.setPrefWidth(10);
		NumberAxis x = new NumberAxis();
		x.setTickUnit(1000);
		x.setAutoRanging(false);
		x.setForceZeroInRange(false);
		x.setMinHeight(0.0);
		// x.setMaxHeight(0.0);
		x.setPrefHeight(0.0);
		// x.setMinWidth(0.0);
		// x.setMaxWidth(0.0);
		// x.setPrefWidth(0.0);
		x.setOpacity(0.0);
		NumberAxis y = new NumberAxis(FFTController.FFT_MIN, 0.0, 5.0);
		// y.setMinHeight(0.0);
		// y.setMaxHeight(0.0);
		// y.setPrefHeight(0.0);
		y.setMinWidth(0.0);
		// y.setMaxWidth(0.0);
		y.setPrefWidth(0.0);
		y.setOpacity(0.0);
		volumeChart = new NegativeBackgroundAreaChart<>(x, y);
		volumeChart.setHorizontalZeroLineVisible(false);
		volumeChart.setHorizontalGridLinesVisible(false);
		volumeChart.setVerticalGridLinesVisible(false);
		volumeChart.setVerticalZeroLineVisible(false);
		volumeChart.setAlternativeColumnFillVisible(false);
		volumeChart.setAnimated(false);
		volumeChart.setCreateSymbols(false);
		volumeChart.setLegendVisible(false);
		volumeChart.setMinHeight(10);
		volumeChart.setPrefHeight(10);
		volumeChart.setMaxHeight(10);
		volumeChart.setMinWidth(10);
		volumeChart.setPrefWidth(10);
		volumeChart.setMaxWidth(10);
		volumeChart.getData().add(series);
		chartPane.getChildren().add(volumeChart);
		AnchorPane.setTopAnchor(volumeChart, -15.0);
		AnchorPane.setBottomAnchor(volumeChart, -15.0);
		AnchorPane.setRightAnchor(volumeChart, -20.0);
		AnchorPane.setLeftAnchor(volumeChart, -20.0);
	}

	private void initTimeline() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (input != null && input instanceof Channel) {
				Channel channel = (Channel) input;
				double value = Channel.percentToDB(channel.getLevel() * 1000.0);
				NumberAxis y = (NumberAxis) volumeChart.getYAxis();
				if (value < y.getLowerBound()) {
					value = y.getLowerBound();
				}
				series.getData().add(new Data<Number, Number>(System.currentTimeMillis(), value));
			} else {
				series.getData().clear();
			}
			ArrayList<Data<Number, Number>> removeList = null;
			for (Data<Number, Number> d : series.getData()) {
				long time = System.currentTimeMillis();
				if (time - (long) d.getXValue() > TIME_RANGE) {
					if (removeList == null) {
						removeList = new ArrayList<>();
					}
					removeList.add(d);
				}
			}
			if (removeList != null) {
				series.getData().removeAll(removeList);
			}
			NumberAxis x = (NumberAxis) volumeChart.getXAxis();
			x.setLowerBound(System.currentTimeMillis() - TIME_RANGE + 1000);
			x.setUpperBound(System.currentTimeMillis());
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Timeline.INDEFINITE);
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
		series.getData().clear();
		if (item == null) {
			label.setText(null);
			line.stop();
		} else {
			label.setText(item.getName());
			line.playFromStart();
		}
	}
}
