package gui.utilities;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.Watchdog;
import data.ColorController;
import data.ColorEntry;
import data.Input;
import dialog.ColorManager;
import gui.FXMLUtil;
import gui.controller.MainController;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.Main;

public abstract class InputCellContextMenu extends ContextMenu {

	private static final int TIMES[] = { 1, 5, 10, 20, 30, 60, 300 };
	private static final Logger LOG = LogManager.getLogger(InputCellContextMenu.class);
	private MenuItem name = new MenuItem("Rename");
	private Menu colorMenu = new Menu("Color");
	private Menu watchList = new Menu("Watchdog");
	private Input input;

	public InputCellContextMenu(final Input in) {
		super();
		setAutoFix(true);
		setAutoHide(true);
		input = in;
		if (input != null) {
			// NAME
			name.setOnAction(e -> rename());

			MenuItem colorManager = new MenuItem("Color Manager");
			colorManager.setOnAction(e -> openColorManager());
			colorMenu.getItems().add(colorManager);
			colorMenu.getItems().add(new SeparatorMenuItem());
			getItems().add(name);
			getItems().add(colorMenu);
			getItems().add(watchList);
			initWatchDogMenu();
		}
		focusedProperty().addListener((obs, o, n) -> {
			if (!n) {
				hide();
			}
		});
		addEventHandler(MouseEvent.MOUSE_CLICKED, e -> hide());
		setOnHidden(e -> MainController.getInstance().refresh());
		colorMenu.setOnShowing(e -> refreshColors());
	}

	private void initWatchDogMenu() {
		MenuItem disableWatchdog = new MenuItem("Disable Watchdog");
		watchList.getItems().add(disableWatchdog);
		disableWatchdog.setOnAction(e -> Watchdog.getInstance().removeEntry(input));
		for (int i : TIMES) {
			MenuItem item = new MenuItem(i + "s");
			watchList.getItems().add(item);
			item.setOnAction(e -> {
				Watchdog.getInstance().removeEntry(input);
				Watchdog.getInstance().addEntry(i, input);
			});
		}
	}

	private void rename() {
		TextInputDialog dialog;
		if (input == null) {
			dialog = new TextInputDialog();
		} else {
			dialog = new TextInputDialog(input.getName());
		}
		FXMLUtil.setStyleSheet(dialog.getDialogPane());
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			input.setName(result.get());
			MainController.getInstance().refresh();
		}
	}

	private void openColorManager() {
		ColorManager cm = new ColorManager();
		FXMLUtil.setStyleSheet(cm);
		cm.setStyle(Main.getStyle());
		Stage stage = new Stage();
		stage.setTitle("Color Manager");
		stage.setScene(new Scene(cm));
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(this.getOwnerWindow());
		FXMLUtil.setIcon(stage, Main.getLogoPath());
		stage.show();
	}

	private void refreshColors() {
		colorMenu.getItems().remove(2, colorMenu.getItems().size());
		for (ColorEntry entry : ColorController.getInstance().getColors()) {
			Circle circle = new Circle(5.0);
			String colorHex = entry.getEntry();
			circle.setStyle("-fx-fill: " + colorHex);
			MenuItem item = new MenuItem(entry.getName());
			item.setGraphic(circle);
			colorMenu.getItems().add(item);
			item.setOnAction(e -> {
				LOG.info("Changing color of " + input.getName() + " to " + colorHex);
				input.setColor(colorHex);
				MainController.getInstance().refresh();
			});
		}
	}

}
