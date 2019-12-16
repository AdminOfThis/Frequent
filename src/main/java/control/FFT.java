package control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jtransforms.fft.FloatFFT_1D;

public final class FFT {

	private static final Logger LOG = LogManager.getLogger(FFT.class);

	private FFT() {}

	/**
	 * Code of FFT is mostly taken from github.com/akuehntopf
	 * https://gist.github.com/akuehntopf/4da9bced2cb88cfa2d19
	 * 
	 * @param output
	 */
	public static float[] fftThis(final float[] output, float sampleRate) {

		try {

			float[] windowed = applyWindow(output);

			// 3. Calculate Power Spectrum (using FFT)
			return powerSpectrum(windowed);

		} catch (Exception e) {
			LOG.info("Problem on FFT", e);
		}
		return null;
	}

	public static float[] applyWindow(float[] from) {
		float[] result = new float[from.length];
		for (int n = 0; n < from.length; n++) {
			result[n] = from[n] * getHammingValue(n, from.length);
		}
		return result;
	}

	/**
	 * Gets the next value of the hamming function.
	 *
	 * @param i    the index
	 * @param size the total size
	 * @return the hamming value
	 */
	private static float getHammingValue(int i, int size) {
		return (float) (0.54 - 0.46 * Math.cos((2 * Math.PI * i) / (size - 1)));
	}

	private static float[] powerSpectrum(float[] window) {
		float[] powerSpectrum = new float[window.length];
		float[] fftBuffer = new float[window.length * 2 + 1];
		System.arraycopy(window, 0, fftBuffer, 0, window.length);
		FloatFFT_1D fft = new FloatFFT_1D(window.length);
		fft.realForward(fftBuffer);

		for (int i = 0; i < fftBuffer.length / 2 - 1; i++) {
			float real = fftBuffer[2 * i];
			float imag = fftBuffer[2 * i + 1];
			powerSpectrum[i] = (float) Math.hypot(real, imag);
		}

		return powerSpectrum;
	}

//	// taken from https://gist.github.com/akuehntopf/4da9bced2cb88cfa2d19,
//	// author Andreas Kuehntopf
//	private static float getFrequencyForIndex(final int index, final int size, final float rate) {
//		return (float) index * (float) rate / size;
//	}

	public static float getFrequencyForIndex(int index, int size, float sampleRate) {
		float freq = (float) index * (float) sampleRate / (float) size;
		return freq;
	}

	public static float getFrequency(float[] map) {
		int maxBin = 0;
		float maxVal = Float.NEGATIVE_INFINITY;

		for (int i = 1; i < map.length - 1; i++) {
			float val = map[i];
			if (val > maxVal) {
				maxVal = val;
				maxBin = i;
			}
		}
//		System.out.println("MAXBIN: " + maxBin);

		// 7. Interpolate
		float mid = map[maxBin];
		float left = map[maxBin - 1];
		float right = map[maxBin + 1];
		float shift = 0.5f * (right - left) / (2.0f * mid - left - right);
		float pEst = maxBin + shift;
//		System.out.println("MAXBIN AFTER INTERPOLATION: " + (int) pEst);

		// 8. Convert to frequency
		float freq = (float) getFrequencyForIndex((int) pEst, map.length, ASIOController.getInstance().getSampleRate());
//		System.out.println("FOUND FREQUENCY: " + freq);
		return freq;
	}

}
