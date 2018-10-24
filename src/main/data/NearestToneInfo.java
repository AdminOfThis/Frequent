/* To change this license header, choose License Headers in Project Properties. To change this template file, choose Tools | Templates and open the template in the editor. */
package data;

/**
 *
 * @author Matchos
 */
public class NearestToneInfo {

	private static final int	octaves					= 8;
	private static final int	halftonesPerScale		= 12;
	private static final double	referentialFrequency	= 440.0;
	private static int[][]		halftoneDiffs;
	private static double[][]	allTonesFreqs;
	private static String[][]	allTonesNames;
	private String				name;
	private double				freq;
	private double				deviation;
	private double				deviationInCents;
	private boolean				isDeviationPositive;
	private int					idx1;							// octave
	private int					idx2;							// halftoneDiff

	public NearestToneInfo(String name, double f, double d, double dc, boolean isPositive, int i1, int i2) {
		this.name = name;
		this.freq = f;
		this.deviation = d;
		this.deviationInCents = dc;
		this.isDeviationPositive = isPositive;
		this.idx1 = i1;
		this.idx2 = i2;
	}

	public String getName() {
		return this.name;
	}

	public double getFreq() {
		return this.freq;
	}

	public double getDeviation() {
		return this.deviation;
	}

	public double getDeviationInCents() {
		return this.deviationInCents;
	}

	public boolean isDeviationPositive() {
		return this.isDeviationPositive;
	}

	public int getIndex1() {
		return this.idx1;
	}

	public int getIndex2() {
		return this.idx2;
	}

	public static NearestToneInfo findNearestTone(double freq) {
		initIfNeeded();
		double distance = Math.abs(allTonesFreqs[0][0] - freq);
		double distanceInCents = 0;
		int idx1 = 0;
		int idx2 = 0;
		for (int i = 0; i < allTonesFreqs.length; i++) {
			for (int j = 0; j < allTonesFreqs[0].length; j++) {
				double cdistance = Math.abs(allTonesFreqs[i][j] - freq);
				if (cdistance < distance) {
					idx1 = i;
					idx2 = j;
					distance = cdistance;
				}
			}
		}
		boolean positive;
		// positive
		if ((freq - allTonesFreqs[idx1][idx2]) >= 0) {
			positive = true;
			// cent is one hundredth of semitone
			// distanceInCents = 100 * distanceInHertzs / semitoneSizeInHertzs;
			distanceInCents = 100 * (distance) / (allTonesFreqs[idx1][idx2] * (Math.pow(2, (1 / 12.0))) - allTonesFreqs[idx1][idx2]);
		}
		// negative
		else {
			positive = false;
			distanceInCents = 100 * (distance) / (allTonesFreqs[idx1][idx2] - allTonesFreqs[idx1][idx2] / (Math.pow(2, (1 / 12.0))));
		}
		NearestToneInfo nearest = new NearestToneInfo(allTonesNames[idx1][idx2], allTonesFreqs[idx1][idx2], distance, distanceInCents, positive, idx1, idx2);
		return nearest;
	}

	public static double[][] getAllTonesFreqsRelativeToRefFreq() {
		double[][] freqs = new double[octaves][halftonesPerScale];
		int[][] n = getHalftonesDiffsToA4();
		for (int octave = 0; octave < octaves; octave++) {
			for (int i = 0; i < halftonesPerScale; i++) {
				freqs[octave][i] = referentialFrequency * (Math.pow(2, (n[octave][i] / 12.0)));
			}
		}
		return freqs;
	}

	public static String[][] getAllTones() {
		String[][] names = new String[octaves][halftonesPerScale];
		names[0] = new String[] { "C0", "C#0", "D0", "D#0", "E0", "F0", "F#0", "G0", "G#0", "A0", "A#0", "B0" };
		names[1] = new String[] { "C1", "C#1", "D1", "D#1", "E1", "F1", "F#1", "G1", "G#1", "A1", "A#1", "B1" };
		names[2] = new String[] { "C2", "C#2", "D2", "D#2", "E2", "F2", "F#2", "G2", "G#2", "A2", "A#2", "B2" };
		names[3] = new String[] { "C3", "C#3", "D3", "D#3", "E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3" };
		names[4] = new String[] { "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4" };
		names[5] = new String[] { "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5", "G#5", "A5", "A#5", "B5" };
		names[6] = new String[] { "C6", "C#6", "D6", "D#6", "E6", "F6", "F#6", "G6", "G#6", "A6", "A#6", "B6" };
		names[7] = new String[] { "C7", "C#7", "D7", "D#7", "E7", "F7", "F#7", "G7", "G#7", "A7", "A#7", "B7" };
		return names;
	}

	public static int[][] getHalftonesDiffsToA4() {
		int[][] count = new int[octaves][halftonesPerScale];
		count[0] = new int[] { -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46 };
		count[1] = new int[] { -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34 };
		count[2] = new int[] { -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22 };
		count[3] = new int[] { -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10 };
		count[4] = new int[] { -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2 };
		count[5] = new int[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		count[6] = new int[] { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
		count[7] = new int[] { 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38 };
		return count;
	}

	private static void initIfNeeded() {
		if (allTonesNames == null) {
			allTonesNames = getAllTones();
		}
		if (halftoneDiffs == null) {
			halftoneDiffs = getHalftonesDiffsToA4();
		}
		if (allTonesFreqs == null) {
			allTonesFreqs = getAllTonesFreqsRelativeToRefFreq();
		}
	}
}
