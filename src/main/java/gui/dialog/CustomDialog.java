package gui.dialog;

import gui.FXMLUtil;
import gui.controller.MainController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Main;

public abstract class CustomDialog<T> extends Dialog<T> implements Initializable {

	@FXML
	protected Region root;

	public CustomDialog(String title) {
		super();
		FXMLUtil.setStyleSheet(getDialogPane());
		initOwner(MainController.getInstance().getStage());
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.UNDECORATED);

		setOnShown(e -> {
			FXMLUtil.setIcon((Stage) getDialogPane().getScene().getWindow(), Main.getLogoPath());
		});
	}

	public CustomDialog(String fxmlPath, String title) {
		this(title);
		Parent p = FXMLUtil.loadFXML(getClass().getResource(fxmlPath), this);

		setDialogPane((DialogPane) p);

	}

//	public void setCenterNode(Node node) {
//		center.getChildren().setAll(node);
//	}

//	public Node getCenterNode() {
//		return center.getChildren().get(0);
//	}

	public void setImportant(boolean value) {
		if (value) {
			root.setStyle(root.getStyle() + "-fx-border-color: red;-fx-border-width: .5em");
		}
	}

}
