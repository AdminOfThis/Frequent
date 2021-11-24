package gui.controller;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.adminofthis.util.gui.FXMLUtil;
import com.github.adminofthis.util.preferences.PropertiesIO;

import control.ASIOController;
import control.Watchdog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.Constants;
import main.Constants.RESTORE_PANEL;
import main.Constants.WINDOW_OPEN;
import main.Main;

/**
 * 
 * @author AdminOfThis
 *
 */
public class SettingsController extends AnchorPane implements Initializable {
	
	private static final String FXML_PATH = "/fxml/Settings.fxml";
	private static final Logger LOG = LogManager.getLogger(SettingsController.class);

	private static final Number[] BUFFERS = new Number[] { 64, 128, 256, 512, 1024, 2048 };

	@FXML
	private BorderPane root;
	@FXML
	private ListView<String> list;
	@FXML
	private ScrollPane scrollPane;
	@FXML
	private GridPane grid;
	@FXML
	private Button btnCancel, btnSave;
	@FXML
	private ChoiceBox<Locale> choiLanguage;
	@FXML
	private ComboBox<Number> chbBuffer;

	@FXML
	private ComboBox<String> chbDevice;

	@FXML
	private RadioButton rBtndbCurrent, rBtndbPeak;

	@FXML
	private ToggleGroup startUp, startUpPanel;

	@FXML
	private RadioButton rdWinAsClosed, rdWinFullscreen, rdWinMaximized, rdWinWindowed;
	@FXML
	private TextField txtWidth, txtHeight;

	@FXML
	private RadioButton rBtnPanelNothing, rBtnPanelLast, rBtnPanelSpecific;

	@FXML
	private VBox vPanel;
	@FXML
	private CheckBox chkRestoreLastFile, chkWarnUnsavedChanges;

	@FXML
	private Slider sldrThreshold;
	@FXML
	private TextField tfThreshold;

