package gui.utilities;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import data.Channel;
import data.RTAIO;
import gui.controller.FFTController;
import gui.controller.MainController;
import gui.controller.Pausable;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ResizableCanvas extends Canvas implements Pausable {

	private static final Logger	LOG			= Logger.getLogger(ResizableCanvas.class);
	int							count		= 0;
	private static final int	POINTS		= 1024;

	private boolean				autoscroll	= true;
	private String				accent		= "#FF0000";
	private GraphicsContext		content;
	private ScrollPane			parent;
	private boolean				pause		= true;
	private boolean				exporting	= false;
	private Pausable			pausableParent;

	private ResizableCanvas(double width, double heigth) {
		this();
		setWidth(width);
		setHeight(heigth);
	}

	private ResizableCanvas() {
		accent = FXMLUtil.getStyleValue("-fx-accent");
		content = getGraphicsContext2D();
	}

	public ResizableCanvas(ScrollPane parent) {
		this();
		this.parent = parent;
		widthProperty().bind(parent.widthProperty());
		widthProperty().addListener(e -> reset());
		setHeight(10.0);
		// Timeline line = new Timeline();
		// line.getKeyFrames().add(new KeyFrame(Duration.millis(20), e -> {
		// double[][] map = new double[2][POINTS];
		// for (int i = 0; i < map[1].length - 1; i++) {
		// map[1][i] = Math.random();
		// }
		// addLine(map);
		// }));
		// line.setCycleCount(Timeline.INDEFINITE);
		// line.playFromStart();

	}

	private void reset() {
		content.clearRect(0, 0, getWidth(), getHeight());
		count = 0;
		setHeight(10);

	}

	public static String makeColorTransparent(String color, double db) {
		double percent = 1 - Math.abs(Math.max(FFTController.FFT_MIN, db)) / Math.abs(FFTController.FFT_MIN);
		// percent = percent *10;

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
		if (!exporting) {
			addLine(map, false);
		}
	}

	private void addLine(double[][] map, boolean toExport) {
		if (!pause || toExport) {
			if (!toExport) {
				RTAIO.writeToFile(map);
			}
			// long before = System.currentTimeMillis();

			double size = (getWidth() / POINTS);

			if (getHeight() < size * count + size) {
				setHeight(getHeight() + size);
			}
			// adding points
			for (int pointCount = 0; pointCount < map[0].length; pointCount++) {
				// System.out.println(map[1][pointCount]);
				content.setFill(Color.web(makeColorTransparent(accent, Channel.percentToDB(map[1][pointCount]))));
				double startPoint = getWidth()/2000.0 * map[0][pointCount];
//				System.out.println(startPoint);
				double endpoint;
				if(pointCount <map[0].length-1) {
					endpoint = getWidth()/2000.0 * map[0][pointCount+1];
				} else {
					endpoint = getWidth();
				}
				content.fillRect(startPoint, size * count, endpoint - startPoint, size);
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

	public void save(File file) {

		final Task<Boolean> task = new Task<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				MainController.getInstance().setStatus("Exporting RTA");
				try {
					ResizableCanvas canvas = recreateCanvas();
					SnapshotParameters params = new SnapshotParameters();
					params.setFill(Color.web(FXMLUtil.getStyleValue("-fx-base")));
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							try {
								WritableImage image = canvas.snapshot(params, null);
								RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
								ImageIO.write(renderedImage, "png", file);
							} catch (Exception e) {
								LOG.warn("Unable to export image", e);
							} finally {
								MainController.getInstance().resetStatus();
							}
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

	private ResizableCanvas recreateCanvas() {
		ResizableCanvas printCanvas = new ResizableCanvas(getWidth(), getHeight());
		reset();
		ArrayList<double[][]> list = RTAIO.readFile();
		for (double[][] entry : list) {
			printCanvas.addLine(entry, true);
		}
		return printCanvas;
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
		return pause || (pausableParent != null && pausableParent.isPaused());
	}

	@Override
	public void setParentPausable(Pausable parent) {
		pausableParent = parent;
	}
}