package control;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ASIOControllerTest {

	@BeforeAll
	static void clearSyso() {
		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
			}
		}));
		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {

			}
		}));
	}

	@Test
	void possibleDrivers() {
		assertNotNull(ASIOController.getPossibleDrivers());
	}

	@Test
	void instance() {
		if (ASIOController.getInstance() == null) {
			return;
		}
		new ASIOController(ASIOController.getPossibleDrivers().get(0));
		assertNotNull(ASIOController.getInstance().getInputList());
		assertTrue(ASIOController.getInstance().getInputList().size() > 0);
	}
}
