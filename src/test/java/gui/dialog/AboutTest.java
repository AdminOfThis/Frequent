package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.GregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import gui.FXMLUtil;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import main.Main;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class AboutTest {

	@BeforeEach
	public void before() throws Exception {
		FXMLUtil.setDefaultStyle(Main.getStyle());
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupSceneRoot(() -> new AboutController());
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
	void launch(FxRobot robot) throws Exception {
		assertEquals(1, robot.listWindows().size());
	}

	@Test
	void checkYear(FxRobot robot) throws Exception {
		TextArea area = robot.lookup("#lblLicense").queryAs(TextArea.class);
		String year = Integer.toString(new GregorianCalendar().get(GregorianCalendar.YEAR));
		assertTrue(area.getText().contains(year));
	}

}
