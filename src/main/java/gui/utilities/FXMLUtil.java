package gui.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.paint.Color;
import main.Main;

public abstract class FXMLUtil {

	private static final Logger		LOG			= Logger.getLogger(FXMLUtil.class);
	public static final String		STYLE_SHEET	= "/css/style.css";
	private static Initializable	controller;

	private static Color colorFade(final Color baseColor, final Color targetColor, final double percent) {
		Color result = null;
		double deltaRed = targetColor.getRed() - baseColor.getRed();
		double deltaGreen = targetColor.getGreen() - baseColor.getGreen();
		double deltaBlue = targetColor.getBlue() - baseColor.getBlue();
		double redD = baseColor.getRed() + deltaRed * percent;
		double greenD = baseColor.getGreen() + deltaGreen * percent;
		double blueD = baseColor.getBlue() + deltaBlue * percent;
		int red = (int) Math.floor(redD * 255.0);
		int green = (int) Math.floor(greenD * 255.0);
		int blue = (int) Math.floor(blueD * 255.0);
		result = Color.rgb(red, green, blue);
		return result;
	}

	public static Color colorFade(final double percent, final Color... colors) {
		try {
			int index = (int) Math.floor(percent * (colors.length - 1));
			// if topped (only with 1.0 percent
			if (index == colors.length - 1) {
				return colors[colors.length - 1];
			} else {
				Color baseColor = colors[index];
				Color targetColor = colors[index + 1];
				double percentNew = (percent - 1.0 / (colors.length - 1) * index) / (1.0 / (colors.length - 1));
				return colorFade(baseColor, targetColor, percentNew);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return colors[0];
		}
	}

	public static Initializable getController() {
		return controller;
	}

	public static String getStyleValue(final String value) {
		String result = "";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(FXMLUtil.class.getResourceAsStream(STYLE_SHEET)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(value)) {
					result = line.split(":")[1];
					break;
				}
			}
		} catch (Exception e) {
			LOG.warn("Unable to load css value " + value);
			LOG.debug("", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.error("Problem closing file reader", e);
				}
			}
		}
		if (result.endsWith(";")) {
			result = result.substring(0, result.length() - 1);
		}
		return result.trim();
	}

	public static Parent loadFXML(final String string) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(FXMLUtil.class.getResource(string));
			parent = loader.load();
			controller = loader.getController();
		} catch (Exception e) {
			LOG.error("Unable to load FXMLFile");
			LOG.info("", e);
		}
		return parent;
	}

	public static Parent loadFXML(final String string, final Initializable controller) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(FXMLUtil.class.getResource(string));
			loader.setController(controller);
			parent = loader.load();
		} catch (Exception e) {
			LOG.error("Unable to load FXMLFile", e);
		}
		return parent;
	}

	public static String toRGBCode(final Color color) {
		int red = (int) (color.getRed() * 255);
		int green = (int) (color.getGreen() * 255);
		int blue = (int) (color.getBlue() * 255);
		return String.format("#%02X%02X%02X", red, green, blue);
	}

	public static void setStyleSheet(Parent p) {
		try {
			p.getStylesheets().add(FXMLUtil.class.getResource(FXMLUtil.STYLE_SHEET).toExternalForm());
			p.setStyle(Main.getStyle());
		} catch (Exception e) {
			LOG.warn("Unable to style dialog", e);
			LOG.debug("", e);
		}
	}

	public static void updateAxis(final NumberAxis xAxis, final long timeFrame, long currentTime) {
		long lower = currentTime - timeFrame;
		xAxis.setLowerBound(lower);
		xAxis.setUpperBound(currentTime);
		if ((currentTime - lower) / xAxis.getTickUnit() > 10) {
			xAxis.setTickUnit(Math.ceil(currentTime - lower) / 10.0);
		}
	}

	public static void removeOldData(final long lowerBound, final Series<Number, Number> series) {
		ArrayList<Data<Number, Number>> removeList = null;
		try {
			for (Data<Number, Number> data : series.getData()) {
				if (data.getXValue().longValue() < (lowerBound - 100)) {
					if (removeList == null) {
						removeList = new ArrayList<>();
					}
					removeList.add(data);
				}
			}
		} catch (Exception e) {
			LOG.error("", e);
		}
		if (removeList != null) {
			series.getData().removeAll(removeList);
		}
	}

}
