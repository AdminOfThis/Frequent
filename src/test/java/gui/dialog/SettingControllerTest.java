package gui.dialog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import gui.FXMLUtil;
import gui.controller.SettingsController;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import main.Main;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class SettingControllerTest {

	@BeforeAll
	public static void load() {
		Runtime.getRuntime().loadLibrary("./workdir/jasiohost64");
	}
	
	@BeforeEach
	public void before() throws Exception {
		if (!Main.isInitialized()) {
			Main.initialize();
		}
		FXMLUtil.setDefaultStyle(Main.getStyle());
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupSceneRoot(() -> new SettingsController());
		FxToolkit.setupStage(Stage::show);
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
	public void save(FxRobot robot) throws InterruptedException {
		robot.clickOn("#btnSave");
		Thread.sleep(1000);
	}

	@Test
	public void cancel(FxRobot robot) throws InterruptedException {
		robot.clickOn("#btnCancel");
	}

	@Test
	public void clickTest(FxRobot robot) throws InterruptedException {
		GridPane grid = robot.lookup("#grid").queryAs(GridPane.class);
		click(grid, robot);
	}

	private void click(Parent parent, FxRobot robot) {
		for (Node n : parent.getChildrenUnmodifiable()) {
			robot.clickOn(n);
			if (n instanceof Parent) {
				Parent p = (Parent) n;
				click(p, robot);
			}

		}
	}

}
