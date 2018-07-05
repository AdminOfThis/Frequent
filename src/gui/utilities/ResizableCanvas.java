package gui.utilities;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import data.RTAIO;
import gui.controller.Pausable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ResizableCanvas extends Canvas implements Pausable {

	private static final Logger	LOG			= Logger.getLogger(ResizableCanvas.class);
	int							count		= 0;
	private static final int	POINTS		= 1024;

	private boolean				autoscroll	= true;
	private String				accent		= "#FF0000";
	private GraphicsContext		content;
	private ScrollPane			parent;
	private boolean				pause		= true;

	public ResizableCanvas(ScrollPane parent) {
		this.parent = parent;
		widthProperty().bind(parent.widthProperty());
		accent = FXMLUtil.getStyleValue("-fx-accent");
		content = getGraphicsContext2D();
		widthProperty().addListener(e -> reset());
		setHeight(10.0);
		Timeline line = new Timeline();
		line.getKeyFrames().add(new KeyFrame(Duration.millis(20), e -> {
			double[][] map = new double[2][POINTS];
			for(int i=0;i<map[1].length-1;i++) {
				map[1][i] = Math.random();
			}
			addLine(map);
		}));
		line.setCycleCount(Timeline.INDEFINITE);
		line.playFromStart();
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
		addLine(map, false);
	}

	private void addLine(double[][] map, boolean toExport) {

		if (!pause|| toExport) {
			
			RTAIO.writeToFile(map);
			// long before = System.currentTimeMillis();

			double size = (getWidth() / POINTS);

			if (getHeight() < size * count + size) {
				setHeight(getHeight() + size);
			}
			// adding points
			for (int pointCount = 0; pointCount < map[0].length; pointCount++) {
				
				content.setFill(Color.web(makeColorTransparent(accent, map[1][pointCount])));
				content.fillRect(size * pointCount, size * count, size, size);
			}
			//

			count++;
			if (count > 500 && !toExport) {
			 reset();
			 }

			if (autoscroll) {
				parent.setVvalue(parent.getVmax());
			}
			// long after = System.currentTimeMillis();
			// System.out.println(after - before);
		}
	}

	public void save(File file) {
		recreateCanvas();
		try {
			SnapshotParameters params = new SnapshotParameters();
			params.setFill(Color.web(FXMLUtil.getStyleValue("-fx-base")));
			WritableImage image = snapshot(params, null);
			RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
			ImageIO.write(renderedImage, "png", file);
		} catch (IOException ex) {
			LOG.warn("Unable to export image", ex);
		}

	}

	private void recreateCanvas() {
		reset();
		ArrayList<double[][]> list = RTAIO.readFile();
		for(double[][] entry : list) {
			addLine(entry, true);
		}

	}

	@Override
	public void pause(boolean pause) {
		if (this.pause != pause) {
			this.pause = pause;
			if (!pause) {
				reset();
				RTAIO.deleteFile();
			} else {
			}
		}
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		LOG.error("Uninplemented method called: addParentPausable");
	}
}