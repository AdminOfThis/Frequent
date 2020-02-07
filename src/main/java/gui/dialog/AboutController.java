package gui.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gui.FXMLUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import main.Constants;
import main.FXMLMain;
import main.Main;

public class AboutController extends AnchorPane implements Initializable {

	private static final String FXML_PATH = "/fxml/dialog/About.fxml";
	private static final Logger LOG = LogManager.getLogger(AboutController.class);

	@FXML
	private BorderPane root;
	@FXML
	private Label lblName, lblVersion, lblCommit, lblCreator, lblBuildJDK, lblCreated;
	@FXML
	private TextArea lblLicense;

	public AboutController() {
		super();
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML_PATH), this);
		if (p != null) {
			getChildren().add(p);
			AnchorPane.setTopAnchor(p, .0);
			AnchorPane.setBottomAnchor(p, .0);
			AnchorPane.setLeftAnchor(p, .0);
			AnchorPane.setRightAnchor(p, .0);

		} else {
			LOG.warn("Unable to load About");
		}

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblName.setText(Main.getOnlyTitle());
		lblVersion.setText(Main.getVersion());
		String commit = Main.getFromManifest("Implementation-Build", "Unknown");
		lblCommit.setText(commit.substring(0, Math.min(7, commit.length())));
		lblCreator.setText(Main.getFromManifest("Created-By", "Unknown"));
		lblBuildJDK.setText(Main.getFromManifest("Build-Jdk", "Unknown"));
		lblCreated.setText(Main.getFromManifest("Built", "Unknown"));
		lblLicense.setText(Constants.LICENSE_TEXT);

	}

	@FXML
	private void close(ActionEvent e) {
		root.getScene().getWindow().hide();
	}

	@FXML
	private void openGithub(ActionEvent e) {
		FXMLMain.getInstance().getHostServices().showDocument(Constants.GITHUB_REPOSITORY_URL);
	}

}
