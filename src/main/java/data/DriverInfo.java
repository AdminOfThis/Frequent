package data;

import com.synthbot.jasiohost.AsioDriver;

public class DriverInfo {

	private String driverName = "UNKNOWN";
	private int asioVersion;
	private int version;

	private int latencyInput;
	private int latencyOutput;

	private int inputCount;
	private int outputCount;
	private int buffer;
	private double sampleRate;

	public DriverInfo(final AsioDriver driver) {

		this(driver.getName(), driver.getNumChannelsInput(), driver.getNumChannelsOutput(), driver.getBufferPreferredSize(), driver.getAsioVersion(), driver.getLatencyInput(), driver.getLatencyOutput(), driver.getSampleRate(), driver.getVersion());

	}

	public DriverInfo(String name, int inCount, int outCount, int buff, int asioV, int latIn, int latOut, double sample, int vers) {
		driverName = name;
		inputCount = inCount;
		outputCount = outCount;
		buffer = buff;
		asioVersion = asioV;
		latencyInput = latIn;
		latencyOutput = latOut;
		sampleRate = sample;
		version = vers;
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
