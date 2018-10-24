package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.shape.Circle;

public class BackgroundItemController implements Initializable {

	@FXML
	private PieChart		chart;
	@FXML
	private Circle			circle;
	private PieChart.Data	data;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		data = new Data("", 0.0);
		PieChart.Data filler = new Data("", 1.0);
		chart.getData().add(data);
		chart.getData().add(filler);
		data.getNode().setStyle("-fx-pie-color: derive(-fx-accent, -80%)");
		filler.getNode().setStyle("-fx-pie-color: transparent");
		chart.setStartAngle(225.0);
		circle.radiusProperty().bind(chart.widthProperty().divide(4.0).multiply(0.75));
		setPercent(Math.random());
	}

	public void setPercent(double percent) {
		if (percent < 0.0 || percent > 1.0) {
			throw new IllegalArgumentException("percent must be >=0.0 and <= 1.0");
		}
		data.setPieValue(percent * 3.0);

	}

}
