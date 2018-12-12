package dialog;

import gui.controller.MainController;
import gui.utilities.FXMLUtil;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public abstract class Dialog<T> extends javafx.scene.control.Dialog<T> {

	public Dialog(String text, String title) {
		setTitle(title);

		setContentText(text);
		initStyle(StageStyle.UNDECORATED);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(MainController.getInstance().getStage());

		FXMLUtil.setStyleSheet(getDialogPane());

		setWidth(300);
		setHeight(200);

	}
}
