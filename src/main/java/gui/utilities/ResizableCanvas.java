package gui.utilities;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import data.Channel;
import data.RTAIO;
import gui.controller.MainController;
import gui.controller.RTAViewController;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ResizableCanvas extends Canvas implements PausableComponent {

	private static final Logger	LOG			= Logger.getLogger(ResizableCanvas.class);
	private static final int	POINTS		= 1024;
	private int					count		= 0;
	private boolean				autoscroll	= true;
	private GraphicsContext		content;
	private ScrollPane			parent;
	private boolean				pause		= true;
	private boolean				exporting	= false;
	private Pausable			pausableParent;

	private ResizableCanvas() {
		content = getGraphicsContext2D();
	}

	private ResizableCanvas(final double width, final double heigth) {
		this();
		setWidth(width);
		setHeight(heigth);
	}

	public ResizableCanvas(final ScrollPane parent) {
		this();
		this.parent = parent;
		widthProperty().bind(parent.widthProperty());
		widthProperty().addListener(e -> reset());
		setHeight(10.0);
	}

	public void addLine(final double[][] map) {
		if (!exporting) {
			addLine(map, false);
		}
	}

	private void addLine(final double[][] map, final boolean toExport) {
		if (!isPaused() || toExport) {
			if (!toExport) {
				RTAIO.writeToFile(map);
			}
			// long before = System.currentTimeMillis();
			double size = getWidth() / POINTS;
			if (getHeight() < size * count + size) {
				setHeight(getHeight() + size);
			}
			// adding points
			for (int pointCount = 0; pointCount < map[0].length; pointCount++) {
				// System.out.println(map[1][pointCount]);
				double level = Math.abs(map[1][pointCount]);
				level = Channel.percentToDB(level * 1000.0);
				if (level <= RTAViewController.FFT_MIN) {
					level = RTAViewController.FFT_MIN + 1;
				} else if (level >= Math.abs(RTAViewController.FFT_MIN)) {
					level = Math.abs(RTAViewController.FFT_MIN) - 1;
				}
				double percent = (Math.abs(RTAViewController.FFT_MIN) - Math.abs(level))
				        / Math.abs(RTAViewController.FFT_MIN);
				percent = 1.0 - percent;
				try {
					content.setFill(
					        FXMLUtil.colorFade(percent, Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED));
				} catch (ArrayIndexOutOfBoundsException e) {
					LOG.error("Out of bounds: " + level, e);
				}
				double startPoint = getWidth() / 2000.0 * map[0][pointCount];
				// System.out.println(startPoint);
				double endpoint;
				if (pointCount < map[0].length - 1) {
					endpoint = getWidth() / 2000.0 * map[0][pointCount + 1];
				} else {
					endpoint = getWidth();
				}
				content.fillRect(startPoint, size * count, endpoint - startPoint + 1, size + 1);
			}
			//
			count++;
			if (count > 5000 && !toExport) {
				reset();
			}
			if (autoscroll && parent != null) {
				parent.setVvalue(parent.getVmax());
			}
			// long after = System.currentTimeMillis();
			// System.out.println(after - before);
		}
	}

	@Override
	public boolean isPaused() {
		return pause || pausableParent != null && pausableParent.isPaused();
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public void pause(final boolean pause) {
		if (this.pause != pause) {
			this.pause = pause;
			if (!pause) {
				reset();
				RTAIO.deleteFile();
			}
		}
	}

	@Override
	public double prefHeight(final double width) {
		return getHeight();
	}

	@Override
	public double prefWidth(final double height) {
		return getWidth();
	}

	private ResizableCanvas recreateCanvas() {
		ResizableCanvas printCanvas = new ResizableCanvas(getWidth(), getHeight());
		reset();
		ArrayList<double[][]> list = RTAIO.readFile();
		for (double[][] entry : list) {
			printCanvas.addLine(entry, true);
		}
		return printCanvas;
	}

	private void reset() {
		content.clearRect(0, 0, getWidth(), getHeight());
		count = 0;
		setHeight(10);
	}

	public void save(final File file) {
		final Task<Boolean> task = new Task<Boolean>() {

			@Override
			public Boolean call() {
				MainController.getInstance().setStatus("Exporting RTA");
				try {
					ResizableCanvas canvas = recreateCanvas();
					SnapshotParameters params = new SnapshotParameters();
					params.setFill(Color.web(FXMLUtil.getStyleValue("-fx-base")));
					Platform.runLater(() -> {
						try {
							WritableImage image = canvas.snapshot(params, null);
							RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
							ImageIO.write(renderedImage, "png", file);
						} catch (Exception e) {
							LOG.warn("Unable to export image", e);
						} finally {
							MainController.getInstance().resetStatus();
						}
					});
				} catch (Exception ex) {
					LOG.warn("Unable to export image", ex);
					MainController.getInstance().resetStatus();
				}
				return true;
			}
		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		pausableParent = parent;
	}
}