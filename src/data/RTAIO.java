package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public abstract class RTAIO {

	private static final File		tempFile	= new File("rta.temp");
	private static final Logger		LOG			= Logger.getLogger(RTAIO.class);
	public static final String		SEPARATOR	= ";";
	private static BufferedWriter	writer;

	public static void writeToFile(double[][] freq) {
		if (writer == null) {
			writer = createWriter();
		}
		if (writer != null) {
			try {
				writer.write(freqToString(freq));
				writer.flush();
			}
			catch (IOException e) {
				LOG.warn("Unable to write rta temp");
				LOG.debug("", e);
			}
		}
	}

	public static void closeFile() {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			}
			catch (Exception e) {
				LOG.error("Problem closing the rta temp file", e);
			}
			writer = null;
		}
	}

	public static void deleteFile() {
		closeFile();
		tempFile.delete();
	}

	public ArrayList<double[]> readFile() {
		ArrayList<double[]> result = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(tempFile));
			String line;
			while ((line = reader.readLine()) != null) {
				double[] entry = parseLine(line);
				result.add(entry);
			}
		}
		catch (Exception e) {
			LOG.warn("Unable to reread rta file");
			LOG.debug("", e);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					LOG.error("Unable to close reader");
					LOG.debug("", e);
				}
			}
		}
		return result;
	}

	private double[] parseLine(String line) {
		ArrayList<Double> result = new ArrayList<>();
		String[] split = line.split(SEPARATOR);
		for (String s : split) {
			try {
				double d = Double.parseDouble(s);
				result.add(d);
			}
			catch (Exception e) {
				LOG.debug("Data unparseable");
				LOG.trace("", e);
			}
		}
		double[] array = new double[result.size()];
		int count = 0;
		for (Double d : result) {
			array[count] = d;
			count++;
		}
		return array;
	}

	private static String freqToString(double[][] freq) {
		String result = "";
		for (double[] entry : freq) {
			result += Double.toString(Math.round(entry[1] * 100.0) / 100.0);
			result += SEPARATOR;
		}
		// removes to last separator
		result = result.substring(0, result.length() - SEPARATOR.length());
		return result + "\r\n";
	}

	private static BufferedWriter createWriter() {
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(tempFile, true));
		}
		catch (Exception e) {
			LOG.warn("Unable to create new writer for temp rta file");
			LOG.debug("", e);
		}
		return w;
	}
}
