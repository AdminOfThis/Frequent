package control.bpmdetect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author https://github.com/widget-/bpm-detect
 *
 */
public final class BPMBestGuess {

	private static double DECAY_RATE = 0.999;
	private static double DELETE_THRESHHOLD = 0.01;
	private static BPMBestGuess instance;
	private Map<Double, Double> bpmEntries = Collections.synchronizedMap(new HashMap<Double, Double>());
	private double confidence = 0;
	private long lastCalc;
	private double bpm;

	private BPMBestGuess() {
	}

	/**
	 * Returns the singleton isntance of {@link BPMBestGuess}
	 * 
	 * @return instance THe singleton instance
	 */
	public static BPMBestGuess getInstance() {
		if (instance == null) {
			instance = new BPMBestGuess();
		}
		return instance;
	}

	/**
	 * The BOM of the beat pattern given
	 * @return bpm The BPM which the algorithm detected
	 */
	public double getBPM() {
		long time = System.currentTimeMillis();
		if (time - lastCalc > 1000) {
			// calc new every second
			bpm = calculateGuess();
			lastCalc = time;
		}
		return bpm;
	}

	/**
	 * Return the confidence, with which the algorithm detected a given BPM
	 * @return confidence, the confidence of the detection
	 */
	public double getConfidence() {
		return this.confidence;
	}

	private double calculateGuess() {
		double bestGuessStart = 0;
		double bestGuessValue = 0;
		// Entry<Double, Double> lastEntry = new SimpleImmutableEntry<Double,
		// Double>(0d, 0d);
		// for (Entry<Double, Double> e : bpmEntries.entrySet()) {
		// if (rising)
		// if (lastEntry.getValue() < e.getValue())
		// currentGuessValue += e.getValue();
		// else
		// rising = false;
		// else
		// if (lastEntry.getValue() > e.getValue())
		// currentGuessValue += e.getValue();
		// else {
		// rising = true;
		// if (currentGuessValue > bestGuessValue)
		// bestGuessValue = currentGuessValue;
		// bestGuessStart = currentGuessStart;
		// currentGuessStart = e.getKey();
		// }
		// }
		synchronized (bpmEntries) {
			for (Entry<Double, Double> e : bpmEntries.entrySet()) {
				if (e.getValue() > bestGuessValue) {
					bestGuessStart = e.getKey();
					bestGuessValue = e.getValue();
				}
			}
		}
		// System.out.println("---------------------");
		confidence = bestGuessValue;
		return bestGuessStart;
	}

	void appendBPMGuess(double bpm, double confidence) {
		if (bpm > 0) {
			synchronized (bpmEntries) {
				Iterator<Entry<Double, Double>> it = bpmEntries.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Double, Double> e = it.next();
					e.setValue(e.getValue() * DECAY_RATE);
					if (e.getValue() < DELETE_THRESHHOLD) {
						it.remove();
					}
				}
				if (bpmEntries.containsKey(bpm)) {
					bpmEntries.put(bpm, bpmEntries.get(bpm) + confidence);
				} else {
					bpmEntries.put(bpm, confidence);
				}
			}
		}
	}
}
