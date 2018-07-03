package gui.utilities;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.css.CssMetaData;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ResizableCanvas extends Canvas {
	int							count		= 0;
	private static final int	POINTS		= 1024;

	private boolean				autoscroll	= true;
	private String				accent = "#FF0000";

	public ResizableCanvas(ScrollPane parent) {
		accent = FXMLUtil.getStyleValue("-fx-accent");
		GraphicsContext content = getGraphicsContext2D();
		widthProperty().addListener(e -> reset());
		Timeline line = new Timeline();

		setHeight(10.0);
		line.getKeyFrames().add(new KeyFrame(Duration.millis(20), e -> {
			// long before = System.currentTimeMillis();
			double size = (getWidth() / POINTS);
			if (getHeight() < size * count + size) {
				setHeight(getHeight() + size);
			}
			for (int i = 0; i < POINTS; i++) {
				String r = Integer.toHexString((int) Math.round(Math.random() * 255.0));
				if (r.length() < 2) {
					r = "0" + r;
				}
				content.setFill(Color.web(makeColorTransparent(accent, Math.random())));
				content.fillRect(size * i, size * count, size, size);
			}
			count++;
			if (count > 5000) {
				reset();
			}

			parent.vvalueProperty().addListener((obs, oldV, newV) -> {
				if ((double) newV > 0.9 * parent.getVmax() || (double) newV == 0.0) {
					autoscroll = true;
				} else {
					autoscroll = false;
				}
			});
			if (autoscroll) {
				parent.setVvalue(parent.getVmax());
			}
			// long after = System.currentTimeMillis();
			// System.out.println(after - before);
		}));
		line.setCycleCount(Timeline.INDEFINITE);
		line.playFromStart();
	}

	private void reset() {
		GraphicsContext content = getGraphicsContext2D();
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
}