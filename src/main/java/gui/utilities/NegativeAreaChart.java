package gui.utilities;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

public class NegativeAreaChart extends AreaChart<Number, Number> {

	private static final int SUBDIVISION_POINTS = 6;

	public NegativeAreaChart(Axis<Number> xAxis, Axis<Number> yAxis) {
		super(xAxis, yAxis);
	}

	public static final Point2D[] subdividePoints(final Point2D[] POINTS, final int SUB_DEVISIONS) {
		assert POINTS != null;
		assert POINTS.length >= 3;
		int noOfPoints = POINTS.length;
		Point2D[] subdividedPoints = new Point2D[((noOfPoints - 1) * SUB_DEVISIONS) + 1];
		double increments = 1.0 / SUB_DEVISIONS;
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

	/**
	 * Returns an array of paths where the first entry represents the fill path and
	 * the second entry represents the stroke path
	 * 
	 * @param SERIES
	 * @return an array of paths where [0] == FillPath and [1] == StrokePath
	 */
	private Path[] getPaths(final Series<Number, Number> SERIES) {
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
		return new Path[] { /* FillPath */ (Path) (seriesGroup).getChildren().get(0), /* StrokePath */ (Path) (seriesGroup).getChildren().get(1) };
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
				final double x = line.getX();
				final double y = line.getY();
				dataPoints[i] = new Point2D(x, y);
			}
		}
		double firstX = dataPoints[0].getX();
		double lastX = dataPoints[dataPoints.length - 1].getX();
		Point2D[] points = subdividePoints(dataPoints, SUBDIVISION_POINTS);
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

	@Override
	protected void layoutPlotChildren() {
		super.layoutPlotChildren();
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
	}
}
