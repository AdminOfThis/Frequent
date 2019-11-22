package main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

class NewMainTest extends FxRobot {

	@BeforeEach
	public void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(FXMLMain.class, new String[] { "-debug" });
		while (!stage.isShowing()) {
			Thread.yield();
		}
		Thread.sleep(500);
	}

	@AfterEach
	public void tearDown() throws Exception {
		FxToolkit.hideStage();
		release(new KeyCode[] {});
		release(new MouseButton[] {});
	}

	@Test
	public void testEnglishInput() {
		clickOn("#listIO");

	}

}
