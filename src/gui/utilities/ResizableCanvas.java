package gui.utilities;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ResizableCanvas extends Canvas {
int count = 0;
private static final int POINTS = 100;
	
	public ResizableCanvas() {
		Timeline line = new Timeline();
		line.getKeyFrames().add(new KeyFrame(Duration.seconds(5), e -> {
			GraphicsContext content = getGraphicsContext2D();
			for (int i = 0; i < POINTS; i++) {
				String r = Integer.toHexString((int) Math.round(Math.random() * 255.0));
				if (r.length() < 2) {
					r = "0" + r;
				}
				String color = "#" + r + "3333";
				content.setFill(Color.web(color));
				content.fillRect((getWidth() / POINTS) * i, 10*count, 10, 10);
			}
			count ++;
		}));
		line.setCycleCount(Timeline.INDEFINITE);
		line.playFromStart();
	}

	@Override
	public double minHeight(double width) {
		return 64;
	}

	@Override
	public double maxHeight(double width) {
		return 1000;
	}

	@Override
	public double prefHeight(double width) {
		return minHeight(width);
	}

	@Override
	public double minWidth(double height) {
		return 0;
	}

	@Override
	public double maxWidth(double height) {
		return 10000;
	}

	@Override
	public boolean isResizable() {
		return true;
	}

}
