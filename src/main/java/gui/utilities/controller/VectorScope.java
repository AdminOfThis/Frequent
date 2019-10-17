package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import data.Input;
import gui.FXMLUtil;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.pausable.PausableView;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
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

/**
 * 
 * @author AdminOfThis
 *
 */
public class VectorScope extends AnchorPane implements Initializable, PausableComponent, ChannelListener {

	private static final Logger LOG = LogManager.getLogger(VectorScope.class);
	private static final String FXML = "/fxml/utilities/VectorScope.fxml";
	// GUI
	private static final int MAX_DATA_POINTS = 200;
	// private static final int DOTS_PER_BUFFER = 500;
	@FXML
	private HBox chartParent;
	@FXML
	private ScatterChart<Number, Number> chart;
	@FXML
	private Label lblTitle;
	private Series<Number, Number> vectorSeries = new Series<>();
	// GUI data
	// pausable
	private boolean pause = false;
	private Pausable parentPausable;
	// data
	private Channel channel1, channel2;
	// private boolean restarting = true;
	// vars to align the buffers, instead of beeing of by one
	// private long timeFirstBuffer, timeSecondBuffer;
	// buffers
	// private List<Float> buffer1, buffer2;
	protected Map<Long, float[]> map1, map2;
	private double decay = 1.0;

	long add1, add2;

	public VectorScope() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML), this);
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
				new Thread(() -> update()).start();
//				update();
			}
		};
		timer.start();

		new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);

						System.out.println("Cleaner runs");
						for (Map<Long, float[]> map : new Map[] { map1, map2 }) {

							synchronized (map) {
								if (!map.isEmpty()) {
									ArrayList<Long> remove = new ArrayList<Long>();
									Long max = Collections.max(map.keySet());
									for (Long key : map1.keySet()) {
										if (key < max - 1000) {
											remove.add(key);
										}
									}
									map.keySet().removeAll(remove);
								}
							}
						}
						System.out.println("Cleaner finished");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		}.start();

	}

	protected ScatterChart<Number, Number> getChart() {
		return chart;
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
				System.out.println(channel.getName() + " " + position);
				if (!map1.containsKey(position) && !map2.containsKey(position + 1)) {
					synchronized (map1) {
						map1.put(position, buffer);
						add1++;
					}
				} else if (!map2.containsKey(position) && !map1.containsKey(position + 1)) {
					synchronized (map2) {
						map2.put(position, buffer);
						add2++;
					}
				} else {
					throw new IllegalArgumentException("Both buffers are filled with this sample position already");
				}

			} catch (Exception e) {
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

	protected void showData(final float[] x, final float[] y) {
		// drawing new data
		if (x.length == y.length) {
			ArrayList<Data<Number, Number>> dataToAdd = new ArrayList<>();
			for (int index = 0; /* index < DOTS_PER_BUFFER && */ index < x.length - 1; index = index + 2) {
				Data<Number, Number> data = new Data<>(x[index], y[index]);
				dataToAdd.add(data);
			}
			vectorSeries.getData().addAll(dataToAdd);
			for (int i = 0; i < dataToAdd.size(); i++) {
				Data<Number, Number> d = dataToAdd.get(i);
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

	protected void update() {
		if (!isPaused()) {
			ArrayList<Long> keysToDisplay = null;
			LinkedHashMap<Long, float[]> copyMap1, copyMap2;
			synchronized (map1) {
				copyMap1 = new LinkedHashMap<Long, float[]>(map1);
			}
			synchronized (map2) {
				copyMap2 = new LinkedHashMap<Long, float[]>(map2);
			}
			Long[] keyArray = copyMap1.keySet().toArray(new Long[0]);
			for (int i = 0; i < keyArray.length; i++) {
				long key = keyArray[i];
				if (copyMap2.containsKey(key)) {

					if (keysToDisplay == null) {
						keysToDisplay = new ArrayList<>();
					}
					keysToDisplay.add(key);

				}
			}

			// clear buffers
			if (keysToDisplay != null) {
				for (int i = 0; i < keysToDisplay.size(); i++) {
					long key = keysToDisplay.get(i);
					float[] copy1, copy2;
					synchronized (map1) {
						copy1 = map1.get(key);
					}
					synchronized (map2) {
						copy2 = map2.get(key);
					}

					System.out.println(map1.size() + "(" + add1 + ") " + map2.size() + " (" + add2 + ")");
					Platform.runLater(() -> showData(copy1, copy2));
//					showData(copy1, copy2);

				}

				synchronized (map2) {
					map2.keySet().removeAll(keysToDisplay);
				}
				synchronized (map1) {
					map1.keySet().removeAll(keysToDisplay);
				}

			}
		}
	}
}
