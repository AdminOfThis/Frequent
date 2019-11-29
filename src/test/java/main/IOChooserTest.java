package main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class IOChooserTest {

	@BeforeEach
	public void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(FXMLMain.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());

	}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		FxToolkit.hideStage();
		robot.release(new KeyCode[] {});
		robot.release(new MouseButton[] {});
	}

	@Test
	public void checkElementsLoaded(FxRobot robot) {
		assertNotNull(robot.lookup("#root"));
		assertNotNull(robot.lookup("#listIO"));
		assertNotNull(robot.lookup("#btnStart"));
		assertNotNull(robot.lookup("#btnQuit"));
	}

	@Test
	public void mouseOverElements(FxRobot robot) {
		robot.moveTo("#root");
		robot.moveTo("#listIO");
		robot.moveTo("#btnStart");
		robot.moveTo("#btnQuit");
	}

	@Test
	public void clickOnListView(FxRobot robot) {
		robot.clickOn("#listIO");
	}

	@Test
	public void checkResizable(FxRobot robot) throws InterruptedException {
		Stage stage = (Stage) robot.lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		assertFalse(stage.isResizable());

	}

}
