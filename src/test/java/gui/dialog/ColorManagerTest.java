package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import com.github.adminofthis.util.gui.FXMLUtil;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import main.Main;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class ColorManagerTest {

	@Test
	public void addColor(FxRobot robot) {
		ListView<?> list = robot.lookup("#list").queryListView();
		assertEquals(0, list.getItems().size());
		robot.clickOn("#btnAdd");
		assertEquals(1, list.getItems().size());
	}

	@BeforeEach
	public void before() throws Exception {

		FXMLUtil.setDefaultStyle(Main.getStyle());
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupSceneRoot(() -> new ColorManager());
		FxToolkit.setupStage(Stage::show);
		do {
			Thread.yield();
		} while (!stage.isShowing());
	}

	@RepeatedTest(value = 2)
	public void moveOver(FxRobot robot) {
		ArrayList<String> buttons = new ArrayList<>();
		buttons.add("#btnAdd");
		buttons.add("#btnCancel");
		buttons.add("#btnDelete");
		buttons.add("#btnRename");
		while (buttons.size() > 0) {
			int index = (int) Math.floor(Math.random() * (buttons.size() - 1));
			robot.moveTo(buttons.get(index));
			buttons.remove(index);
		}
	}

	@Test
	public void removeColor(FxRobot robot) {
		ListView<?> list = robot.lookup("#list").queryListView();
//		assertEquals(0, list.getItems().size());
		robot.clickOn("#btnAdd");
		int before = list.getItems().size();
		robot.clickOn(list);
		robot.press(KeyCode.DOWN);
		robot.release(KeyCode.DOWN);
		robot.clickOn("#btnDelete");
		int after = list.getItems().size();
		assertEquals(before - 1, after);
	}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		FxToolkit.hideStage();
		robot.release(new KeyCode[] {});
		robot.release(new MouseButton[] {});
	}

}
