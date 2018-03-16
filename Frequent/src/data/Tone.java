package data;

public class Tone {
	private double	frequency;
	private double	amplitude;

	public Tone(double freq, double ampl) {
		this.frequency = freq;
		this.amplitude = ampl;
	}

	public double getFrequency() {
		return frequency;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	public double getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(double amplitude) {
		this.amplitude = amplitude;
	}

}
