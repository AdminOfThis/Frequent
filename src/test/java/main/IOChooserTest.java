package main;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

class IOChooserTest extends FxRobot {

	@BeforeEach
	public void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(FXMLMain.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());

	}

	@AfterEach
	public void tearDown() throws Exception {
		FxToolkit.hideStage();
		release(new KeyCode[] {});
		release(new MouseButton[] {});
	}

	@Test
	public void checkElementsLoaded() {
		assertNotNull(lookup("#root"));
		assertNotNull(lookup("#listIO"));
		assertNotNull(lookup("#btnStart"));
		assertNotNull(lookup("#btnQuit"));
	}

	@Test
	public void mouseOverElements() {
		moveTo("#root");
		moveTo("#listIO");
		moveTo("#btnStart");
		moveTo("#btnQuit");
	}

	@Test
	public void clickOnListView() {
		clickOn("#listIO");
	}

	@Test
	public void checkResizable() throws InterruptedException {
		Stage stage = (Stage) lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		assertFalse(stage.isResizable());

	}

}
