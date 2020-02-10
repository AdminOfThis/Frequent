package gui.preloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import javafx.application.Preloader.ProgressNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import main.Main;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class PreloaderTest {

	private static PreLoader loader;

	@BeforeAll
	public static void before() throws Exception {
		Stage stage = FxToolkit.registerPrimaryStage();
		Main.initTitle();
		loader = (PreLoader) FxToolkit.setupApplication(PreLoader.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());
	}

//	@AfterEach
//	public void tearDown(FxRobot robot) throws Exception {
//		FxToolkit.hideStage();
//		robot.release(new KeyCode[] {});
//		robot.release(new MouseButton[] {});
//	}

	@Test
	void basic(FxRobot robot) throws Exception {
		assertEquals(1, robot.listWindows().size());
	}

	@Test
	void label(FxRobot robot) throws Exception {
		Label title = robot.lookup("#title").queryAs(Label.class);
		Label version = robot.lookup("#version").queryAs(Label.class);
		assertFalse(title.getText().isEmpty());
		assertFalse(version.getText().isEmpty());
	}

	@RepeatedTest(3)
	void progress(FxRobot robot) throws Exception {
		ProgressBar prog = robot.lookup("#progress").queryAs(ProgressBar.class);
		double progress = Math.random();
		WaitForAsyncUtils.asyncFx(() -> loader.handleApplicationNotification(new ProgressNotification(progress)));
		assertEquals(progress, prog.getProgress());
	}

	@Test
	void wrongProgress(FxRobot robot) throws Exception {
		ProgressBar prog = robot.lookup("#progress").queryAs(ProgressBar.class);
		double progress = Math.random();
		WaitForAsyncUtils.asyncFx(() -> loader.handleApplicationNotification(new StateChangeNotification(StateChangeNotification.Type.BEFORE_INIT)));
		assertNotEquals(progress, prog.getProgress());
	}

}
