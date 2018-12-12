package dialog;

import javafx.scene.control.ButtonType;

public class InformationDialog extends Dialog<Void> {

	public InformationDialog(String text) {
		super(text);
		getDialogPane().getButtonTypes().add(ButtonType.OK);
	}


}
