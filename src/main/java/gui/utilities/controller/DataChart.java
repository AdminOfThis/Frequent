package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ChannelListener;
import control.FFT;
import data.Channel;
import data.Input;
import com.github.adminofthis.util.gui.FXMLUtil;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.AnchorPane;

public class DataChart extends AnchorPane implements Initializable, PausableComponent, ChannelListener {

	private static final Logger LOG = LogManager.getLogger(WaveFormChart.class);
	private static final String FXML = "/fxml/utilities/DataChart.fxml";
	@FXML
	private LineChart<Number, Number> chart;
	@FXML
	private NumberAxis yAxis;
	private Series<Number, Number> series = new Series<>();
	private Channel channel;
	private List<Float> pendingData = Collections.synchronizedList(new ArrayList<>());
	private Pausable pausableParent;
	private boolean pause = false;

	public DataChart() {
		LOG.debug("Creating new DataChart");
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML), this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {

				new Thread(() -> update()).start();
			}
		};
		timer.start();
	}

	@Override
	public void colorChanged(String newColor) {}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chart.getData().add(series);

	}

	@Override
	public boolean isPaused() {
		return pause || pausableParent != null && pausableParent.isPaused() || channel == null;
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		// do nothing
	}

	@Override
	public void nameChanged(String name) {}

	@Override
	public void newBuffer(final Channel channel, final float[] buffer, final long time) {
		float[] windowed = FFT.applyWindow(buffer);
		synchronized (pendingData) {
			pendingData.clear();
			for (float buf : windowed) {
				pendingData.add(buf);
			}
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	public void setChannel(final Channel c) {
		try {
			if (!Objects.equals(c, channel)) {
				if (channel != null) {
					channel.removeListener(this);
				}
				series.getData().clear();
				pendingData.clear();
				synchronized (pendingData) {
					pendingData.clear();
				}
				channel = c;
				if (c != null) {
					c.addListener(this);
				}
			}
		} catch (Exception e) {
			LOG.warn("", e);
		}
	}

	@Override
	public void setParentPausable(Pausable parent) {
		pausableParent = parent;
	}

	private void update() {
		if (!isPaused()) {
			List<Float> dataCopy;
			synchronized (pendingData) {
				dataCopy = new ArrayList<>(pendingData);
			}
			ArrayList<Data<Number, Number>> adding = new ArrayList<>();
			float max = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < dataCopy.size(); i++) {
				max = Math.max(max, Math.abs(dataCopy.get(i)));
				adding.add(new Data<Number, Number>(i, dataCopy.get(i)));
			}
			max = max + .05f;
			final float axisTop = Math.max(max, .2f);

			Platform.runLater(() -> {
				series.getData().setAll(adding);
				yAxis.setLowerBound(-axisTop);
				yAxis.setUpperBound(axisTop);
			});
			if (!series.getData().isEmpty()) {
				((NumberAxis) chart.getXAxis()).setLowerBound(series.getData().get(0).getXValue().doubleValue());
				((NumberAxis) chart.getXAxis()).setUpperBound(series.getData().get(series.getData().size() - 1).getXValue().doubleValue());
			}
		}
	}
}
