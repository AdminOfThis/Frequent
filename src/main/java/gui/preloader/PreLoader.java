package gui.preloader;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import data.util.StringProgressNotification;
import gui.FXMLUtil;
import javafx.application.Preloader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.FXMLMain;
import main.Main;

public class PreLoader extends Preloader implements Initializable {

	private static final Logger LOG = LogManager.getLogger(Preloader.class);
	private static final String PRELOADER_PATH = "/fxml/preloader/SplashScreen.fxml";
	private Stage stage;
	@FXML
	private ProgressBar progress;
	@FXML
	private Label status;
	@FXML
	private Label title, version;

	@Override
	public void handleApplicationNotification(PreloaderNotification arg0) {
		if (arg0 instanceof ProgressNotification) {
			ProgressNotification pn = (ProgressNotification) arg0;
			progress.setProgress(pn.getProgress());
			if(pn instanceof StringProgressNotification) {
				StringProgressNotification spn = (StringProgressNotification) pn;
				progress.setProgress(pn.getProgress());
				status.setText(spn.getMessage());
			}
		}
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification evt) {
		switch (evt.getType()) {
		case BEFORE_INIT:
			break;
		case BEFORE_LOAD:
			break;
		case BEFORE_START:
			stage.close();
			break;
		default:
			break;
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		status.setText("Starting ...");
		title.setText(Main.getOnlyTitle());
		version.setText(Main.getVersion());
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOG.info("Loading SplashScreen");
		stage = primaryStage;
		FXMLUtil.setIcon(stage, FXMLMain.getLogoPath());
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setTitle(Main.getReadableTitle());
		stage.setWidth(400);
		stage.setHeight(300);
		stage.centerOnScreen();
		stage.setResizable(false);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(PRELOADER_PATH));
		loader.setController(this);
		Parent p = loader.load();
		FXMLUtil.setStyleSheet(p);
//		p.setStyle(Main.getStyle());
		Scene scene = new Scene(p);
		stage.setScene(scene);
		stage.show();
	}
}
