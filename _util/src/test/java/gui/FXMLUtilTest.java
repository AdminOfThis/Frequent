package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.embed.swing.JFXPanel;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.paint.Color;
import test.SuperTest;

public class FXMLUtilTest extends SuperTest {

	@Test
	public void colorFade() {
		assertEquals(Color.RED, FXMLUtil.colorFade(0.0, Color.RED, Color.GREEN));
		assertEquals(Color.RED, FXMLUtil.colorFade(1.0, Color.GREEN, Color.RED));
		assertEquals(Color.rgb(255 / 2, 255 / 2, 255 / 2), FXMLUtil.colorFade(0.5, Color.WHITE, Color.BLACK));
		assertEquals(Color.RED, FXMLUtil.colorFade(1.0, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.RED));
		assertEquals(Color.RED, FXMLUtil.colorFade(0.0, Color.RED, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN));
	}

	@Test
	public void toRGB() {
		assertEquals("#FF0000", FXMLUtil.toRGBCode(Color.RED));
		assertEquals("#FF0000", FXMLUtil.toRGBCode(Color.web("#FF0000")));
	}

	@Test
	public void removeDataFromSeries() throws InterruptedException {
		Series<Number, Number> series = new Series<>();
		Data<Number, Number> dataOld = new Data<>(0, 0);
		Data<Number, Number> dataNew = new Data<>(System.nanoTime() - 100, 0);
		series.getData().add(dataNew);
		series.getData().add(dataOld);
		FXMLUtil.removeOldData(System.nanoTime() - 30000000000l, series);
		Thread.sleep(200);
		assertEquals(1, series.getData().size());
		assertTrue(series.getData().contains(dataNew));
		assertFalse(series.getData().contains(dataOld));
	}

	@Test
	public void updateAxis() {
		new JFXPanel();
		NumberAxis axis = new NumberAxis(0, 10, 0.0000001);
		FXMLUtil.updateAxis(axis, 5, 11);
		assertEquals(11, axis.getUpperBound());
		assertEquals(6, axis.getLowerBound());
		assertEquals(0.5, axis.getTickUnit());
		axis = new NumberAxis(0, 10, 5);
		FXMLUtil.updateAxis(axis, 5, 11);
		assertEquals(11, axis.getUpperBound());
		assertEquals(6, axis.getLowerBound());
		assertEquals(5, axis.getTickUnit());
	}
}
