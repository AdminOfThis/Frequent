package checkVersion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

import main.CheckVersion;

public class CheckVersionTest {

	private static final String DEFAULT_COMMAND = "\"\"";

	@BeforeClass
	public static void changeSyso() {
		PrintStream emptyStream = new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
			}
		});

		System.setOut(emptyStream);
		System.setErr(emptyStream);
	}

	@Test
	public void noArgs() {
		String[] args = new String[0];
		assertFalse(CheckVersion.checkVersion(args));
	}

	@Test
	public void tooManyArgs() {
		String[] args = new String[3];
		args[0] = "1.8";
		args[1] = DEFAULT_COMMAND;
		args[2] = "LoremIpsum";
		assertFalse(CheckVersion.checkVersion(args));
	}

	@Test
	public void checkVersionTooOld() {
		String[] args = new String[2];
		args[0] = "1.100";
		args[1] = DEFAULT_COMMAND;

		assertFalse(CheckVersion.checkVersion(args));
	}

	@Test
	public void checkVersionMatches() {
		String[] args = new String[2];
		args[0] = "1.6";
		args[1] = DEFAULT_COMMAND;
		assertTrue(CheckVersion.checkVersion(args));
	}

	@Test
	public void checkVersionNewer() {
		String[] args = new String[2];
		args[0] = "1.2";
		args[1] = DEFAULT_COMMAND;
		assertTrue(CheckVersion.checkVersion(args));
	}

}
