package gui.utilities;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class ResizableCanvas extends Canvas {

	/** The Graphics Context 2D of the canvas */
	private final GraphicsContext gc = getGraphicsContext2D();

	/**
	 * Redraw the Canvas
	 */
	public ResizableCanvas() {
		widthProperty().addListener(e -> redraw());
	}

	public void redraw() {
		// System.out.println(" Real Canvas Width is:" + getWidth() + " , Real Canvas
		// Height is:" + getHeight() + "\n")
		gc.setLineWidth(3);
		gc.clearRect(0, 0, getWidth(), getHeight());
	}

	@Override
	public double minHeight(double width) {
		return 1;
	}

	@Override
	public double minWidth(double height) {
		return 1;
	}

	@Override
	public double prefWidth(double width) {
		return minWidth(width);
	}

	@Override
	public double prefHeight(double width) {
		return minHeight(width);
	}

	@Override
	public double maxWidth(double height) {
		return Double.MAX_VALUE;
	}

	@Override
	public double maxHeight(double width) {
		return Double.MAX_VALUE;
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public void resize(double width, double height) {
		super.setWidth(width);
		super.setHeight(height);
		// This is for testing...
		// draw()
		// System.out.println("Resize method called...")
	}
}