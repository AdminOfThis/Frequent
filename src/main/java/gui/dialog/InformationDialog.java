package gui.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InformationDialog extends CustomDialog<Void> {

	private static final String FXML_PATH = "/fxml/dialog/InformationDialog.fxml";

	@FXML
	private VBox center;
	@FXML
	private Label text, subText, topText;

	private Object data;

	public InformationDialog(String title) {
		this(title, false);
	}

	public InformationDialog(String title, boolean showOK) {
		super(FXML_PATH, title);
		if (!showOK) {
			Button finish = (Button) getDialogPane().lookupButton(ButtonType.OK);
			finish.setManaged(false);
			finish.setManaged(false);
			getDialogPane().getScene().setOnKeyPressed(e2 -> close());
			getDialogPane().getScene().setOnMouseClicked(e2 -> close());
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		for (Label l : new Label[] { text, subText, topText }) {
			l.setText("");
		}
	}

	public void setText(String string) {
		text.setText(string);
	}

	public void clear() {
		Platform.runLater(() -> {
			center.getChildren().clear();
			center.getChildren().add(topText);
		});
	}

	public void addText(String mainText) {
		Platform.runLater(() -> {
			Label label = new Label(mainText);
			label.setFont(text.getFont());
			label.setStyle(subText.getStyle());
			center.getChildren().add(label);
		});
	}

	public void setTopText(String string) {
		topText.setText(string);
	}

	public void setSubText(String string) {
		subText.setText(string);
	}

	public void addSubText(String text) {
		Platform.runLater(() -> {
			Label label = new Label(text);
			label.setFont(subText.getFont());
			label.setStyle(subText.getStyle());
			center.getChildren().add(label);
		});

	}

	public void sizeToScene() {
		Platform.runLater(() -> ((Stage) getDialogPane().getScene().getWindow()).sizeToScene());

	}

	public void setData(Object o) {
		this.data = o;
	}

	public Object getData() {
		return data;
	}

}
