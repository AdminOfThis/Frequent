package gui.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public class ConfirmationDialog extends CustomDialog<ButtonType> {

	private static final String FXML_PATH = "/fxml/dialog/ConfirmationDialog.fxml";

	@FXML
	private Label lblText;

	public ConfirmationDialog(String text) {
		this(text, false);
	}

	public ConfirmationDialog(String text, boolean showCancel) {
		super(FXML_PATH, text);
		lblText.setText(text);
		if (showCancel) {
			getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
		}

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblText.setText("");
	}
}
