package gui.utilities.controller;

import java.net.URL;
import java.util.ResourceBundle;

import gui.FXMLUtil;
import gui.controller.MainController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Main;

public class Dialog implements Initializable {

	private static final String FXML_PATH = "/fxml/utilities/Dialog.fxml";

	@FXML
	private BorderPane root;
	@FXML
	private VBox center;
	@FXML
	private Label text, subText, topText;
	private Stage stage;

	public Dialog(String title) {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML_PATH), this);
		FXMLUtil.setStyleSheet(p);
		p.setStyle(Main.getStyle());
		Platform.runLater(() -> {
			stage = new Stage();
			stage.setTitle(title);
			FXMLUtil.setIcon(stage, Main.getLogoPath());
			Scene scene = new Scene(p);
			stage.setScene(scene);
			Stage mainStage = MainController.getInstance().getStage();
			stage.initOwner(mainStage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initStyle(StageStyle.UNDECORATED);
			scene.setOnKeyPressed(e -> stage.close());
			scene.setOnMouseClicked(e -> stage.close());
			stage.sizeToScene();
			stage.setResizable(false);
			stage.show();
			stage.setX((mainStage.getX() + (mainStage.getWidth() / 2.0)) - stage.getWidth() / 2.0);
			stage.setY((mainStage.getY() + (mainStage.getHeight() / 2.0)) - stage.getHeight() / 2.0);

		});

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		for (Label l : new Label[] { text, subText, topText }) {
			l.setText("");
		}
	}

	public void setCenterNode(Node node) {
		center.getChildren().setAll(node);
	}

	public Node getCenterNode() {
		return center.getChildren().get(0);
	}

	public void setImportant(boolean value) {
		if (value) {
			root.setStyle(root.getStyle() + "-fx-border-color: red;-fx-border-width: .5em");
		}
	}

	public void setTitle(final String title) {
		if (stage != null) {
			stage.setTitle(title);
		}
	}

	public void setText(String string) {
		text.setText(string);
		sizeToScene();
	}

	public void setTopText(String string) {
		topText.setText(string);
		sizeToScene();
	}

	public void setSubText(String string) {
		subText.setText(string);
		sizeToScene();
	}

	public void sizeToScene() {
		if (stage != null) {
			stage.sizeToScene();
		}
	}

	public boolean isShowing() {
		return stage.isShowing();
	}

}