	public SettingsController() {
		super();
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FXML_PATH), this);
		FXMLUtil.setStyleSheet(this);
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

		// FXMLUtil.setIcon((Stage) root.getScene().getWindow(), Main.getLogoPath());

		sldrThreshold.valueProperty().addListener((e, newV, oldV) -> tfThreshold.setText(Math.round(newV.doubleValue()) + " dB"));
		sldrThreshold.setValue(Double.parseDouble(PropertiesIO.getProperty(Constants.SETTING_WATCHDOG_THRESHOLD, Double.toString(Watchdog.DEFAULT_THRESHOLD))));
		tfThreshold.setText(Math.round(sldrThreshold.getValue()) + " dB");
		// controls
		vPanel.disableProperty().bind(rBtnPanelSpecific.selectedProperty().not());
		// Init data
		chbBuffer.getItems().addAll(BUFFERS);
		chbDevice.getItems().addAll(ASIOController.getPossibleDriverStrings());
		chbBuffer.setValue(ASIOController.getInstance().getBufferSize());
		chbDevice.setValue(ASIOController.getInstance().getDevice());

		// language
		choiLanguage.setConverter(new StringConverter<Locale>() {

			@Override
			public String toString(Locale object) {
				String result="";
				if(object !=null) {
					result = object.getDisplayLanguage();
				}
				return result;
			}

			@Override
			public Locale fromString(String string) {
				return Locale.forLanguageTag(string);
			}
		});

		initWindowPanel();

		initSpecificPanel();

		for (Node n : grid.getChildren()) {
			try {
				Integer in = GridPane.getColumnIndex(n);
				if (in == null && n != null && n instanceof Label) {
					Label label = (Label) n;
					list.getItems().add(label.getText());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		list.getSelectionModel().selectedItemProperty().addListener((e, oldV, newV) -> {
			for (Node n : grid.getChildren()) {
				if (n instanceof Label) {
					Label label = (Label) n;
					if (label.getText().equals(newV)) {
						ensureVisible(scrollPane, label);
						break;
					}
				}
			}
		});

		btnSave.widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				FXMLUtil.setPrefWidthToMaximumRequired(btnSave, btnCancel);
				btnSave.widthProperty().removeListener(this);
			}
		});
		loadValues();
	}

	private static void ensureVisible(ScrollPane pane, Node node) {
		double width = pane.getContent().getBoundsInLocal().getWidth();
		double height = pane.getContent().getBoundsInLocal().getHeight();

		double x = node.getBoundsInParent().getMaxX();
		double y = node.getBoundsInParent().getMaxY();

		// scrolling values range from 0 to 1
		pane.setVvalue(y / height);
		pane.setHvalue(x / width);

		// just for usability
		node.requestFocus();
	}

	private void initWindowPanel() {
		initField(txtHeight);
		initField(txtWidth);
	}

	private void initField(final TextField field) {
		field.disableProperty().bind(rdWinWindowed.selectedProperty().not());
		field.setOnMouseClicked(e -> {
			rdWinWindowed.setSelected(true);
		});
		field.textProperty().addListener((e, oldV, newV) -> {
			boolean isNumber = true;
			for (char c : newV.toCharArray()) {
				if (!Character.isDigit(c)) {
					isNumber = false;
					break;
				}
			}
			if (!isNumber) {
				field.setText(oldV);
			}
		});
	}

	@FXML
	private void cancel(ActionEvent e) {
		close();
	}

	private void close() {
		Stage stage = (Stage) btnCancel.getScene().getWindow();
		stage.close();
	}

	private void initSpecificPanel() {
		vPanel.getChildren().clear();
		if (MainController.getInstance() != null) {
			for (String panel : MainController.getInstance().getPanels()) {
				RadioButton button = new RadioButton(panel);
				button.setToggleGroup(startUpPanel);
				vPanel.getChildren().add(button);
			}
			startUpPanel.getToggles().get(1).setSelected(true);
		}
	}

	private void loadFilePanel() {
		setSettingSecure(() -> chkRestoreLastFile.setSelected(Boolean.parseBoolean(PropertiesIO.getProperty(Constants.SETTING_RELOAD_LAST_FILE))), Constants.SETTING_RELOAD_LAST_FILE);
		setSettingSecure(() -> chkWarnUnsavedChanges.setSelected(Boolean.parseBoolean(PropertiesIO.getProperty(Constants.SETTING_WARN_UNSAVED_CHANGES))), Constants.SETTING_WARN_UNSAVED_CHANGES);
	}

	private void loadGUIPanel() {
		// db current or Peak
		setSettingSecure(() -> rBtndbCurrent.setSelected(!PropertiesIO.getProperty(Constants.SETTING_DB_LABEL_CURRENT).equals(Boolean.toString(false))), Constants.SETTING_DB_LABEL_CURRENT);
		setSettingSecure(() -> rBtndbPeak.setSelected(PropertiesIO.getProperty(Constants.SETTING_DB_LABEL_CURRENT).equals(Boolean.toString(false))), Constants.SETTING_DB_LABEL_CURRENT);

		// panel selection
		setSettingSecure(() -> rBtnPanelNothing.setSelected(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.NOTHING.name())), Constants.SETTING_RESTORE_PANEL);
		setSettingSecure(() -> rBtnPanelLast.setSelected(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.LAST.name())), Constants.SETTING_RESTORE_PANEL);
		if (PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL) != null && PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL).equals(RESTORE_PANEL.SPECIFIC.name())) {
			rBtnPanelSpecific.setSelected(true);
			if (PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC) != null) {
				for (Toggle t : startUpPanel.getToggles()) {
					ToggleButton btn = (ToggleButton) t;
					if (btn.getText().equals(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC))) {
						btn.setSelected(true);
						break;
					}
				}
			}
		}
		Task<Void> loadLanguages = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				String folder = Main.LOCALIZATION_FILES.replaceAll("\\.", "/");
				folder = folder.substring(0, folder.lastIndexOf("/"));
				String languageRootFile = Main.LOCALIZATION_FILES;
				languageRootFile = languageRootFile.substring(languageRootFile.lastIndexOf(".") + 1);
				List<Locale> languages = FXMLUtil.getLanguages(folder, languageRootFile, Constants.LANGUAGE_FILE_ENDING);
				Platform.runLater(() -> {
					choiLanguage.getItems().setAll(languages);
					choiLanguage.getSelectionModel().select(Main.getLanguage());
				});
				return null;
			}
		};
		new Thread(loadLanguages).start();
		
		//loadLanguages.run();
	}

	private void loadValues() {

		loadGUIPanel();
		loadFilePanel();
		loadWatchdogPanel();
	}

	private void loadWatchdogPanel() {
		setSettingSecure(() -> sldrThreshold.setValue(Double.parseDouble(PropertiesIO.getProperty(Constants.SETTING_WATCHDOG_THRESHOLD))), Constants.SETTING_WATCHDOG_THRESHOLD);
	}

	@FXML
	private void save(ActionEvent e) {
		// language
		PropertiesIO.setProperty(Constants.SETTING_LANGUAGE, choiLanguage.getValue().getLanguage());
		Main.setLanguage(choiLanguage.getValue());
		// window on open
		String windowOpen = WINDOW_OPEN.DEFAULT.toString();
		if (rdWinFullscreen.isSelected()) {
			windowOpen = WINDOW_OPEN.FULLSCREEN.toString();
		} else if (rdWinMaximized.isSelected()) {
			windowOpen = WINDOW_OPEN.MAXIMIZED.toString();
		} else if (rdWinWindowed.isSelected()) {
			windowOpen = WINDOW_OPEN.WINDOWED.toString() + "," + txtWidth.getText() + "," + txtHeight.getText();
		}
		PropertiesIO.setProperty(Constants.SETTING_WINDOW_OPEN, windowOpen, false);
		// gui db label
		PropertiesIO.setProperty(Constants.SETTING_DB_LABEL_CURRENT, Boolean.toString(rBtndbCurrent.isSelected()), false);
		// Panel restore
		if (rBtnPanelNothing.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.NOTHING.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, Integer.toString(-1), false);
		} else if (rBtnPanelLast.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.LAST.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, Integer.toString(-1), false);
		} else if (rBtnPanelSpecific.isSelected()) {
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL, RESTORE_PANEL.SPECIFIC.name(), false);
			PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC, ((ToggleButton) startUpPanel.getSelectedToggle()).getText(), false);
		} else {
			LOG.error("State should not be reached");
		}
		// File/saving
		PropertiesIO.setProperty(Constants.SETTING_RELOAD_LAST_FILE, Boolean.toString(chkRestoreLastFile.isSelected()), false);
		PropertiesIO.setProperty(Constants.SETTING_WARN_UNSAVED_CHANGES, Boolean.toString(chkWarnUnsavedChanges.isSelected()), false);
//watchdog
		PropertiesIO.setProperty(Constants.SETTING_WATCHDOG_THRESHOLD, Double.toString(sldrThreshold.getValue()));

		new Thread(() -> PropertiesIO.saveProperties()).start();

		ASIOController.getInstance().setBufferSize(chbBuffer.getValue().intValue());
		ASIOController.getInstance().setDevice(chbDevice.getValue());
		ASIOController.getInstance().restart();

		if (MainController.getInstance() != null) {
			MainController.getInstance().resetInfosFromDevice();
		}
		close();
	}

	/**
	 * secure in this case means it will not crash if setting can not be loaded
	 * 
	 * @param r
	 * @param setting
	 */
	private void setSettingSecure(Runnable r, String setting) {
		try {
			r.run();
		} catch (Exception e) {
			LOG.warn("Unable to load Setting \"" + setting + "\"");
		}
	}
}
