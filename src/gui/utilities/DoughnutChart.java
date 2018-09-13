package gui.utilities;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

public class DoughnutChart extends PieChart {

	private static final double	CIRCLE_PERCENT	= 0.6;
	private final Circle		innerCircle;

	public DoughnutChart(ObservableList<Data> pieData) {
		super(pieData);
		innerCircle = new Circle();
		innerCircle.setStyle("-fx-fill: -fx-background");
		setLabelsVisible(false);
		setStartAngle(90);
		setClockwise(false);
		setLegendSide(Side.TOP);
	}

	@Override
	protected void layoutChartChildren(double top, double left, double contentWidth, double contentHeight) {
		super.layoutChartChildren(top, left, contentWidth, contentHeight);

		addInnerCircleIfNotPresent();
		updateInnerCircleLayout();

	}

	private void addInnerCircleIfNotPresent() {
		if (getData().size() > 0) {
			Node pie = getData().get(0).getNode();
			if (pie.getParent() instanceof Pane) {
				Pane parent = (Pane) pie.getParent();

				if (!parent.getChildren().contains(innerCircle)) {
					parent.getChildren().add(innerCircle);

				} else {
					innerCircle.toFront();
				}
			}
		}
	}

	private void updateInnerCircleLayout() {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
		for (PieChart.Data data : getData()) {
			Node node = data.getNode();

			Bounds bounds = node.getBoundsInParent();
			if (bounds.getMinX() < minX) {
				minX = bounds.getMinX();
			}
			if (bounds.getMinY() < minY) {
				minY = bounds.getMinY();
			}
			if (bounds.getMaxX() > maxX) {
				maxX = bounds.getMaxX();
			}
			if (bounds.getMaxY() > maxY) {
				maxY = bounds.getMaxY();
			}
		}

		innerCircle.setCenterX(minX + (maxX - minX) / 2);
		innerCircle.setCenterY(minY + (maxY - minY) / 2);

		innerCircle.setRadius((maxX - minX) * CIRCLE_PERCENT / 2);
	}
}