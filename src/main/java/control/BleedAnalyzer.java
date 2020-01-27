package control;

import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.Channel;
import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;

public class BleedAnalyzer extends Thread implements PausableComponent, ChannelListener {

	private static final Logger LOG = LogManager.getLogger(BleedAnalyzer.class);
	private static final double MIN_CONFIDENCE = .5;
	private boolean pause = false;
	private Pausable parent;
	private Channel primaryChannel, secondaryChannel;
	// data
	private float[] newData1 = new float[ASIOController.getInstance().getBufferSize()];
	private float[] newData2 = new float[ASIOController.getInstance().getBufferSize()];
	private float[] originalSeries = new float[ASIOController.getInstance().getBufferSize()];
	private float[] otherSeries = new float[ASIOController.getInstance().getBufferSize()];
	private double minDiff = Double.MAX_VALUE;
	private double confidence;
	private int delay = 0;
	private int shift = 0;
	private double multi;
	private double equal;

	public BleedAnalyzer() {
		LOG.info("New BleedAnalyzer");
		newData1 = new float[ASIOController.getInstance().getSampleRate() / 8];
		newData2 = new float[ASIOController.getInstance().getSampleRate() / 8];
		originalSeries = new float[ASIOController.getInstance().getSampleRate() / 8];
		otherSeries = new float[ASIOController.getInstance().getSampleRate() / 8];
		startSubtractor();
		start();
	}

	@Override
	public void colorChanged(String newColor) {}

	public double getEqual() {
		return equal;
	}

	public float[] getNewData1() {
		return newData1;
	}

	public float[] getNewData2() {
		return newData2;
	}

	public Channel getPrimaryChannel() {
		return primaryChannel;
	}

	public Channel getSecondaryChannel() {
		return secondaryChannel;
	}

	@Override
	public boolean isPaused() {
		return pause || (parent != null && parent.isPaused()) || primaryChannel == null || secondaryChannel == null || Objects.equals(primaryChannel, secondaryChannel);
	}

	@Override
	public void levelChanged(Input input, double level, long time) {
		if (!Objects.equals(input, primaryChannel) && !Objects.equals(input, secondaryChannel)) {
			LOG.warn("Had to remove Channel listener by force");
			input.removeListener(this);
		}
	}

	@Override
	public void nameChanged(String name) {}

	@Override
	public void newBuffer(Channel channel, float[] buffer, long time) {
		if (Objects.equals(channel, primaryChannel)) {
			for (int i = 0; i < newData1.length - ASIOController.DESIRED_BUFFER_SIZE; i++) {
				newData1[i] = newData1[i + ASIOController.DESIRED_BUFFER_SIZE];
			}
			for (int i = 0; i < buffer.length; i++) {
				newData1[newData1.length - ASIOController.DESIRED_BUFFER_SIZE + i] = buffer[i];
			}
		} else if (Objects.equals(channel, secondaryChannel)) {
			for (int i = 0; i < newData2.length - ASIOController.DESIRED_BUFFER_SIZE; i++) {
				newData2[i] = newData2[i + ASIOController.DESIRED_BUFFER_SIZE];
			}
			for (int i = 0; i < buffer.length; i++) {
				newData2[newData2.length - ASIOController.DESIRED_BUFFER_SIZE + i] = buffer[i];
			}
		} else {
			LOG.warn("Had to remove Channel listener by force");
			channel.removeListener(this);
		}
	}

	@Override
	public void pause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public void run() {
		while (true) {
			if (isPaused()) {
				Thread.yield();
			} else {
				originalSeries = newData1;
				otherSeries = newData2;
				minDiff = Double.MAX_VALUE;
				shift = 0;
				for (int i = 0; i < Math.min(originalSeries.length, otherSeries.length) / 4; i++) {
					if (Math.min(originalSeries.length, otherSeries.length) < 100) {
						break;
					}
					shift++;
					double confidence = getLatency(i);
					if (confidence > .9) {
						break;
					}
				}
			}
		}
	}

	@Override
	public void setParentPausable(Pausable parent) {
		this.parent = parent;
	}

	public void setPrimaryChannel(Channel primaryChannel) {
		if (this.primaryChannel != null) {
			this.primaryChannel.removeListener(this);
		}
		this.primaryChannel = primaryChannel;
		if (this.primaryChannel != null) {
			this.primaryChannel.addListener(this);
		}
	}

	public void setSecondaryChannel(Channel secondaryChannel) {
		if (this.secondaryChannel != null) {
			this.secondaryChannel.removeListener(this);
		}
		this.secondaryChannel = secondaryChannel;
		if (this.secondaryChannel != null) {
			this.secondaryChannel.addListener(this);
		}
	}

	private double getLatency(int from) {
		double verhaltTot = 0.0;
		int count = 0;
		for (int i = 0; i < Math.min(originalSeries.length, otherSeries.length) - 1 - from; i++) {
			if (otherSeries[i + from] != 0) {
				count++;
				double verhaeltnis = originalSeries[i] / otherSeries[i + from];
				verhaltTot += verhaeltnis;
			}
		}
		verhaltTot = verhaltTot / count;
		if (verhaltTot > .0) {
// System.out.println(verhaltTot);
			double diffTot = 0;
			double total = 0;
			for (int i = 0; i < Math.min(originalSeries.length, otherSeries.length) - 1 - from; i++) {
				float orig = originalSeries[i];
				float other = otherSeries[i + from];
				double diff = orig - verhaltTot * other;
				diffTot += Math.abs(diff);
				total += Math.abs(orig);
			}
// System.out.println("Confidence: " + conficence);
// Diff: diffTot
			double tempCof = 1.0 - (diffTot / total);
			tempCof = Math.max(.0, tempCof);
			confidence -= 0.000001;
			if (Math.abs(diffTot) < Math.abs(minDiff) && tempCof > .5 && tempCof > confidence) {
				multi = verhaltTot;
				confidence = tempCof;
				minDiff = diffTot;
				delay = shift;
			}
			return tempCof;
		}
		return 0.0;
	}

	private void startSubtractor() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					if (confidence > MIN_CONFIDENCE && !isPaused()) {
						float[] original = Arrays.copyOfRange(originalSeries, 0, originalSeries.length - delay);
						float[] other = Arrays.copyOfRange(otherSeries, delay, otherSeries.length);
						float diffTotal = 0f;
						for (int i = 0; i < Math.min(original.length, other.length); i++) {
							diffTotal += Math.abs(multi * other[i]) - Math.abs(original[i]);
						}
						diffTotal = diffTotal / Math.min(original.length, other.length);
						equal = 1.0 - diffTotal;
					} else {
						Thread.yield();
					}
				}
			}
		}).start();
	}
}
