package gui.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.ColorController;
import data.ColorEntry;
import gui.FXMLUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import main.Main;

public class ColorManager extends AnchorPane implements Initializable {

	private class ColorListCell extends ListCell<ColorEntry> {
		private final TextField textField = new TextField();

		public ColorListCell() {
			textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					cancelEdit();
				}
			});
			textField.setOnAction(e -> {
				getItem().setName(textField.getText());
				setText(textField.getText());
				setContentDisplay(ContentDisplay.TEXT_ONLY);
				cancelEdit();
			});
			setGraphic(textField);

			selectedProperty().addListener((e, oldV, newV) -> {
				if (getItem() != null) {
					if (newV) {
						setStyle("-fx-background-color: " + getItem().getEntry() + "; -fx-border-color: " + Main.getAccentColor() + "; -fx-border-style: solid; -fx-border-width: 3px;");
					} else {
						setStyle("-fx-background-color: " + getItem().getEntry());
					}
				} else {
					setStyle("");
				}
			});
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(getItem().getName());
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}

		@Override
		public void startEdit() {
			super.startEdit();
			textField.setText(getItem().getName());
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			textField.requestFocus();
			textField.selectAll();
		}

		protected void updateItem(ColorEntry item, boolean empty) {

			super.updateItem(item, empty);
			if (isEditing()) {
				textField.setText(item.getName());
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			} else {
				setContentDisplay(ContentDisplay.TEXT_ONLY);
				if (item == null || empty) {
					setText("");
					setStyle("");
				} else if (item != null) {
					setText(item.getName());
					setStyle("-fx-background-color: " + item.getEntry());
				}
			}
		}
	}
	private static final Logger LOG = LogManager.getLogger(ColorManager.class);
	private static final String FXML_PATH = "/fxml/dialog/ColorManager.fxml";
	@FXML
	private ListView<ColorEntry> list;
	@FXML
	private Button btnAdd, btnCancel, btnDelete, btnRename;

	@FXML
	private ColorPicker colorPicker;

	public ColorManager() {
		super();
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML_PATH), this);
		if (p != null) {
			getChildren().add(p);
			AnchorPane.setTopAnchor(p, .0);
			AnchorPane.setBottomAnchor(p, .0);
			AnchorPane.setLeftAnchor(p, .0);
			AnchorPane.setRightAnchor(p, .0);

		} else {
			LOG.warn("Unable to load ChannelCell");
		}

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		btnDelete.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		btnRename.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		colorPicker.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

		list.setCellFactory(e -> new ColorListCell());

		list.getSelectionModel().selectedItemProperty().addListener((e, oldV, newV) -> {
			if (newV != null) {
				colorPicker.setValue(Color.web(newV.getEntry()));
			}
		});
		colorPicker.setOnAction(e -> {
			ColorEntry entry = list.getSelectionModel().getSelectedItem();
			if (entry != null) {
				entry.setEntry(FXMLUtil.toRGBCode(colorPicker.getValue()));
				refreshData();
			}
		});
		refreshData();
	}

	@FXML
	private void add(ActionEvent e) {
		double colorVal = (360 / 16 * ColorController.getInstance().getColors().size()) + (360 / 2 * ColorController.getInstance().getColors().size());
		colorVal = colorVal % 360;
		String color = FXMLUtil.toRGBCode(Color.hsb(colorVal, 1, 1));
		ColorController.getInstance().addColor("Color # " + ColorController.getInstance().getColors().size(), color);
		refreshData();
	}

	@FXML
	private void cancel(ActionEvent e) {
		try {
			((Stage) getScene().getWindow()).close();

		} catch (Exception ex) {
			LOG.warn("Problem closing window", ex);
		}
	}

	@FXML
	private void delete(ActionEvent e) {
		if (list.getSelectionModel().getSelectedIndex() >= 0) {
			ColorController.getInstance().removeColor(list.getSelectionModel().getSelectedItem());
			refreshData();
		}
	}

	private void refreshData() {
		int selectedIndex = list.getSelectionModel().getSelectedIndex();
		list.getItems().setAll(ColorController.getInstance().getColors());
		if (list.getItems().size() >= selectedIndex) {
			list.getSelectionModel().clearAndSelect(selectedIndex);
		}
	}

	@FXML
	private void rename(ActionEvent e) {
		if (list.getSelectionModel().getSelectedIndex() >= 0) {
			list.edit(list.getSelectionModel().getSelectedIndex());
		}
	}
}
