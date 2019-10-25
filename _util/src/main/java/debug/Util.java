package debug;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public final class Util {

	public static void disableSyso() {
		PrintStream emptyStream = new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
			}
		});

		System.setOut(emptyStream);
		System.setErr(emptyStream);
	}
	
	
	
	public static long executeTime(Runnable e) {

		return executeTime("The execution took" , e);
	}
	
	public static long executeTime(String name, Runnable e) {
		long timeBefore = System.nanoTime();
		try {
			e.run();
		} catch (Exception ex) {
			System.err.println("The task threw an exception: ");
			ex.printStackTrace();
		}
		long timeAfter = System.nanoTime();
		long duration = timeAfter - timeBefore;
		StringBuilder sb = new StringBuilder(name+" ");
		if (duration > 1000.0) {
			sb.append(Math.round(duration / 1000000.0) + " ms");
		} else {
			sb.append(duration + " ns");
		}
		System.out.println(sb.toString());
		return duration;
	}

}
