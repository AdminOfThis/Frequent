package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import main.Constants;
import main.Constants.WINDOW_OPEN;
import main.FXMLMain;
import main.Main;
import preferences.PropertiesIO;

@ExtendWith(ApplicationExtension.class)
class WindowPositionTest {

	@BeforeEach
	public void before() throws Exception {}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		FxToolkit.hideStage();
		robot.release(new KeyCode[] {});
		robot.release(new MouseButton[] {});
	}

	@AfterAll
	public void resetPos() throws Exception {
		launch(WINDOW_OPEN.MAXIMIZED.toString());
	}

	@Test
	public void openMaximized(FxRobot robot) throws Exception {
		launch(WINDOW_OPEN.MAXIMIZED.toString());
		assertTrue(getStage(robot).isMaximized());
		assertFalse(getStage(robot).isFullScreen());
	}

	@Test
	public void openFullscreen(FxRobot robot) throws Exception {
		launch(WINDOW_OPEN.FULLSCREEN.toString());
		assertTrue(getStage(robot).isFullScreen());
	}

	@RepeatedTest(3)
	public void openWindowed(FxRobot robot) throws Exception {
		int width = (int) Math.random() * (1920 - 600) + 600;
		int height = (int) Math.random() * (1920 - 400) + 400;
		launch(WINDOW_OPEN.WINDOWED.toString() + "," + width + "," + height);
		assertFalse(getStage(robot).isFullScreen());
		assertFalse(getStage(robot).isMaximized());
		assertEquals(width, getStage(robot).getWidth());
		assertEquals(height, getStage(robot).getHeight());
	}

	@RepeatedTest(5)
	public void openAsClosed(FxRobot robot) throws Exception {
		int width = (int) Math.random() * (1920 - 600) + 600;
		int height = (int) Math.random() * (1920 - 400) + 400;
		int x = (int) Math.random() * (1920 - 10) + 10;
		int y = (int) Math.random() * (1080 - 10) + 10;
		boolean full = Math.random() <= .5 ? true : false;
		launch(WINDOW_OPEN.DEFAULT.toString() + "," + width + "," + height + "," + x + "," + y + "," + Boolean.toString(full));

		assertEquals(full, getStage(robot).isFullScreen());
		if (!full) {
			assertEquals(width, getStage(robot).getWidth(), 1);
			assertEquals(height, getStage(robot).getHeight(), 1);
			assertEquals(x, getStage(robot).getX(), 1);
			assertEquals(y, getStage(robot).getY(), 1);
		}

	}

	private void launch(String open) throws Exception {

		Stage stage = FxToolkit.registerPrimaryStage();
		Main.setDebug(true);
		PropertiesIO.setProperty(Constants.SETTING_WINDOW_OPEN, open);
		FxToolkit.setupApplication(FXMLMain.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());
		Thread.sleep(100);
	}

	private Stage getStage(FxRobot robot) {
		Stage stage = (Stage) robot.lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		return stage;
	}

}
