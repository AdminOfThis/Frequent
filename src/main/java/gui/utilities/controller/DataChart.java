package gui.utilities.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ChannelListener;
import data.Channel;
import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.utilities.FXMLUtil;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

public class DataChart extends AnchorPane implements Initializable, PausableComponent, ChannelListener {

	private static final Logger			LOG			= Logger.getLogger(WaveFormChart.class);
	private static final String			FXML		= "/fxml/utilities/DataChart.fxml";
	@FXML
	private BorderPane					root;
	@FXML
	private LineChart<Number, Number>	chart;
	private Series<Number, Number>		series		= new Series<>();
	private Channel						channel;
	private List<Float>					pendingData	= Collections.synchronizedList(new ArrayList<>());
	private Pausable					pausableParent;
	private boolean						pause		= false;

	public DataChart() {
		LOG.debug("Creating new DataChart");
		Parent p = FXMLUtil.loadFXML(FXML, this);
		getChildren().add(p);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				update();
			}
		};
		timer.start();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		chart.getData().add(series);
	}

	private void update() {
		if (!isPaused()) {
			synchronized (pendingData) {
				ArrayList<Data<Number, Number>> adding = new ArrayList<>();
				for (int i = 0; i < pendingData.size(); i++) {
					adding.add(new Data<Number, Number>(i, pendingData.get(i)));
				}
				series.getData().setAll(adding);
			}
		}
	}

	public void setChannel(final Channel c) {
		try {
			if (!Objects.equals(c, channel)) {
				if (channel != null) {
					channel.removeListener(this);
				}
				series.getData().clear();
				synchronized (pendingData) {
					pendingData.clear();
				}
				channel = c;
				if (c != null) {
					c.addListener(this);
				}
			}
		}
		catch (Exception e) {
			LOG.warn("", e);
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public boolean isPaused() {
		return pause || pausableParent != null && pausableParent.isPaused() || channel == null;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		pausableParent = parent;
	}

	@Override
	public void levelChanged(final Input input, final double level, final long time) {
		// do nothing
	}

	@Override
	public void newBuffer(final Channel channel, final float[] buffer, final long time) {
		synchronized (pendingData) {
			pendingData.clear();
			for (float buf : buffer) {
				pendingData.add(buf);
			}
		}
	}
}
