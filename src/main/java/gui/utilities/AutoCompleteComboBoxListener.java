package gui.utilities;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Class for auto completing combo boxes, which check the data typed with the
 * dataset of the combo box
 * 
 * @author AdminOfThis
 *
 * @param <T> inherited from {@link ComboBox}
 */
public class AutoCompleteComboBoxListener<T> implements EventHandler<KeyEvent> {

	private ComboBox<T> comboBox;
	private ObservableList<T> data;
	private boolean moveCaretToPos = false;
	private int caretPos;

	/**
	 * Creates a new AutoCompleteLsitener, and adds it to the given {@link ComboBox}
	 * @param comboBox THe {@link ComboBox} to which the Listener gets added
	 */
	public AutoCompleteComboBoxListener(final ComboBox<T> comboBox) {
		this.comboBox = comboBox;
		data = comboBox.getItems();

		this.comboBox.setEditable(true);
		this.comboBox.setOnKeyPressed(t -> comboBox.hide());
		this.comboBox.setOnKeyReleased(AutoCompleteComboBoxListener.this);
	}

	@Override
	public void handle(final KeyEvent event) {

		if (event.getCode() == KeyCode.UP) {
			caretPos = -1;
			moveCaret(comboBox.getEditor().getText().length());
			return;
		} else if (event.getCode() == KeyCode.DOWN) {
			if (!comboBox.isShowing()) {
				comboBox.show();
			}
			caretPos = -1;
			moveCaret(comboBox.getEditor().getText().length());
			return;
		} else if (event.getCode() == KeyCode.BACK_SPACE) {
			moveCaretToPos = true;
			caretPos = comboBox.getEditor().getCaretPosition();
		} else if (event.getCode() == KeyCode.DELETE) {
			moveCaretToPos = true;
			caretPos = comboBox.getEditor().getCaretPosition();
		}

		if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT || event.isControlDown()
				|| event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END
				|| event.getCode() == KeyCode.TAB) {
			return;
		}

		handle2();
	}

	private void handle2() {
		ObservableList<T> list = FXCollections.observableArrayList();
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).toString().toLowerCase()
					.startsWith(AutoCompleteComboBoxListener.this.comboBox.getEditor().getText().toLowerCase())) {
				list.add(data.get(i));
			}
		}
		String t = comboBox.getEditor().getText();

		comboBox.setItems(list);
		comboBox.getEditor().setText(t);
		if (!moveCaretToPos) {
			caretPos = -1;
		}
		moveCaret(t.length());
		if (!list.isEmpty()) {
			comboBox.show();
		}
	}

	private void moveCaret(final int textLength) {
		if (caretPos == -1) {
			comboBox.getEditor().positionCaret(textLength);
		} else {
			comboBox.getEditor().positionCaret(caretPos);
		}
		moveCaretToPos = false;
	}

}