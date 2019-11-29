package main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.service.query.EmptyNodeQueryException;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class MainNewTest {

	@BeforeEach
	public void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		Main.setDebug(true);
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
	public void clickWaveCharts(FxRobot robot) {
		robot.clickOn("#toggleBtmRaw");
		robot.clickOn("#toggleBtmWave");
	}

	@Test
	public void clickWave(FxRobot robot) {
		assertNotNull(robot.lookup("#waveFormPane").query());
		robot.clickOn("#togglePreview");
		assertThrows(EmptyNodeQueryException.class, () -> robot.lookup("#waveFormPane").query());
		robot.clickOn("#togglePreview");
		assertNotNull(robot.lookup("#waveFormPane").query());
	}

	@Test
	public void clickChannel(FxRobot robot) throws InterruptedException {
		ToggleButton button = robot.lookup("#toggleChannels").query();
		assertTrue(button.isSelected());
		robot.clickOn(button);
		Thread.sleep(5000);
		assertFalse(button.isSelected());
		assertThrows(EmptyNodeQueryException.class, () -> robot.lookup("#channelList").query());
		robot.clickOn(button);
		assertTrue(button.isSelected());
		assertNotNull(robot.lookup("#channelList").query());
		robot.clickOn(button);
	}

	@Test
	public void clickModules(FxRobot robot) {
		for (Node node : robot.lookup("#tglOverView").query().getParent().getChildrenUnmodifiable()) {
			if (node instanceof ToggleButton) {
				ToggleButton button = (ToggleButton) node;
				ArrayList<Node> before = new ArrayList<Node>(((SplitPane) robot.lookup("#contentPane").query()).getItems());
				robot.clickOn(button);
				ArrayList<Node> after = new ArrayList<Node>(((SplitPane) robot.lookup("#contentPane").query()).getItems());
				assertNotEquals(before, after);
			}
		}
	}

	@Test
	public void clickModulesAndSubmodules(FxRobot robot) {
		for (Node node : robot.lookup("#tglOverView").query().getParent().getChildrenUnmodifiable()) {
			if (node instanceof ToggleButton) {
				ToggleButton button = (ToggleButton) node;
				ArrayList<Node> before = new ArrayList<Node>(((SplitPane) robot.lookup("#contentPane").query()).getItems());
				robot.clickOn(button);
				ArrayList<Node> after = new ArrayList<Node>(((SplitPane) robot.lookup("#contentPane").query()).getItems());
				assertNotEquals(before, after);
				for (Node child : ((HBox) robot.lookup("#buttonBox").query()).getChildren()) {
					if (child instanceof ToggleButton) {
						robot.clickOn(child);
						robot.clickOn(child);
					}
				}
			}
		}
	}

	@Test
	public void checkResizable(FxRobot robot) throws InterruptedException {
		Stage stage = (Stage) robot.lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		assertTrue(stage.isResizable());

	}

}
