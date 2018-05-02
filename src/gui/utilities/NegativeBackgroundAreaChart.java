package gui.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * AreaChart - Plots the area between the line that connects the data points and
 * the 0 line on the Y axis. This implementation Plots the area between the line
 * that connects the data points and the bottom of the chart area.
 * 
 * @since JavaFX 2.0
 */
public class NegativeBackgroundAreaChart<X, Y> extends AreaChart<X, Y> {

	protected Map<Series<X, Y>, DoubleProperty> shadowSeriesYMultiplierMap = new HashMap<>();

	// -------------- CONSTRUCTORS
	// ----------------------------------------------
	public NegativeBackgroundAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
		this(xAxis, yAxis, FXCollections.<Series<X, Y>> observableArrayList());
	}

	public NegativeBackgroundAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis,
		@NamedArg("data") ObservableList<Series<X, Y>> data) {
		super(xAxis, yAxis, data);
	}

	// -------------- METHODS
	// ------------------------------------------------------------------------------------------
	@Override
	protected void seriesAdded(Series<X, Y> series, int seriesIndex) {
		DoubleProperty seriesYAnimMultiplier = new SimpleDoubleProperty(this, "seriesYMultiplier");
		shadowSeriesYMultiplierMap.put(series, seriesYAnimMultiplier);
		super.seriesAdded(series, seriesIndex);
	}

	@Override
	protected void seriesRemoved(final Series<X, Y> series) {
		shadowSeriesYMultiplierMap.remove(series);
		super.seriesRemoved(series);
	}

	@Override
	protected void layoutPlotChildren() {
		// super.layoutPlotChildren();
		try {
			List<LineTo> constructedPath = new ArrayList<>(getDataSize());
			for (int seriesIndex = 0; seriesIndex < getDataSize(); seriesIndex++) {
				Series<X, Y> series = getData().get(seriesIndex);
				DoubleProperty seriesYAnimMultiplier = shadowSeriesYMultiplierMap.get(series);
				double lastX = 0;
				final ObservableList<Node> children = ((Group) series.getNode()).getChildren();
				ObservableList<PathElement> seriesLine = ((Path) children.get(1)).getElements();
				ObservableList<PathElement> fillPath = ((Path) children.get(0)).getElements();
				seriesLine.clear();
				fillPath.clear();
				constructedPath.clear();
				for (Iterator<Data<X, Y>> it = getDisplayedDataIterator(series); it.hasNext();) {
					Data<X, Y> item = it.next();
					double x = getXAxis().getDisplayPosition(item.getXValue());// FIXME:
																				// here
																				// should
																				// be
																				// used
																				// item.getCurrentX()
					double y = getYAxis().getDisplayPosition(getYAxis().toRealValue(getYAxis().toNumericValue(item.getYValue())));// FIXME:
																																	// here
																																	// should
																																	// be
																																	// used
																																	// item.getCurrentY()
					constructedPath.add(new LineTo(x, y));
					if (Double.isNaN(x) || Double.isNaN(y)) {
						continue;
					}
					lastX = x;
					Node symbol = item.getNode();
					if (symbol != null) {
						final double w = symbol.prefWidth(-1);
						final double h = symbol.prefHeight(-1);
						symbol.resizeRelocate(x - (w / 2), y - (h / 2), w, h);
					}
				}
				if (!constructedPath.isEmpty()) {
					Collections.sort(constructedPath, (e1, e2) -> Double.compare(e1.getX(), e2.getX()));
					LineTo first = constructedPath.get(0);
					seriesLine.add(new MoveTo(first.getX(), first.getY()));
					fillPath.add(new MoveTo(first.getX(), getYAxis().getHeight()));
					seriesLine.addAll(constructedPath);
					fillPath.addAll(constructedPath);
					fillPath.add(new LineTo(lastX, getYAxis().getHeight()));
					fillPath.add(new ClosePath());
				}
			}
			// smoothing

			double height = getLayoutBounds().getHeight();
			getData().forEach(series -> {
				final Path[] paths = getPaths(series);
				if (null == paths) {
					return;
				}

				smooth(paths[1].getElements(), paths[0].getElements(), height);

				paths[0].setVisible(true);
				paths[0].setManaged(true);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Returns an array of paths where the first entry represents the fill path
	 * and the second entry represents the stroke path
	 * 
	 * @param SERIES
	 * @return an array of paths where [0] == FillPath and [1] == StrokePath
	 */
	private Path[] getPaths(final Series<X, Y> SERIES) {
		if (!getData().contains(SERIES)) {
			return null;
		}

		Node seriesNode = SERIES.getNode();
		if (null == seriesNode) {
			return null;
		}

		Group seriesGroup = (Group) seriesNode;
		if (seriesGroup.getChildren().isEmpty() || seriesGroup.getChildren().size() < 2) {
			return null;
		}

		return new Path[] { /* FillPath */ (Path) (seriesGroup).getChildren().get(0),
			/* StrokePath */ (Path) (seriesGroup).getChildren().get(1) };
	}

	private void smooth(ObservableList<PathElement> strokeElements, ObservableList<PathElement> fillElements, final double HEIGHT) {
		if (fillElements.isEmpty())
			return;
		// as we do not have direct access to the data, first recreate the list
		// of all the data points we have
		final Point2D[] dataPoints = new Point2D[strokeElements.size()];
		for (int i = 0; i < strokeElements.size(); i++) {
			final PathElement element = strokeElements.get(i);
			if (element instanceof MoveTo) {
				final MoveTo move = (MoveTo) element;
				dataPoints[i] = new Point2D(move.getX(), move.getY());
			} else if (element instanceof LineTo) {
				final LineTo line = (LineTo) element;
				final double x = line.getX(), y = line.getY();
				dataPoints[i] = new Point2D(x, y);
			}
		}
		double firstX = dataPoints[0].getX();
		double lastX = dataPoints[dataPoints.length - 1].getX();

		Point2D[] points = subdividePoints(dataPoints, 16);

		fillElements.clear();
		fillElements.add(new MoveTo(firstX, HEIGHT));

		strokeElements.clear();
		strokeElements.add(new MoveTo(points[0].getX(), points[0].getY()));

		for (Point2D p : points) {
			if (Double.compare(p.getX(), firstX) >= 0) {
				fillElements.add(new LineTo(p.getX(), p.getY()));
				strokeElements.add(new LineTo(p.getX(), p.getY()));
			}
		}

		fillElements.add(new LineTo(lastX, HEIGHT));
		fillElements.add(new LineTo(0, HEIGHT));
		fillElements.add(new ClosePath());
	}

	/**
	 * Gets the size of the data returning 0 if the data is null
	 *
	 * @return The number of items in data, or null if data is null
	 */
	public int getDataSize() {
		final ObservableList<Series<X, Y>> data = getData();
		return (data != null) ? data.size() : 0;
	}

	public static final Point2D[] subdividePoints(final Point2D[] POINTS, final int SUB_DEVISIONS) {
		assert POINTS != null;
		assert POINTS.length >= 3;

		int noOfPoints = POINTS.length;

		Point2D[] subdividedPoints = new Point2D[((noOfPoints - 1) * SUB_DEVISIONS) + 1];

		double increments = 1.0 / (double) SUB_DEVISIONS;

		for (int i = 0; i < noOfPoints - 1; i++) {
			Point2D p0 = i == 0 ? POINTS[i] : POINTS[i - 1];
			Point2D p1 = POINTS[i];
			Point2D p2 = POINTS[i + 1];
			Point2D p3 = (i + 2 == noOfPoints) ? POINTS[i + 1] : POINTS[i + 2];

			CatmullRom crs = new CatmullRom(p0, p1, p2, p3);

			for (int j = 0; j <= SUB_DEVISIONS; j++) {
				subdividedPoints[(i * SUB_DEVISIONS) + j] = crs.q(j * increments);
			}
		}
		return subdividedPoints;
	}


}

/**
 * User: hansolo Date: 03.11.17 Time: 04:47
 */
class CatmullRom {
	private CatmullRomSpline	splineXValues;
	private CatmullRomSpline	splineYValues;


	// ******************** Constructors
	// **************************************
	public CatmullRom(final Point2D P0, final Point2D P1, final Point2D P2, final Point2D P3) {
		assert P0 != null : "p0 cannot be null";
		assert P1 != null : "p1 cannot be null";
		assert P2 != null : "p2 cannot be null";
		assert P3 != null : "p3 cannot be null";

		splineXValues = new CatmullRomSpline(P0.getX(), P1.getX(), P2.getX(), P3.getX());
		splineYValues = new CatmullRomSpline(P0.getY(), P1.getY(), P2.getY(), P3.getY());
	}


	// ******************** Methods
	// *******************************************
	public Point2D q(final double T) {
		return new Point2D(splineXValues.q(T), splineYValues.q(T));
	}


	// ******************** Inner Classes
	// *************************************
	class CatmullRomSpline {
		private double	p0;
		private double	p1;
		private double	p2;
		private double	p3;


		// ******************** Constructors
		// **************************************
		protected CatmullRomSpline(final double P0, final double P1, final double P2, final double P3) {
			p0 = P0;
			p1 = P1;
			p2 = P2;
			p3 = P3;
		}


		// ******************** Methods
		// *******************************************
		protected double q(final double T) {
			return 0.5 * ((2 * p1) + (p2 - p0) * T + (2 * p0 - 5 * p1 + 4 * p2 - p3) * T * T + (3 * p1 - p0 - 3 * p2 + p3) * T * T * T);
		}
	}
}