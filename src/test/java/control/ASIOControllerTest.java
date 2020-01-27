package control;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ASIOControllerTest {

	@Test
	public void instance() {
		if (ASIOController.getPossibleDrivers() == null || ASIOController.getPossibleDrivers().isEmpty()) {
			return;
		}
		new ASIOController(ASIOController.getPossibleDrivers().get(0).getName());
		assertNotNull(ASIOController.getInstance().getInputList());
		assertTrue(ASIOController.getInstance().getInputList().size() > 0);
	}

	@Test
	public void possibleDrivers() {
		assertNotNull(ASIOController.getPossibleDrivers());
	}
}
