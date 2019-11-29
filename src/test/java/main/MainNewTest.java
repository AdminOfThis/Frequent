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
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.service.query.EmptyNodeQueryException;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

class MainNewTest extends FxRobot {

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
	public void tearDown() throws Exception {
		FxToolkit.hideStage();
		release(new KeyCode[] {});
		release(new MouseButton[] {});
	}

	@Test
	public void clickWaveCharts() {
		clickOn("#toggleBtmRaw");
		clickOn("#toggleBtmWave");
	}

	@Test
	public void clickWave() {
		assertNotNull(lookup("#waveFormPane").query());
		clickOn("#togglePreview");
		assertThrows(EmptyNodeQueryException.class, () -> lookup("#waveFormPane").query());
		clickOn("#togglePreview");
		assertNotNull(lookup("#waveFormPane").query());
	}

	@Test
	public void clickChannel() throws InterruptedException {
		ToggleButton button = lookup("#toggleChannels").query();
		assertTrue(button.isSelected());
		clickOn(button);
		Thread.sleep(100);
		assertFalse(button.isSelected());
		assertThrows(EmptyNodeQueryException.class, () -> lookup("#channelList").query());
		clickOn(button);
		Thread.sleep(100);
		assertTrue(button.isSelected());
		assertNotNull(lookup("#channelList").query());
		clickOn(button);
	}

	@Test
	public void clickModules() {
		for (Node node : lookup("#tglOverView").query().getParent().getChildrenUnmodifiable()) {
			if (node instanceof ToggleButton) {
				ToggleButton button = (ToggleButton) node;
				ArrayList<Node> before = new ArrayList<Node>(((SplitPane) lookup("#contentPane").query()).getItems());
				clickOn(button);
				ArrayList<Node> after = new ArrayList<Node>(((SplitPane) lookup("#contentPane").query()).getItems());
				assertNotEquals(before, after);
			}
		}
	}

	@Test
	public void clickModulesAndSubmodules() {
		for (Node node : lookup("#tglOverView").query().getParent().getChildrenUnmodifiable()) {
			if (node instanceof ToggleButton) {
				ToggleButton button = (ToggleButton) node;
				ArrayList<Node> before = new ArrayList<Node>(((SplitPane) lookup("#contentPane").query()).getItems());
				clickOn(button);
				ArrayList<Node> after = new ArrayList<Node>(((SplitPane) lookup("#contentPane").query()).getItems());
				assertNotEquals(before, after);
				for (Node child : ((HBox) lookup("#buttonBox").query()).getChildren()) {
					if (child instanceof ToggleButton) {
						clickOn(child);
						clickOn(child);
					}
				}
			}
		}
	}

	@Test
	public void checkResizable() throws InterruptedException {
		Stage stage = (Stage) lookup("#root").queryAs(BorderPane.class).getScene().getWindow();
		assertTrue(stage.isResizable());

	}

}
