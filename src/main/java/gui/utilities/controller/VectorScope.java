package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import data.Input;
import gui.FXMLUtil;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.pausable.PausableView;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
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
	// pausable
	private boolean							pause;
	private Pausable						parentPausable;
	// data
	private Channel							channel1, channel2;
	// private boolean restarting = true;
	// vars to align the buffers, instead of beeing of by one
	// private long timeFirstBuffer, timeSecondBuffer;
	// buffers
	// private List<Float> buffer1, buffer2;
	private Map<Long, float[]>				map1, map2;
	private double							decay			= 1.0;

	public VectorScope() {
		Parent p = FXMLUtil.loadFXML(FXML, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		// initialize synchronized lists
		map1 = Collections.synchronizedMap(new LinkedHashMap<>());
		map2 = Collections.synchronizedMap(new LinkedHashMap<>());
		// buffer1 = Collections.synchronizedList(new ArrayList<Float>());
		// buffer2 = Collections.synchronizedList(new ArrayList<Float>());
		// initialize timer
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				update();
			}
		};
		timer.start();
	}

	public VectorScope(final PausableView parent) {
		this();
		parentPausable = parent;
	}

	public Channel getChannel1() {
		return channel1;
	}

	public Channel getChannel2() {
		return channel2;
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		chart.getStyleClass().add("vectorscope");
		chart.getData().add(vectorSeries);
		chart.prefWidthProperty().bindBidirectional(chart.prefHeightProperty());
		ChangeListener<Number> lis = (observable, oldValue, newValue) -> {
			double h = Math.min(chartParent.getWidth(), chartParent.getHeight());
			double val = Math.sqrt(0.5 * Math.pow(h, 2));
			chart.setPrefHeight(val);
		};
		chartParent.heightProperty().addListener(lis);
		chartParent.widthProperty().addListener(lis);
		NumberAxis x = (NumberAxis) chart.getXAxis();
		NumberAxis y = (NumberAxis) chart.getYAxis();
		x.setAnimated(false);
		y.setAnimated(false);
		x.lowerBoundProperty().bindBidirectional(x.upperBoundProperty());
		y.lowerBoundProperty().bindBidirectional(y.upperBoundProperty());
		x.lowerBoundProperty().bindBidirectional(y.lowerBoundProperty());
	}

	@Override
	public boolean isPaused() {
		return pause || parentPausable != null && parentPausable.isPaused() || channel1 == null || channel2 == null;
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		// do nothing, don't care
	}

	@Override
	public void newBuffer(final Channel channel, final float[] buffer, final long position) {
		if (!isPaused()) {
			try {
				if (!map1.containsKey(position)) {
					synchronized (map1) {
						map1.put(position, buffer);
					}
				} else if (!map2.containsKey(position)) {
					synchronized (map2) {
						map2.put(position, buffer);
					}
				} else {
					throw new IllegalArgumentException("Both buffers are filled with this sample position already");
				}
			}
			catch (Exception e) {
				LOG.error("Problem showing vectorscope", e);
			}
		}
	}

	@Override
	public void pause(final boolean pause) {
		this.pause = pause;
	}

	public void setChannels(final Channel c1, final Channel c2) {
		if (!Objects.equals(c1, channel1) || !Objects.equals(c2, channel2)) {
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
		}
	}

	public void setDecay(final double value) {
		decay = value;
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		parentPausable = parent;
	}

	public void setTitle(final String title) {
		lblTitle.setText(title);
	}

	private void showData(final float[] x, final float[] y) {
		// drawing new data
		if (x.length == y.length) {
			ArrayList<Data<Number, Number>> dataToAdd = new ArrayList<>();
			for (int index = 0; /* index < DOTS_PER_BUFFER && */ index < x.length - 1; index = index + 2) {
				Data<Number, Number> data = new Data<>(x[index], y[index]);
				dataToAdd.add(data);
			}
			vectorSeries.getData().addAll(dataToAdd);
			for (Data<Number, Number> d : dataToAdd) {
				double percent = d.getXValue().doubleValue() / d.getYValue().doubleValue();
				if (percent > 1 || percent < -1) {
					percent = 1.0 / percent;
				}
				percent = 1 - Math.abs((percent + 1) / 2.0);
				d.getNode().setStyle("-fx-background-color: " + FXMLUtil.toRGBCode(FXMLUtil.colorFade(percent, Color.web(Main.getAccentColor()), Color.RED)));
			}
		} // removing old data points
		if (vectorSeries.getData().size() > MAX_DATA_POINTS * decay) {
			List<Data<Number, Number>> removeList = vectorSeries.getData().subList(0, (int) Math.round(vectorSeries.getData().size() - MAX_DATA_POINTS * decay));
			vectorSeries.getData().removeAll(removeList);
		}
	}

	private void update() {
		if (!isPaused()) {
			ArrayList<Long> clearedKeys = null;
			synchronized (map1) {
				for (long key : map1.keySet()) {
					if (map2.containsKey(key)) {
						synchronized (map2) {
							showData(map1.get(key), map2.get(key));
							if (clearedKeys == null) {
								clearedKeys = new ArrayList<>();
							}
							clearedKeys.add(key);
						}
					}
				}
			}
			// clear buffers
			if (clearedKeys != null) {
				for (long key : clearedKeys) {
					synchronized (map1) {
						map1.remove(key);
					}
					synchronized (map2) {
						map1.remove(key);
					}
				}
			}
		}
	}
}
