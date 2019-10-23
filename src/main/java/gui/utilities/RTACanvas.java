package gui.utilities;

import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.Channel;
import data.RTAIO;
import gui.FXMLUtil;
import gui.controller.MainController;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import main.Constants;

public class RTACanvas extends Canvas implements PausableComponent {

	private static final Logger LOG = LogManager.getLogger(RTACanvas.class);
	private static final WritablePixelFormat<IntBuffer> PIXEL_FORMAT = PixelFormat.getIntArgbPreInstance();
	private int count = 0;
	private GraphicsContext content;
	private boolean pause = true;
	private boolean exporting = false;
	private Pausable pausableParent;

	private RTACanvas() {
		content = getGraphicsContext2D();
	}

	private RTACanvas(final double width, final double heigth) {
		this();
		setWidth(width);
		setHeight(heigth);
	}

	public RTACanvas(final ScrollPane parent) {
		this();
		widthProperty().bind(parent.widthProperty());
		widthProperty().addListener(e -> reset());
		setHeight(1.0);
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
// double size = (getWidth() / (map[1].length * MICROSTEPS));
// if (getHeight() < size * count + size) {
			setHeight(getHeight() + 1);
// }
// long sizeTime = System.currentTimeMillis();
// adding points
			ArrayList<Color> baseColors = createBaseColorList(map[1]);
// long creatTime = System.currentTimeMillis();
			drawDots(baseColors);
// long drawTime = System.currentTimeMillis();
//
			count++;
// long after = System.currentTimeMillis();
// System.out.println("Size: " + (sizeTime - before) + " Create: " + (creatTime - sizeTime) + " Draw: " + (drawTime - creatTime) + " After: " + (after - before));
		}
	}

	private void drawDots(final ArrayList<Color> list) {
		if (!list.isEmpty()) {
			// System.out.println(map[1][pointCount]);
			PixelWriter p = content.getPixelWriter();
			int width = (int) Math.floor(getWidth());
			int[] buffer = new int[width];
// for (int i = 0; i < (list.size() - 1) * MICROSTEPS; i++) {
// Color baseColor = list.get(Math.floorDiv(i, MICROSTEPS));
// Color targetColor = list.get(Math.floorDiv(i, MICROSTEPS) + 1);
// double percent = (i % MICROSTEPS) / (double) MICROSTEPS;
// Color resultColor = FXMLUtil.colorFade(percent, baseColor, targetColor);
//// drawing using pixelWriter
// buffer[i] = toInt(resultColor);
// }
			for (int i = 0; i < width; i++) {
				int index = (int) Math.round(i * ((list.size() - 1) / getWidth()));
				Color baseColor = list.get(index);
// drawing using pixelWriter
				buffer[i] = toInt(baseColor);
			}
			try {
				p.setPixels(0, count, width, 1, PIXEL_FORMAT, buffer, 0, buffer.length - 1);
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}

	private ArrayList<Color> createBaseColorList(final double[] map) {
		ArrayList<Color> result = new ArrayList<>();
		for (double value : map) {
			double percent = percentFromRawValue(value);
			result.add(FXMLUtil.colorFade(percent, Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED));
		}
		return result;
	}

	private double percentFromRawValue(final double raw) {
		double level = Math.abs(raw);
		level = Channel.percentToDB(level);
		level = Math.abs(level);
		if (level >= Math.abs(Constants.FFT_MIN)) {
			level = Math.abs(Constants.FFT_MIN) - 1;
		}
		double percent = (Math.abs(Constants.FFT_MIN) - Math.abs(level)) / Math.abs(Constants.FFT_MIN);
		return percent;
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

	private RTACanvas recreateCanvas() {
		RTACanvas printCanvas = new RTACanvas(getWidth(), 1);
		reset();
		ArrayList<double[][]> list = RTAIO.readFile();
		for (double[][] entry : list) {
			printCanvas.addLine(entry, true);
		}
		return printCanvas;
	}

	public void reset() {
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
					RTACanvas canvas = recreateCanvas();
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

	private int toInt(Color c) {
		return (255 << 24) | ((int) (c.getRed() * 255) << 16) | ((int) (c.getGreen() * 255) << 8) | ((int) (c.getBlue() * 255));
	}
}