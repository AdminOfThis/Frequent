package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import control.ASIOController;
import data.DriverInfo;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import main.FXMLMain;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class IOChooserTest {

	@BeforeAll
	public static void addDummyDriver() {
		ASIOController.getInstance().addDriverInfo(new DriverInfo("TEST", 2, 2, 128, 0, 20, 20, 48000, 0));
	}

	@BeforeEach
	public void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(FXMLMain.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());

	}

	@Test
	public void checkElementsLoaded(FxRobot robot) {
		assertNotNull(robot.lookup("#root"));
		assertNotNull(robot.lookup("#listIO"));
		assertNotNull(robot.lookup("#btnStart"));
		assertNotNull(robot.lookup("#btnQuit"));
	}

	@Test
	public void checkNumItems(FxRobot robot) throws InterruptedException {
		ListView<?> list = robot.lookup("#listIO").queryAs(ListView.class);
		assertEquals(1, list.getItems().size());
	}

	@Test
	public void checkResizable(FxRobot robot) throws InterruptedException {
		Stage stage = (Stage) robot.lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		assertFalse(stage.isResizable());
	}

	@Test
	public void clickOnListView(FxRobot robot) {
		robot.clickOn("#listIO");
	}

	@Test
	public void mouseOverElements(FxRobot robot) {
		robot.moveTo("#root");
		robot.moveTo("#listIO");
		robot.moveTo("#btnStart");
		robot.moveTo("#btnQuit");
	}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		FxToolkit.hideStage();
		robot.release(new KeyCode[] {});
		robot.release(new MouseButton[] {});
	}

}
