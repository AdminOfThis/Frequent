package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

	@ParameterizedTest
	@ValueSource(strings = { "800,600", "1280,720", "600,400" })
	public void openWindowed(String word, FxRobot robot) throws Exception {
		launch(WINDOW_OPEN.WINDOWED.toString() + "," + word);
		assertFalse(getStage(robot).isFullScreen());
		assertFalse(getStage(robot).isMaximized());
		int width = Integer.parseInt(word.split(",")[0]);
		int height = Integer.parseInt(word.split(",")[1]);
		assertEquals(width, getStage(robot).getWidth());
		assertEquals(height, getStage(robot).getHeight());
	}

	@ParameterizedTest
	@ValueSource(strings = { "800,600,100,200,false", "1280,720,600,80,true", "600,400,0,0,false" })
	public void openAsClosed(String word, FxRobot robot) throws Exception {
		launch(WINDOW_OPEN.DEFAULT.toString() + "," + word);
		int width = Integer.parseInt(word.split(",")[0]);
		int height = Integer.parseInt(word.split(",")[1]);
		int x = Integer.parseInt(word.split(",")[2]);
		int y = Integer.parseInt(word.split(",")[3]);
		boolean full = Boolean.parseBoolean(word.split(",")[4]);
		assertEquals(full, getStage(robot).isFullScreen());
		if (!full) {
			assertEquals(width, getStage(robot).getWidth());
			assertEquals(height, getStage(robot).getHeight());
			assertEquals(x, getStage(robot).getX());
			assertEquals(y, getStage(robot).getY());
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
	}

	private Stage getStage(FxRobot robot) {
		Stage stage = (Stage) robot.lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		return stage;
	}

}
