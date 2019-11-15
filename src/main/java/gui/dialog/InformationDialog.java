package gui.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class InformationDialog extends CustomDialog<Void> {

	private static final String FXML_PATH = "/fxml/dialog/InformationDialog.fxml";

	@FXML
	private VBox center;
	@FXML
	private Label text, subText, topText;

	public InformationDialog(String title) {
		super(FXML_PATH, title);

		getDialogPane().getButtonTypes().add(ButtonType.FINISH);
		Button finish = (Button) getDialogPane().lookupButton(ButtonType.FINISH);
		finish.setManaged(false);
		finish.setManaged(false);

		getDialogPane().getScene().setOnKeyPressed(e2 -> close());
		getDialogPane().getScene().setOnMouseClicked(e2 -> close());
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

	public void setTopText(String string) {
		topText.setText(string);
	}

	public void setSubText(String string) {
		subText.setText(string);
	}

}
