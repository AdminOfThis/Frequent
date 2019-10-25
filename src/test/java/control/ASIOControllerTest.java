package control;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.SuperTest;

class ASIOControllerTest extends SuperTest {

	@BeforeAll
	public static void clearSyso() {
		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// do nothing
			}
		}));
		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// do nothing
			}
		}));
	}

	@Test
	public void possibleDrivers() {
		assertNotNull(ASIOController.getPossibleDrivers());
	}

	@Test
	public void instance() {
		if (ASIOController.getInstance() == null) {
			return;
		}
		new ASIOController(ASIOController.getPossibleDrivers().get(0));
		assertNotNull(ASIOController.getInstance().getInputList());
		assertTrue(ASIOController.getInstance().getInputList().size() > 0);
	}
}
