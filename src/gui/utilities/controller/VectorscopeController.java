package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import gui.controller.Pausable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.HBox;

public class VectorscopeController implements Initializable, Pausable, ChannelListener {

	private static final Logger				LOG				= Logger.getLogger(VectorscopeController.class);
	private static final int				MAX_DATA_POINTS	= 5000;
	@FXML
	private HBox							chartParent;
	@FXML
	private ScatterChart<Number, Number>	chart;
	private Series<Number, Number>			vectorSeries	= new Series<>();
	private boolean							pause;
	private Pausable						parentPausable;
	private Channel							channel1, channel2;
	private boolean							restarting		= true;
	private long							timeFirstBuffer, timeSecondBuffer;
	private float[]							buffer1, buffer2;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.info("Starting vectorscope");
		chart.getData().add(vectorSeries);
		chart.prefWidthProperty().bindBidirectional(chart.prefHeightProperty());
		ChangeListener<Number> lis = new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				double h = Math.min(chartParent.getWidth(), chartParent.getHeight());
				double val = Math.sqrt(0.5 * Math.pow(h, 2));
				chart.setPrefHeight(val);
			}
		};
		chartParent.heightProperty().addListener(lis);
		chartParent.widthProperty().addListener(lis);
		initializeTimeline();
	}

	private void initializeTimeline() {}

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
			if (channel1 != null && channel2 != null) {
				restarting = true;
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
		if (restarting) {
			if (timeFirstBuffer == 0) {
				timeFirstBuffer = System.nanoTime();
			} else if (timeSecondBuffer == 0) {
				timeSecondBuffer = System.nanoTime();
			} else {
				// Checking timings to get synched
				long timeThirdBuffer = System.nanoTime();
				if (timeSecondBuffer - timeFirstBuffer < timeThirdBuffer - timeSecondBuffer) {
					buffer1 = buffer;
				}
				restarting = false;
				timeFirstBuffer = 0;
				timeSecondBuffer = 0;
			}
		} else {
			// Restarting done
			if (buffer1 == null) {
				buffer1 = buffer;
			} else {
				buffer2 = buffer;
				// compare the buffers to the scope
				float[] x = new float[buffer.length];
				float[] y = new float[buffer.length];
				for (int index = 0; index < buffer.length - 1; index++) {
					x[index] = buffer1[index];
					y[index] = buffer2[index];
				}
				showData(x, y);
				// clear buffers
				buffer1 = null;
			}
		}
		System.out.println(buffer);
	}

	private void showData(float[] x, float[] y) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				// drawing new data
				for (int index = 0; index < x.length - 1; index++) {
					vectorSeries.getData().add(new Data<Number, Number>(x[index], y[index]));
				}
				// removing old data points
				if (vectorSeries.getData().size() > MAX_DATA_POINTS) {
					vectorSeries.getData().remove(0, vectorSeries.getData().size() - MAX_DATA_POINTS);
				}
			}
		});
	}
}
