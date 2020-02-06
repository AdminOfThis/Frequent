package gui.utilities;

import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.Watchdog;
import data.ColorController;
import data.ColorEntry;
import data.Input;
import gui.FXMLUtil;
import gui.controller.MainController;
import gui.dialog.ColorManager;
import gui.dialog.TextInputDialog;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.FXMLMain;

public abstract class InputCellContextMenu extends ContextMenu {

	private static final int TIMES[] = { 1, 5, 10, 20, 30, 60, 300 };
	private static final Logger LOG = LogManager.getLogger(InputCellContextMenu.class);
	private MenuItem rename = new MenuItem("Rename");
	private Menu colorMenu = new Menu("Color");
	Menu watchList = new Menu("Watchdog");
	private Input input;

	public InputCellContextMenu(final Input in) {
		super();
		setIds();
		setAutoFix(true);
		setAutoHide(true);
		input = in;
		if (input != null) {
			// NAME
			rename.setOnAction(e -> rename());

			MenuItem colorManager = new MenuItem("Color Manager");
			colorManager.setOnAction(e -> openColorManager());
			colorMenu.getItems().add(colorManager);
			colorMenu.getItems().add(new SeparatorMenuItem());
			getItems().add(rename);
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
//		setOnHidden(e -> MainController.getInstance().refresh());
		colorMenu.setOnShowing(e -> refresh());

	}

	private void setIds() {
		rename.setId("rename");

	}

	private void initWatchDogMenu() {
		ToggleGroup tglGroup = new ToggleGroup();
		RadioMenuItem disableWatchdog = new RadioMenuItem("Disable Watchdog");
		disableWatchdog.setToggleGroup(tglGroup);
		watchList.getItems().add(disableWatchdog);
		watchList.getItems().add(new SeparatorMenuItem());
		disableWatchdog.setOnAction(e -> Watchdog.getInstance().removeEntry(input));
		for (int i : TIMES) {
			RadioMenuItem item = new RadioMenuItem(i + "s");
			item.setToggleGroup(tglGroup);
			watchList.getItems().add(item);
			item.setOnAction(e -> {
				for (Input channel : MainController.getInstance().getSelectedChannels()) {

					Watchdog.getInstance().addEntry(i, channel);
				}
			});
		}
	}

	private void openColorManager() {
		ColorManager cm = new ColorManager();
		FXMLUtil.setStyleSheet(cm);
		Stage stage = new Stage();
		stage.setTitle("Color Manager");
		stage.setScene(new Scene(cm));
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(this.getOwnerWindow());
		FXMLUtil.setIcon(stage, FXMLMain.getLogoPath());
		stage.show();
	}

	private void refresh() {
		refreshColors();
		refreshWatchdog();
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

	private void refreshWatchdog() {
		long time = Watchdog.getInstance().getTimeForInput(input);
		if (time == 0) {
			((RadioMenuItem) watchList.getItems().get(0)).setSelected(true);
		} else {
			for (MenuItem n : watchList.getItems()) {
				if (n instanceof RadioMenuItem) {
					RadioMenuItem item = (RadioMenuItem) n;
					if (Objects.equals(item.getText(), Long.toString(time) + "s")) {
						item.setSelected(true);
						break;
					}
				}
			}

		}

	}

	private void rename() {
		TextInputDialog dialog;
		if (input == null) {
			dialog = new TextInputDialog("Rename");
		} else {
			dialog = new TextInputDialog("Rename", input.getName());
		}
		FXMLUtil.setStyleSheet(dialog.getDialogPane());
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			input.setName(result.get());
		}
	}

}
