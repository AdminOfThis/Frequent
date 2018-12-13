package control.bpmdetect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * 
 * 
 * @author https://github.com/widget-/bpm-detect
 *
 */
public abstract class BPMDetect {

	public static final int		sampleRate		= 500;
	public static final int		NUM_FLAGS		= 8;
	// public static Multimap<Double, Integer> reps = TreeMultimap.create();
	public static List<Integer>	flags			= new ArrayList<>();
	public static double		bpm				= -1;
	public static double		confidence		= 0;
	private static int			ramp			= sampleRate / 60;
	public static double		rampRequired	= 0.35;
	private static int			rampSeperation	= ramp * 5;
	private static long			downbeatTime	= System.currentTimeMillis();

	public static double detectBPM(float[] samples, int sampleRate) {
		int crossoverSize = sampleRate * 60 / 130 * 4; // 4 beats at 130BPM
		if (crossoverSize > sampleRate * 10.0) return -1; // whoops
		float max = getMaxInArray(samples);
		int lastBeat = 0;
		for (int i = 1; i < samples.length; i++) {
			if ((samples[i] - samples[i - 1]) / max > rampRequired) {
				if (i - lastBeat < rampSeperation) continue;
				else lastBeat = i;
			}
		}
		for (int i = samples.length - (int) (sampleRate * 0.3); i < samples.length - 1; i++) {}
		for (int i = samples.length - sampleRate * 5; i < samples.length - 1; i++) {}
		bpm = bpmFromFlags(sampleRate);
		if (bpm > 0) {
			while (bpm > 180)
				bpm /= 2.0;
			while (bpm < 90)
				bpm *= 2.0;
		}
		BPMBestGuess.getInstance().appendBPMGuess(bpm, confidence);
		return bpm;
	}

	public static double bpmFromFlags(int sampleRate) {
		ArrayList<Integer> distances = new ArrayList<>();
		for (int i = 0; i < flags.size(); i++)
			for (int j = i + 1; j < flags.size(); j++) {
				int distance = Math.abs(flags.get(i) - flags.get(j));
				// while (distance > (sampleRate * 60 / 180))
				// distance /= 2;
				distances.add(distance);
				// System.out.print(distance+"|");
			}
		HashMap<Integer, Double> distanceMap = new HashMap<>();
		for (int distance : distances) {
			for (int i = -2; i < 3; i++)
				if (distanceMap.containsKey(distance + i)) distanceMap.put(distance + i, distanceMap.get(distance + i) + 1 - Math.abs(i) / 4.0);
				else distanceMap.put(distance + i, 1 - Math.abs(i) / 4.0);
			// System.out.print(distance+" ");
		}
		// System.out.println(distanceMap.size());
		Pair<Integer, Double> bestGuess = new Pair<>(-1, -1.0);
		// Pair<Integer, Double> nextBestGuess = new Pair<Integer, Double>(-1,
		// -1.0);
		int scoreTotal = 0;
		for (Entry<Integer, Double> e : distanceMap.entrySet()) {
			if (e.getValue() > bestGuess.y) {
				// nextBestGuess = bestGuess;
				bestGuess = new Pair<>(e.getKey(), e.getValue());
				scoreTotal += e.getValue();
			}
			// System.out.print("K: "+e.getKey()+",V:"+e.getValue()+". ");
		}
		// System.out.println();
		// confidence[sampleType] = 15.0 / Math.min(15, flags.size()) * 100;
		// confidence[sampleType] = ((bestGuess.y / nextBestGuess.y)) * 50;
		// confidence[sampleType] = ((bestGuess.y / nextBestGuess.y));
		confidence = bestGuess.y / scoreTotal * 100;
		confidence = Math.min((confidence - 10) * (100f / 25), 100);
		if (flags.size() == 0) confidence = 0;
		else if (flags.size() < 10) confidence *= flags.size() / 10;
		if (bestGuess.x == 0) return -1;
		else {
			double bpm = sampleRate * 60.0 / (bestGuess.x - 1);
			if (bpm <= 0) return bpm;
			while (bpm < 70)
				bpm *= 2.0;
			while (bpm > 180)
				bpm /= 2.0;
			return bpm;
		}
	}

	public static long getDownbeat(float[] samples, int sampleRate) {
		Pair<Integer, Float> bestGuess = new Pair<>(0, 0f);
		float[] sums = new float[samples.length - sampleRate];
		for (int i = 0; i < samples.length - sampleRate; i++)
			for (int j = 0; j < sampleRate; j++)
				sums[i] += samples[i + j];
		for (int i = 0; i < samples.length - 2 * sampleRate; i++)
			if (sums[i + sampleRate] - sums[i] > bestGuess.y) {
				bestGuess = new Pair<>(i + sampleRate, sums[i + sampleRate]);
			}
		if (sums[bestGuess.x - sampleRate] * 5 < sums[bestGuess.x] && Math.abs(downbeatTime - (System.currentTimeMillis() - (samples.length - bestGuess.x) * 1000 / sampleRate)) > 100) {
			downbeatTime = System.currentTimeMillis() - (samples.length - bestGuess.x) * 1000 / sampleRate;
		}
		return downbeatTime;
	}

	public static int getBeat() {
		double time = (System.currentTimeMillis() - downbeatTime);
		double bpm = BPMBestGuess.getInstance().getBPM();
		long beat = Math.round(bpm * time / 60000);
		return (int) beat % 32 + 1;
	}

	public static double getPhase() {
		double time = (System.currentTimeMillis() - downbeatTime);
		double bpm = BPMBestGuess.getInstance().getBPM();
		return (bpm * time / 60000) - Math.round(bpm * time / 60000) + 0.5d;
	}

	public static float[] filterSamplesDecay(float[] samples) {
		float[] s = new float[samples.length];
		s = samples.clone();
		for (int i = s.length - 1; i > 0; i--)
			for (int j = 0; j < 5; j++)
				if (i > 5 && s[i] < s[i - j]) s[i] = s[i - j];
		return s;
	}

	public static float[] dIntegral(float[] samples) {
		float[] s = new float[samples.length];
		s[0] = 0;
		for (int i = 1; i < s.length; i++)
			if (samples[i] - samples[i - 1] > 0) s[i] = samples[i] - samples[i - 1];
			else s[i] = 0;
		return s;
	}

	public static float[] filteredIntegral(float[] samples) {
		return dIntegral(filterSamplesDecay(samples));
	}

	public static float getMaxInArray(float[] array) {
		float max = 0;
		for (int i = 0; i < array.length; i++)
			if (array[i] > max) max = array[i];
		return max;
	}
}


class Pair<X, Y> {

	public final X	x;
	public final Y	y;

	public Pair(X x, Y y) {
		this.x = x;
		this.y = y;
	}
}
