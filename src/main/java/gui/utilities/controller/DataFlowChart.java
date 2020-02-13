package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class DataFlowChart extends AnchorPane implements Initializable, PausableComponent, ChannelListener {

	private static final Logger LOG = LogManager.getLogger(DataFlowChart.class);
	private static final String FXML = "/fxml/utilities/DataFlowChart.fxml";

	private static final double TIME_FRAME = .6;
	@FXML
	private LineChart<Number, Number> chart;
	@FXML
	private NumberAxis yAxis;
	private Series<Number, Number> series = new Series<>();
	private Series<Number, Number> treshold = new Series<>();
	private Channel channel;
	private List<Float> pendingData = Collections.synchronizedList(new ArrayList<>());
	private Pausable pausableParent;
	private boolean pause = true;

	public DataFlowChart() {
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
	public void colorChanged(String newColor) {
		// do nothing
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chart.getData().add(series);
	}

	@Override
	public boolean isPaused() {
		return pause || channel == null || (pausableParent != null && pausableParent.isPaused());
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		// do nothing
	}

	@Override
	public void nameChanged(String name) {
		// do nothing
	}

	@Override
	public void newBuffer(final Channel channel, final float[] buffer, final long time) {
//		float[] windowed = FFT.applyWindow(buffer);
		synchronized (pendingData) {
			pendingData.clear();
			for (float buf : buffer) {
				pendingData.add(buf);
			}
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
		if (pause) {
			clear();
		}
	}

	private void clear() {
		Platform.runLater(() -> {
			try {
				chart.getData().remove(series);
				series.getData().clear();
				chart.getData().add(series);
				pendingData.clear();

			} catch (Exception e) {
				LOG.error("Problem displaying data", e);
			}
		});
	}

	public void setChannel(final Channel c) {
		new Thread(() -> {
			try {
				if (!Objects.equals(c, channel)) {
					if (channel != null) {
						channel.removeListener(this);
					}
					clear();
					channel = c;
					if (c != null) {
						c.addListener(this);
					}
				}
			} catch (Exception e) {
				LOG.warn("", e);
			}
		}).start();
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
			pendingData.clear();

			int show = (int) (ASIOController.getInstance().getSampleRate() * TIME_FRAME);
			if (!series.getData().isEmpty()) {
				int upperbound = Math.max(0, series.getData().size() - show);
				if (upperbound > 0) {
					Platform.runLater(() -> {
						if (series.getData().size() > upperbound) {
							series.getData().remove(0, upperbound);
						}
					});
				}
			}

			int count = 0;
			if (!series.getData().isEmpty()) {
				count = series.getData().get(series.getData().size() - 1).getXValue().intValue();
			}
			ArrayList<Data<Number, Number>> adding = new ArrayList<>();
			for (int i = 0; i < dataCopy.size(); i++) {
				adding.add(new Data<Number, Number>(count + i, dataCopy.get(i)));
			}

			try {
				Platform.runLater(() -> series.getData().addAll(adding));
			} catch (Exception e) {
				LOG.error("Problem displaying data", e);
			}
			NumberAxis xAxis = ((NumberAxis) chart.getXAxis());
			if (!series.getData().isEmpty()) {
				double upper = series.getData().get(series.getData().size() - 1).getXValue().intValue();
				xAxis.setUpperBound(upper);
				xAxis.setLowerBound(upper - show);
			}
			if (treshold.getData().size() >= 2) {
				treshold.getData().get(1).setXValue(xAxis.getUpperBound() + 10000);
			}
		}

	}

	public void setThreshold(final double value) {
		NumberAxis time = (NumberAxis) chart.getXAxis();
		synchronized (treshold) {
			treshold.getData().clear();
			treshold.getData().add(new Data<>(time.getLowerBound(), value));
			treshold.getData().add(new Data<>(time.getUpperBound() + 10000, value));
		}
	}

	public void showTreshold(boolean value) {
		if (value) {
			if (!chart.getData().contains(treshold)) {
				chart.getData().add(treshold);
			}
		} else {
			chart.getData().remove(treshold);
		}
	}
}
