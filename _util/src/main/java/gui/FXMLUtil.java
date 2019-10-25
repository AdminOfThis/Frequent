package gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public final class FXMLUtil {

	private static final Logger LOG = LogManager.getLogger(FXMLUtil.class);
	private static String styleSheetPath = "/css/style.css";
	private static String defaultStyle = "";
	private static Initializable controller;

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
			reader = new BufferedReader(new InputStreamReader(FXMLUtil.class.getResourceAsStream(styleSheetPath)));
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

	/**
	 * Loads teh requested URL file and returns the root of the file as parent
	 * 
	 * @param url The URL of the FXML file to load
	 * @return The root of the loaded file
	 */
	public static Parent loadFXML(final URL url) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(url);
			parent = loader.load();
			setStyleSheet(parent);
			controller = loader.getController();
		} catch (Exception e) {
			LOG.error("Unable to load FXMLFile", e);
		}
		return parent;
	}

	/**
	 * Loads the requested FXML file with the controller predefined
	 * 
	 * @param url        The URL of the FXML file to load
	 * @param controller The controller for the loaded ressource to set
	 * @return THe root of the loaded File
	 */
	public static Parent loadFXML(final URL url, final Initializable controller) {
		Parent parent = null;
		try {
			FXMLLoader loader = new FXMLLoader(url);
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
			p.getStylesheets().add(FXMLUtil.styleSheetPath);
			p.setStyle(getDefaultStyle());
		} catch (Exception e) {
			LOG.warn("Unable to style dialog");
		}
	}

	/**
	 * Adjussts the time axis of a chart by using the cureent time and the desired
	 * length of the frame
	 * 
	 * @param xAxis       The axis of which the scale is to be adjusted
	 * @param timeFrame   The width of the timeframe, defining the lower bound by
	 *                    *currentTime - timeFrame*
	 * @param currentTime The current runtime of the system
	 */
	public static void updateAxis(final NumberAxis xAxis, final long timeFrame, long currentTime) {
		long lower = currentTime - timeFrame;
		xAxis.setLowerBound(lower);
		xAxis.setUpperBound(currentTime);
		if ((currentTime - lower) / xAxis.getTickUnit() > 10) {
			xAxis.setTickUnit(Math.ceil(currentTime - lower) / 10.0);
		}
	}

	public static double setPrefWidthToMaximumRequired(Region... button) {

		double max = 0;
		Region r = null;
		for (Region node : button) {
			if (node.getWidth() > max) {
				max = node.getWidth();
				r = node;
			}
		}
		for (Region node : button) {
			if (!node.equals(r)) {
				node.setPrefWidth(max);
			}
		}
		return max;
	}

	public static void removeOldData(final long lowerBound, final Series<Number, Number> series) {

		ArrayList<Data<Number, Number>> removeList = new ArrayList<>();
		try {
			ArrayList<Data<Number, Number>> copyList;
			synchronized (series) {
				copyList = new ArrayList<>(series.getData());
			}
			for (Data<Number, Number> data : copyList) {
				if (data.getXValue().longValue() < (lowerBound - 100)) {

					removeList.add(data);
				}
			}
		} catch (Exception e) {
			LOG.error("", e);
		}
		if (removeList != null) {
			synchronized (series.getData()) {
				try {
					Platform.runLater(() -> series.getData().removeAll(removeList));
				} catch (Exception e) {
					LOG.warn("Unable to use FX-Application Thread, removing on this thread");
					series.getData().removeAll(removeList);
				}
			}
		}
	}

	public static String deriveColor(final String baseColor, final int index, final int total) {
		String result = baseColor;
		Color color = Color.web(baseColor);
		double value = (double) index / (double) total;
		color = color.deriveColor(1, 1, value, 1);
		result = FXMLUtil.toRGBCode(color);
		return result;
	}

	public static String getDefaultStyle() {
		return defaultStyle;
	}

	public static void setDefaultStyle(String defaultStyle) {
		FXMLUtil.defaultStyle = defaultStyle;
	}

	public static String getStyleSheetPath() {
		return styleSheetPath;
	}

	public static void setStyleSheetPath(String styleSheetPath) {
		FXMLUtil.styleSheetPath = styleSheetPath;
	}

	public static void setIcon(Stage stage, String logo) {
		try {
			stage.getIcons().add(new Image(FXMLUtil.class.getResourceAsStream(logo)));
		} catch (Exception e) {
			LOG.error("Unable to load logo");
			LOG.debug("", e);
		}
	}
}
