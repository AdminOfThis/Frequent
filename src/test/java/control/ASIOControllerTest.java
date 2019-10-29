package control;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import test.SuperTest;

class ASIOControllerTest extends SuperTest {

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
