package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import gui.controller.Pausable;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.HBox;

public class VectorscopeController implements Initializable, Pausable, ChannelListener {

	private static final Logger				LOG				= Logger.getLogger(VectorscopeController.class);

	@FXML
	private HBox							chartParent;
	@FXML
	private ScatterChart<Number, Number>	chart;


	private Series<Number, Number>			vectorSeries	= new Series<>();
	private boolean							pause;
	private Pausable						parentPausable;

	private Channel							channel1, channel2;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chart.getData().add(vectorSeries);
		chart.prefWidthProperty().bindBidirectional(chart.prefHeightProperty());

		ChangeListener<Number> lis = new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				System.out.println("hey");
				double h = (double) Math.min(chartParent.getWidth(), chartParent.getHeight());
				double val = Math.sqrt(0.5 * Math.pow(h, 2));
				chart.setPrefHeight(val);

			}
		};
		chartParent.heightProperty().addListener(lis);
		chartParent.widthProperty().addListener(lis);

		initializeTimeline();
	}

	private void initializeTimeline() {

	}

	public void setChannels(Channel c1, Channel c2) {
		if (!c1.equals(channel1) || !c2.equals(channel2)) {
			if (channel1 != null) {
				channel1.removeObserver(this);
			}
			if (channel2 != null) {
				channel2.removeObserver(this);
			}
			channel1 = c1;
			channel2 = c2;
			if (channel1 != null) {
				channel1.addObserver(this);
			}
			if (channel2 != null) {
				channel2.addObserver(this);
			}
		}
	}

	public Channel getChannel1() {
		return channel1;
	}

	public Channel getChannel2() {
		return channel2;
	}


	@Override
	public void pause(boolean pause) {
		this.pause = pause;

	}

	@Override
	public boolean isPaused() {
		return pause || parentPausable.isPaused();
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parentPausable = parent;
	}

	@Override
	public void levelChanged(double level) throws Exception {
		// do nothing, don't care
	}

	@Override
	public void newBuffer(float[] buffer) {
		// TODO
		System.out.println(buffer);
	}

}
