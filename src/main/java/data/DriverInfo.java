package data;

import com.synthbot.jasiohost.AsioDriver;

public class DriverInfo {

	private String driverName;
	private int asioVersion;
	private int version;

	private int latencyInput;
	private int latencyOutput;

	private int inputCount;
	private int outputCount;
	private int buffer;
	private double sampleRate;

	public DriverInfo(final AsioDriver driver) {
		driverName = driver.getName();
		inputCount = driver.getNumChannelsInput();
		outputCount = driver.getNumChannelsOutput();
		buffer = driver.getBufferPreferredSize();
		asioVersion = driver.getAsioVersion();
		latencyInput = driver.getLatencyInput();
		latencyOutput = driver.getLatencyOutput();
		sampleRate = driver.getSampleRate();
		version = driver.getVersion();
	}

	public String getName() {
		return driverName;
	}

	public int getAsioVersion() {
		return asioVersion;
	}

	public int getVersion() {
		return version;
	}

	public int getLatencyInput() {
		return latencyInput;
	}

	public int getLatencyOutput() {
		return latencyOutput;
	}

	public int getInputCount() {
		return inputCount;
	}

	public int getOutputCount() {
		return outputCount;
	}

	public int getBuffer() {
		return buffer;
	}

	public double getSampleRate() {
		return sampleRate;
	}

}
