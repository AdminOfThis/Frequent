package dialog;

import javafx.scene.control.ButtonType;

public class InformationDialog extends Dialog<Void> {

	public InformationDialog(String text, String title) {
		super(text, title);
		getDialogPane().getButtonTypes().add(ButtonType.OK);
	}


}
