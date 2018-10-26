package gui.preloader;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import javafx.application.Preloader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Main;

public class PreLoader extends Preloader implements Initializable {

	private static final Logger	LOG				= Logger.getLogger(Preloader.class);
	private static final String	PRELOADER_PATH	= "/gui/preloader/SplashScreen.fxml";
	private static final String	LOGO_SMALL		= "/res/logo_64.png";
	private Stage				stage;
	@FXML
	private ProgressBar			progress;
	@FXML
	private Label				status;
	@FXML
	private BorderPane			root;
	@FXML
	private Label				title, version;

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
		try {
			stage.getIcons().add(new Image(getClass().getResourceAsStream(LOGO_SMALL)));
		} catch (Exception e) {
			LOG.error("Unable to load logo");
			LOG.debug("", e);
		}
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setTitle(Main.getTitle());
		stage.setWidth(400);
		stage.setHeight(300);
		stage.centerOnScreen();
		stage.setResizable(false);
		FXMLLoader loader = new FXMLLoader(getClass().getResource(PRELOADER_PATH));
		loader.setController(this);
		Parent p = loader.load();
		p.setStyle(Main.getStyle());
		Scene scene = new Scene(p);
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification evt) {
		switch (evt.getType()) {
		case BEFORE_INIT:
			status.setText("Initializing ...");
			progress.setProgress(0.1);
			break;
		case BEFORE_LOAD:
			status.setText("Loading GUI");
			break;
		case BEFORE_START:
			stage.close();
			break;
		default:
			break;
		}
	}

	@Override
	public void handleApplicationNotification(PreloaderNotification arg0) {
		if (arg0 instanceof ProgressNotification) {
			ProgressNotification pn = (ProgressNotification) arg0;
			progress.setProgress(pn.getProgress());
		}
	}
}
