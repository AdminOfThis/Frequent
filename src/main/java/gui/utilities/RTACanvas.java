package gui.utilities;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.adminofthis.util.gui.FXMLUtil;

import control.ASIOController;
import control.FFT;
import data.Channel;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import main.Constants;

public class RTACanvas extends Canvas implements PausableComponent {

	private static final Logger LOG = LogManager.getLogger(RTACanvas.class);
	private static final WritablePixelFormat<IntBuffer> PIXEL_FORMAT = PixelFormat.getIntArgbPreInstance();
	private int count = 0;
	private GraphicsContext content;
	private boolean pause = false;
	private Pausable pausableParent;

	public RTACanvas(final ScrollPane parent) {
		content = getGraphicsContext2D();
		widthProperty().bind(parent.widthProperty());
		widthProperty().addListener(e -> reset());
		setHeight(1.0);
	}

	@Override
	public boolean isPaused() {
		return pause || (pausableParent != null && pausableParent.isPaused());
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

	public void reset() {
		content.clearRect(0, 0, getWidth(), getHeight());
		count = 0;
		setHeight(10);
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		pausableParent = parent;
	}

	public void addLine(final float[] map) {
		if (!isPaused()) {
			setHeight(getHeight() + 1);
// adding points
			List<Color> baseColors = createBaseColorList(map);

			drawDots(baseColors);
			count++;
		}
	}

	private ArrayList<Color> createBaseColorList(final float[] map) {
		ArrayList<Color> result = new ArrayList<>();
		for (int i = 0; i < map.length; i++) {
			double value = map[i];
			double percent = percentFromRawValue(value);
			double frequency = FFT.getFrequencyForIndex(i, map.length, ASIOController.getInstance().getSampleRate());
			if (frequency >= 5 && frequency <= 20000) {
				result.add(FXMLUtil.colorFade(percent, Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED));
			}
		}
		return result;
	}

	private void drawDots(final List<Color> list) {
		if (!list.isEmpty()) {
			PixelWriter p = content.getPixelWriter();
			int width = (int) Math.floor(getWidth());
			if (width > 0) {
				int[] buffer = new int[width];
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

	private int toInt(Color c) {
		return (255 << 24) | ((int) (c.getRed() * 255) << 16) | ((int) (c.getGreen() * 255) << 8) | ((int) (c.getBlue() * 255));
	}
}