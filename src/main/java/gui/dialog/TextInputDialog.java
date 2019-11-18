package gui.dialog;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class TextInputDialog extends CustomDialog<String> {

	private static final String FXML_PATH = "/fxml/dialog/TextInputDialog.fxml";

	@FXML
	private Label topText;
	@FXML
	private TextField text;
	private String oldText, description;

	public TextInputDialog(String title, String oldText) {
		super(FXML_PATH, title);
		Platform.runLater(() -> init());

		this.description = title;
		this.oldText = oldText;
		setResultConverter((button) -> {
			if (Objects.equals(button, ButtonType.FINISH)) {
				return text.getText();
			} else {
				return null;
			}
		});
		Platform.runLater(() -> text.selectAll());
	}

	private void init() {
		topText.setText(description);
		text.setText(oldText);

	}

	public TextInputDialog(String title) {
		this(title, null);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		topText.setText("");
	}

}
