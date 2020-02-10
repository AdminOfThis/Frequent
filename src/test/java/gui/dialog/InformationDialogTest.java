package gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
class InformationDialogTest {

	private InformationDialog dialog;

	public void init(final boolean showCancel) throws Exception {
		FxToolkit.setupSceneRoot(() -> {
			Button openDialogButton = new Button("Open Dialog");
			openDialogButton.setId("openDialog");
			openDialogButton.setOnAction(event -> {
				dialog = new InformationDialog("Bla", showCancel);
				dialog.show();
			});
			StackPane root = new StackPane(openDialogButton);
			root.setPrefSize(500, 500);
			return new StackPane(root);
		});
		FxToolkit.setupStage(Stage::show);
	}

	public void init() throws Exception {
		FxToolkit.setupSceneRoot(() -> {
			Button openDialogButton = new Button("Open Dialog");
			openDialogButton.setId("openDialog");
			openDialogButton.setOnAction(event -> {
				dialog = new InformationDialog("Bla");
				dialog.show();
			});
			StackPane root = new StackPane(openDialogButton);
			root.setPrefSize(500, 500);
			return new StackPane(root);
		});
		FxToolkit.setupStage(Stage::show);
	}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		if (dialog != null && dialog.isShowing()) {
			Platform.runLater(() -> dialog.close());
		}
		FxToolkit.hideStage();
		robot.release(new KeyCode[] {});
		robot.release(new MouseButton[] {});
	}

	@Test
	public void openDialog(FxRobot robot) throws Exception {
		init();
		start(robot);
	}

	private void start(FxRobot robot) throws Exception {
		robot.clickOn("#openDialog");
		assertEquals(2, robot.listWindows().size());
	}

	@Test
	public void addText(FxRobot robot) throws Exception {
		init(true);
		start(robot);
		int before = robot.lookup("#center").queryAs(VBox.class).getChildren().size();
		dialog.addText("This is Main Text");
		Thread.sleep(200);
		int after = robot.lookup("#center").queryAs(VBox.class).getChildren().size();
		assertEquals(before + 1, after);
	}

	@Test
	public void addSubText(FxRobot robot) throws Exception {
		init(true);
		start(robot);
		int before = robot.lookup("#center").queryAs(VBox.class).getChildren().size();
		dialog.addSubText("This is sub Text");
		Thread.sleep(200);
		int after = robot.lookup("#center").queryAs(VBox.class).getChildren().size();
		assertEquals(before + 1, after);
	}

	@Test
	public void confirm(FxRobot robot) throws Exception {
		init(true);
		start(robot);
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		robot.clickOn(okButton);
		assertEquals(1, robot.listWindows().size());
	}

	@Test
	public void close(FxRobot robot) throws Exception {
		init(false);
		start(robot);
		Platform.runLater(() -> dialog.close());
		Thread.sleep(200);
		assertEquals(1, robot.listWindows().size());
	}

}
