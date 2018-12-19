package control.bpmdetect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 
 * @author https://github.com/widget-/bpm-detect
 *
 */
public final class BPMBestGuess {

	private HashMap<Double, Double>	bpmEntries			= new HashMap<>();
	private static double			DECAY_RATE			= 0.999;
	private static double			DELETE_THRESHHOLD	= 0.01;
	private double					confidence			= 0;
	private static BPMBestGuess		instance;

	private BPMBestGuess() {
	}

	public static BPMBestGuess getInstance() {
		if (instance == null) {
			instance = new BPMBestGuess();
		}
		return instance;
	}

	public void appendBPMGuess(double bpm, double confidence) {
		Iterator<Entry<Double, Double>> it = bpmEntries.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Double, Double> e = it.next();
			e.setValue(e.getValue() * DECAY_RATE);
			if (e.getValue() < DELETE_THRESHHOLD)
				it.remove();
		}
		if (bpmEntries.containsKey(bpm)) {
			bpmEntries.put(bpm, bpmEntries.get(bpm) + confidence);
		} else {
			bpmEntries.put(bpm, confidence);
		}
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
		for (Entry<Double, Double> e : bpmEntries.entrySet()) {
			// System.out.println(e.getKey()+"\t"+e.getValue());
			if (e.getValue() > bestGuessValue) {
				bestGuessStart = e.getKey();
				bestGuessValue = e.getValue();
			}
		}
		// System.out.println("---------------------");
		confidence = bestGuessValue;
		return bestGuessStart;
	}

	public double getConfidence() {
		return this.confidence;
	}

	public double getBPM() {
		return calculateGuess();
	}
}
