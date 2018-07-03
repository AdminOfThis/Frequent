package gui.utilities;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;

public class ResizableCanvas extends Canvas {
	int							count		= 0;
	private static final int	POINTS		= 1024;

	private boolean				autoscroll	= true;
	private String				accent		= "#FF0000";
	private GraphicsContext		content;
	private ScrollPane			parent;

	public ResizableCanvas(ScrollPane parent) {
		this.parent = parent;
		widthProperty().bind(parent.widthProperty());
		accent = FXMLUtil.getStyleValue("-fx-accent");
		content = getGraphicsContext2D();
		widthProperty().addListener(e -> reset());
		setHeight(10.0);
		parent.vvalueProperty().addListener((obs, oldV, newV) -> {
			if ((double) newV > 0.9 * parent.getVmax() || (double) newV == 0.0) {
				autoscroll = true;
			} else {
				autoscroll = false;
			}
		});
	}

	private void reset() {
		content.clearRect(0, 0, getWidth(), getHeight());
		count = 0;
		setHeight(10);
	}

	public static String makeColorTransparent(String color, double percent) {

		String transparency = Integer.toHexString((int) Math.floor(percent * 255.0));
		if (transparency.length() < 2) {
			transparency = "0" + transparency;
		}
		String bla = color + transparency.toUpperCase();
		return bla.trim();
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public double prefWidth(double height) {
		return getWidth();
	}

	@Override
	public double prefHeight(double width) {
		return getHeight();
	}

	public void addLine(double[][] map) {
		// long before = System.currentTimeMillis();

		double size = (getWidth() / POINTS);

		if (getHeight() < size * count + size) {
			setHeight(getHeight() + size);
		}
		// adding points
		int pointCount = 0;
		for (double[] entry : map) {
			String r = Integer.toHexString((int) Math.round(Math.random() * 255.0));
			if (r.length() < 2) {
				r = "0" + r;
			}
			content.setFill(Color.web(makeColorTransparent(accent, entry[1])));
			content.fillRect(size * pointCount, size * count, size, size);
			pointCount++;
		}
		//

		count++;
		if (count > 5000) {
			reset();
		}

		if (autoscroll) {
			parent.setVvalue(parent.getVmax());
		}
		// long after = System.currentTimeMillis();
		// System.out.println(after - before);
	}
}