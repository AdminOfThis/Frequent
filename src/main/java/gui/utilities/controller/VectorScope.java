package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.pausable.PausableView;
import gui.utilities.FXMLUtil;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import main.Main;

public class VectorScope extends AnchorPane implements Initializable, PausableComponent, ChannelListener {

	private static final Logger				LOG				= Logger.getLogger(VectorScope.class);
	private static final String				FXML			= "/fxml/utilities/VectorScope.fxml";
	// GUI
	private static final int				MAX_DATA_POINTS	= 200;
	// private static final int DOTS_PER_BUFFER = 500;
	@FXML
	private HBox							chartParent;
	@FXML
	private ScatterChart<Number, Number>	chart;
	@FXML
	private Label							lblTitle;
	private Series<Number, Number>			vectorSeries	= new Series<>();
	// GUI data
	private AnimationTimer					timer;
	// pausable
	private boolean							pause;
	private Pausable						parentPausable;
	// data
	private Channel							channel1, channel2;
	private boolean							restarting		= true;
	// vars to align the buffers, instead of beeing of by one
	private long							timeFirstBuffer, timeSecondBuffer;
	// buffers
	private List<Float>						buffer1, buffer2;
	private double							decay			= 1.0;

	public VectorScope(PausableView parent) {
		this();
		parentPausable = parent;
	}

	public VectorScope() {
		Parent p = FXMLUtil.loadFXML(FXML, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		// initialize synchronized lists
		buffer1 = Collections.synchronizedList(new ArrayList<Float>());
		buffer2 = Collections.synchronizedList(new ArrayList<Float>());
		// initialize timer
		timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				update();
			}
		};
		timer.start();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chart.getStyleClass().add("vectorscope");
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
		chart.getXAxis().setAnimated(false);
		chart.getYAxis().setAnimated(false);
	}

	public void setChannels(Channel c1, Channel c2) {
		if (!c1.equals(channel1) || !c2.equals(channel2)) {
			if (channel1 != null) {
				channel1.removeListener(this);
			}
			if (channel2 != null) {
				channel2.removeListener(this);
			}
			channel1 = c1;
			channel2 = c2;
			if (channel1 != null) {
				channel1.addListener(this);
			}
			if (channel2 != null) {
				channel2.addListener(this);
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

	public void setTitle(String title) {
		lblTitle.setText(title);
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause || (parentPausable != null && parentPausable.isPaused()) || channel1 == null || channel2 == null;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parentPausable = parent;
	}

	@Override
	public void levelChanged(double level) throws Exception {
		// do nothing, don't care
	}

	private void update() {
		if (!isPaused()) {
			showData(buffer1, buffer2);
			// clear buffers
			buffer1.clear();
			buffer2.clear();
		}
	}

	@Override
	public void newBuffer(float[] buffer) {
		if (!isPaused()) {
			try {
				if (restarting) {
					if (timeFirstBuffer == 0) {
						timeFirstBuffer = System.nanoTime();
					} else if (timeSecondBuffer == 0) {
						timeSecondBuffer = System.nanoTime();
					} else {
						// Checking timings to get synched
						long timeThirdBuffer = System.nanoTime();
						if (timeSecondBuffer - timeFirstBuffer < timeThirdBuffer - timeSecondBuffer) {
							for (float f : buffer) {
								buffer1.add(f);
							}
						}
						restarting = false;
						timeFirstBuffer = 0;
						timeSecondBuffer = 0;
					}
				} else {
					// Restarting done
					ArrayList<Float> tempList = new ArrayList<>(buffer.length);
					for (float f : buffer) {
						tempList.add(f);
					}
					if (buffer1.size() < buffer2.size()) {
						buffer1.addAll(tempList);
					} else {
						buffer2.addAll(tempList);
					}
				}
			} catch (Exception e) {
				LOG.error("Problem showing vectorscope", e);
			}
		}
	}

	private void showData(final List<Float> x, final List<Float> y) {
		// drawing new data
		if (x.size() == y.size()) {
			ArrayList<Data<Number, Number>> dataToAdd = new ArrayList<>();
			for (int index = 0; /* index < DOTS_PER_BUFFER && */ index < x.size() - 2; index = index + 2) {
				Data<Number, Number> data = new Data<>(x.get(index), y.get(index));
				dataToAdd.add(data);
			}
			vectorSeries.getData().addAll(dataToAdd);
			for (Data<Number, Number> d : dataToAdd) {
				double percent = (d.getXValue().doubleValue() / d.getYValue().doubleValue());
				if (percent > 1 || percent < -1) {
					percent = 1.0 / percent;
				}
				percent = 1 - Math.abs((percent + 1) / 2.0);
				d.getNode().setStyle("-fx-background-color: "
					+ FXMLUtil.toRGBCode(FXMLUtil.colorFade(percent, Color.web(Main.getAccentColor()), Color.RED)));
			}
		} // removing old data points
		if (vectorSeries.getData().size() > (MAX_DATA_POINTS * decay)) {
			List<Data<Number, Number>> removeList = vectorSeries.getData().subList(0,
				(int) Math.round(vectorSeries.getData().size() - (MAX_DATA_POINTS * decay)));
			vectorSeries.getData().removeAll(removeList);
		}
	}

	public void setMax(final double max) {
		double value;
		if (max == -1) {
			value = 1.0;
		} else {
			value = Math.abs(max);
		}
		NumberAxis x = (NumberAxis) chart.getXAxis();
		x.setUpperBound(value);
		x.setLowerBound(-value);
		NumberAxis y = (NumberAxis) chart.getYAxis();
		y.setUpperBound(value);
		y.setLowerBound(-value);
	}

	public void setDecay(double value) {
		decay = value;
	}
}
