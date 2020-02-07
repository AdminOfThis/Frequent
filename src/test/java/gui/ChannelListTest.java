package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import com.synthbot.jasiohost.AsioChannel;

import data.Channel;
import gui.controller.MainController;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import main.FXMLMain;
import main.Main;

@ExtendWith(ApplicationExtension.class)
@Tag("gui")
public class ChannelListTest {

	private static final String NAME = "TestChannel";

	private Channel channel;

	@BeforeEach
	public void before() throws Exception {
//		AsioChannel asioChannel = Mockito.mock(AsioChannel.class);
		AsioChannel asioChannel = Mockito.mock(AsioChannel.class);
		Mockito.when(asioChannel.getChannelIndex()).thenReturn((int) Math.random() * 16);
		Mockito.when(asioChannel.getChannelName()).thenReturn(NAME);
		channel = new Channel(asioChannel);
		ArrayList<Channel> list = new ArrayList<>();
		list.add(channel);

		Main.setDebug(true);
		Stage stage = FxToolkit.registerPrimaryStage();
		FxToolkit.setupApplication(FXMLMain.class);
		do {
			Thread.yield();
		} while (!stage.isShowing());
		MainController.getInstance().setChannelList(list);
	}

	@AfterEach
	public void tearDown(FxRobot robot) throws Exception {
		try {
			FxToolkit.hideStage();
			robot.release(new KeyCode[] {});
			robot.release(new MouseButton[] {});
		} catch (Exception e) {}
	}

	@Test
	@Timeout(1000)
	public ContextMenu openContextMenuWithMouse(FxRobot robot) throws InterruptedException {
		ListCell<?> cell = selectFirstElement(robot);
		ContextMenu menu = cell.getContextMenu();
		robot.clickOn(cell, MouseButton.SECONDARY);
		// Bugfix for monocle bug, see https://github.com/TestFX/Monocle/issues/12
		while (!menu.isShowing()) {
			Platform.runLater(() -> menu.show(cell.getScene().getWindow()));
		}
		assertTrue(cell.getContextMenu().isShowing());
		return cell.getContextMenu();
	}

	@Test
	public void rename(FxRobot robot) throws InterruptedException {
		int windowsPrior = robot.listWindows().size();
		openContextMenuWithMouse(robot);
		robot.clickOn("#rename");
		assertEquals(windowsPrior + 1, robot.listWindows().size());
		robot.clickOn("#textField");
		robot.type(KeyCode.BACK_SPACE, 20);
		robot.type(KeyCode.X, 8);
		robot.type(KeyCode.ENTER);
		assertEquals("xxxxxxxx", channel.getName());
	}

	@Test
	public void resetName(FxRobot robot) throws InterruptedException {
		rename(robot);
		openContextMenuWithMouse(robot);
		robot.clickOn("#resetName");

		assertEquals(NAME, channel.getName());
	}

	private ListCell<?> selectFirstElement(FxRobot robot) throws InterruptedException {
		ListView<?> list = robot.lookup("#channelList").queryListView();
		assertTrue(list.getItems().size() >= 1);
		robot.moveTo(list);
		Thread.sleep(100);
		ListCell<?> cell = robot.from(list).lookup(".list-cell").nth(0).query();
		return cell;
	}

}
