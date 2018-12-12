package dialog;

import javafx.scene.control.ButtonType;
import javafx.util.Callback;

public class ConfirmationDialog extends Dialog<ButtonType> {

	public ConfirmationDialog(String text, boolean showCancel) {
		super(text);
		getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
		if (showCancel) {
			getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
		}

		setResultConverter(new Callback<ButtonType, ButtonType>() {

			@Override
			public ButtonType call(ButtonType param) {
				return param;
			}
		});


	}


	public ConfirmationDialog(String text) {
		this(text, false);
	}
}
