package gui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.synthbot.jasiohost.AsioChannel;

import control.ASIOController;
import data.Channel;
import gui.utilities.LogarithmicAxis;
import gui.utilities.NegativeBackgroundAreaChart;
import gui.utilities.controller.VuMeter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class FFTController implements Initializable {

	public static final double		FFT_MIN			= -80;
	private static final Logger		LOG				= Logger.getLogger(FFTController.class);
	private static final int		X_MIN			= 25;
	private static final int		X_MAX			= 20000;
	private static final int		REFRESH_RATE	= 25;
	@FXML
	private HBox					chartRoot;

	private XYChart<Number, Number>	chart;
	private Timeline				line;
	private ASIOController			controller;
	private VuMeter					meter;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		LOG.info("Loading FFT Chart");
		initVuMeter();
		initChart();
		initTimeline();
	}

	private void initTimeline() {
		line = new Timeline();
		KeyFrame frame = new KeyFrame(Duration.millis(REFRESH_RATE), e -> {
			if (controller != null) {
				double[][] map = controller.getSpectrumMap();
				if (map != null) {
					Series<Number, Number> series = null;
					if (!chart.getData().isEmpty()) {
						series = chart.getData().get(0);
						series.getData().clear();
					}
					if (series == null) {
						series = new XYChart.Series<>();
						chart.getData().add(series);
					}
					ArrayList<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
					for (int count = 0; count < map[0].length; count++) {
						double frequency = map[0][count];
						if (frequency >= 20 && frequency <= X_MAX) {
							double level = Math.abs(map[1][count]);
							level = Channel.percentToDB(level);
							Data<Number, Number> data = new XYChart.Data<>(frequency, level);
							dataList.add(data);
						}
					}
					series.getData().addAll(dataList);
				}
			}
		});
		line.getKeyFrames().add(frame);
		line.setCycleCount(Timeline.INDEFINITE);
	}

	private void initVuMeter() {
		meter = new VuMeter(null);
		chartRoot.getChildren().add(meter);
	}

	private void initChart() {
		ValueAxis<Number> yaxis = new NumberAxis(FFT_MIN, 0, 6);
		yaxis.setPrefWidth(20.0);
		// yaxis.setAutoRanging(true);
		// yaxis.setOpacity(0.0);
		yaxis.setAnimated(true);
		ValueAxis<Number> logAxis = new LogarithmicAxis(X_MIN, X_MAX);
		chart = new NegativeBackgroundAreaChart<>(logAxis, yaxis);
		chart.setAnimated(false);
		chart.getStyleClass().add("transparent");
		((NegativeBackgroundAreaChart<Number, Number>) chart).setCreateSymbols(false);
		chart.setLegendVisible(false);
		chart.setLegendSide(Side.RIGHT);
		chart.setHorizontalZeroLineVisible(false);
		chartRoot.getChildren().add(chart);
		HBox.setHgrow(chart, Priority.ALWAYS);
	}

	public void setDriver(ASIOController driver) {
		this.controller = driver;
	}

	public void play(boolean play) {
		if (play) {
			line.playFromStart();
		} else {
			line.pause();
		}
	}

	public boolean isPlaying() {
		return line.getStatus() == Animation.Status.RUNNING;
	}

	public void setChannel(Channel channel) {
		meter.setChannel(channel);

	}
}
