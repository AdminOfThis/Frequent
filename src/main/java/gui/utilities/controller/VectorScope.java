package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
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

	private static final int X = 0;
	private static final int Y = 1;

	private static final String FXML = "/fxml/utilities/VectorScope.fxml";
	// GUI
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
	private double decay = 1;
	private float[][] ring;

	public VectorScope() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		// initialize synchronized lists
		// buffer2 = Collections.synchronizedList(new ArrayList<Float>());
		// initialize timer

		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				if (!isPaused()) {
					new Thread(() -> update()).start();
				}
//				update();
			}
		};
		timer.start();

	}

	public VectorScope(final PausableView parent) {
		this();
		parentPausable = parent;
	}

	@Override
	public void colorChanged(String newColor) {}

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
	}

	@Override
	public boolean isPaused() {
		return pause || (parentPausable != null && parentPausable.isPaused()) || channel1 == null || channel2 == null || Objects.equals(channel1, channel2);
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		// do nothing, don't care
	}

	@Override
	public void nameChanged(String name) {}

	@Override
	public void newBuffer(final Channel channel, final float[] buffer, final long position) {
		if (!isPaused()) {
			try {
				if (ring == null) {
					setDecay(1.0);
				}
				int channelIndex = 0;
				if (Objects.equals(channel2, channel)) {
					channelIndex = 1;
				}
				synchronized (ring) {
					int pos = (int) (position % ring[0].length);
					for (int i = 0; i < buffer.length; i++) {
						int posElement = (pos + i) % ring[0].length;
						ring[channelIndex][posElement] = buffer[i];
					}
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
			vectorSeries.getData().clear();
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

			// clearing ring buffer
			if (ring != null) {
				synchronized (ring) {
					for (float[] arr : ring) {
						for (int i = 0; i < arr.length; i++) {
							arr[i] = 0;
						}
					}
				}
			}
		}
	}

	public void setDecay(final double value) {
		decay = value;
		ring = new float[2][(int) Math.round(ASIOController.getInstance().getBufferSize() * decay)];
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		parentPausable = parent;
	}

	public void setTitle(final String title) {
		lblTitle.setText(title);
	}

	protected ScatterChart<Number, Number> getChart() {
		return chart;
	}

	protected void update() {
		if (!isPaused() && ring != null) {

			float[][] copy = Arrays.copyOf(ring, ring.length);

			// drawing new data
			try {
				// ad new Data
				ArrayList<Data<Number, Number>> dataToAdd = new ArrayList<>();
				for (int index = 0; /* index < DOTS_PER_BUFFER && */ index < copy[X].length - 1; index = index + 2) {
					Data<Number, Number> data = new Data<>(copy[X][index], copy[Y][index]);
					dataToAdd.add(data);
				}
				Platform.runLater(() -> vectorSeries.getData().setAll(dataToAdd));
				// modify existing Data
				for (int i = 0; i < dataToAdd.size(); i++) {
					Data<Number, Number> d = dataToAdd.get(i);
					double percent = d.getXValue().doubleValue() / d.getYValue().doubleValue();
					if (percent > 1 || percent < -1) {
						percent = 1.0 / percent;
					}
					double per = 1 - Math.abs((percent + 1) / 2.0);
					Platform.runLater(() -> d.getNode().setStyle("-fx-background-color: " + FXMLUtil.toRGBCode(FXMLUtil.colorFade(per, Color.web(Main.getAccentColor()), Color.RED))));
				}

				float maxX = 0;
				float maxY = 0;
				for (Data<Number, Number> data : vectorSeries.getData()) {
					maxX = Math.max(maxX, Math.abs(data.getXValue().floatValue()));
					maxY = Math.max(maxY, Math.abs(data.getYValue().floatValue()));
				}

				NumberAxis xAxis = (NumberAxis) chart.getXAxis();
				NumberAxis yAxis = (NumberAxis) chart.getYAxis();

				xAxis.setLowerBound(-maxX - .01);
				xAxis.setUpperBound(maxX + .01);
				yAxis.setLowerBound(-maxY - .01);
				yAxis.setUpperBound(maxY + .01);

			} catch (Exception e) {
				LOG.error("Problem while displaying data", e);
			}

		}
	}
}
