package gui.dialog;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.DriverInfo;
import gui.controller.MainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import main.Constants;
import main.Constants.WINDOW_OPEN;
import main.FXMLMain;
import main.Main;
import preferences.PropertiesIO;

public class IOChooserController implements Initializable {

	private static final Logger LOG = LogManager.getLogger(IOChooserController.class);
	@FXML
	private CheckBox errorReports;
	@FXML
	private BorderPane root;
	@FXML
	private ListView<DriverInfo> listIO;
	@FXML
	private Button btnStart, btnQuit;
	@FXML
	private Label lblDriverCount, lblName, lblVersion, lblASIOVersion, lblSampleRate, lblBuffer, lblLatencyIn, lblLatencyOut, lblInput, lblOutput;
	private Scene mainScene;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		Collection<DriverInfo> ioList = ASIOController.getPossibleDrivers();
		LOG.info("Loaded " + ioList.size() + " possible drivers");
		String appendix = " Driver";
		if (ioList.size() > 1) {
			appendix += "s";
		}
		errorReports.setSelected(Main.isErrorReporting());
		lblDriverCount.setText(ioList.size() + appendix);
		setDevices(ioList);
		listIO.setCellFactory(e -> new ListCell<>() {
			@Override
			protected void updateItem(DriverInfo info, boolean empty) {
				super.updateItem(info, empty);
				if (info == null || empty) {
					setText("");
				} else {
					setText(info.getName());
				}
			}
		});

		listIO.getSelectionModel().selectedItemProperty().addListener((e, oldV, info) -> {
			if (info == null) {
				for (Label tempLabel : new Label[] { lblName, lblVersion, lblASIOVersion, lblSampleRate, lblBuffer, lblLatencyIn, lblLatencyOut, lblInput, lblOutput }) {
					tempLabel.setText("");
				}
			} else {
				lblName.setText(info.getName());
				lblVersion.setText(Integer.toString(info.getVersion()));
				lblASIOVersion.setText(Integer.toString(info.getAsioVersion()));
				DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
				DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
				symbols.setGroupingSeparator('.');
				formatter.setDecimalFormatSymbols(symbols);
				String sampleRate = formatter.format(info.getSampleRate()) + " Hz";
				lblSampleRate.setText(sampleRate);
				lblBuffer.setText(Integer.toString(info.getBuffer()));
				double latency = (double) info.getBuffer() * 1 / info.getSampleRate() * 1000.0;
				double latencyIn = Math.round((latency + (info.getLatencyInput() / 1000.0)) * 10.0) / 10.0;
				double latencyOut = Math.round((latency + (info.getLatencyOutput() / 1000.0)) * 10.0) / 10.0;
				lblLatencyIn.setText(Double.toString(latencyIn) + " ms");
				lblLatencyOut.setText(Double.toString(latencyOut) + " ms");
				lblInput.setText(Integer.toString(info.getInputCount()));
				lblOutput.setText(Integer.toString(info.getOutputCount()));
			}
		});

		// Quit Button
		btnQuit.setOnAction(e -> {
			((Stage) root.getScene().getWindow()).close();
			FXMLMain.getInstance().close();
		});
		btnStart.disableProperty().bind(listIO.getSelectionModel().selectedItemProperty().isNull());

		if (listIO.getItems().size() > 0) {
			listIO.getSelectionModel().select(0);
		}

	}

	public void setDevices(Collection<DriverInfo> devices) {
		listIO.getItems().setAll(devices);
	}

	public void setMainScene(Scene scene) {
		mainScene = scene;
		btnStart.requestFocus();
	}

	public void startDebug() {
		// if (!listIO.getItems().isEmpty()) {
		start(new ActionEvent());
		// }
	}

	@FXML
	private void errorReports(ActionEvent e) {
		Main.setErrorReporting(errorReports.isSelected());
	}

	private void launchMain(String selectedIO) {
		if (selectedIO != null && !selectedIO.isEmpty()) {
			MainController.getInstance().initIO(selectedIO);
		}
		Stage stage = (Stage) listIO.getScene().getWindow();
		stage.setScene(mainScene);
		stage.setResizable(true);

		// Set resloution depending on settings
		Constants.WINDOW_OPEN windowBehaviour = WINDOW_OPEN.DEFAULT;
		String value = PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN);
		if (value != null && !value.isEmpty()) {
			try {
				if (value.contains(",")) {
					value = value.split(",")[0];
				}
				windowBehaviour = Constants.WINDOW_OPEN.valueOf(value);
			} catch (Exception e) {
				LOG.warn("Cannot parse " + value + " to enum");
			}
		}
		switch (windowBehaviour) {
		case MAXIMIZED:
			stage.setFullScreen(false);
			stage.setMaximized(true);
			break;
		case DEFAULT:
			stage.setFullScreen(false);
			stage.setMaximized(false);
			String windowed = PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN);
			try {
				String[] windowedSplit = windowed.split(",");
				if (windowedSplit.length >= 6) {
					stage.setFullScreen(Boolean.parseBoolean(windowedSplit[5]));
				}
				stage.setWidth(Integer.parseInt(windowedSplit[1]));
				stage.setHeight(Integer.parseInt(windowedSplit[2]));
				if (windowedSplit.length >= 5) {
					stage.setX(Integer.parseInt(windowedSplit[3]));
					stage.setY(Integer.parseInt(windowedSplit[4]));

				}
			} catch (Exception e) {
				LOG.warn("Unable to open Application with windowed parameters");
			}
			break;
		case FULLSCREEN:
			stage.setFullScreen(true);
			break;
		case WINDOWED:
			stage.setFullScreen(false);
			stage.setMaximized(false);
			int width = 800;
			int height = 600;
			String windowed2 = PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN);

			try {
				String[] windowedSplit = windowed2.split(",");
				width = Integer.parseInt(windowedSplit[1]);
				height = Integer.parseInt(windowedSplit[2]);
			} catch (Exception e) {
				LOG.warn("Unable to open Application with windowed parameters");
			}
			stage.setWidth(width);
			stage.setHeight(height);
			stage.centerOnScreen();
			break;

		}

		stage.xProperty().addListener(e -> writePos(stage));
		stage.yProperty().addListener(e -> writePos(stage));
		stage.widthProperty().addListener(e -> writePos(stage));
		stage.heightProperty().addListener(e -> writePos(stage));
	}

	private void writePos(final Stage stage) {
		String value = PropertiesIO.getProperty(Constants.SETTING_WINDOW_OPEN).split(",")[0];
		if (value.contains(",")) {
			value = value.split(",")[0];
		}
		if (Objects.equals(Constants.WINDOW_OPEN.DEFAULT, Constants.WINDOW_OPEN.valueOf(value))) {
			if (value != null && !value.isEmpty()) {
				value += ",";
			}
			value = value += Math.round(stage.getWidth()) + "," + Math.round(stage.getHeight()) + "," + Math.round(stage.getX()) + "," + Math.round(stage.getY()) + "," + Boolean.toString(stage.isFullScreen());
			PropertiesIO.setProperty(Constants.SETTING_WINDOW_OPEN, value);
		}
	}

	@FXML
	private void start(ActionEvent e) {
		String selectedIO = null;
		if (!Main.isDebug()) {
			selectedIO = listIO.getSelectionModel().getSelectedItem().getName();
		} else if (!listIO.getItems().isEmpty()) {
			selectedIO = listIO.getItems().get(0).getName();
		} else {
			LOG.info("Starting without selected driver, for DEBUG purposes only!");
		}
		LOG.info("Loading Main-Window with selected Driver \"" + selectedIO + "\"");
		launchMain(selectedIO);
		// }
		Stage stage = null;
		try {
			stage = (Stage) listIO.getScene().getWindow();
		} catch (Exception ex) {
			LOG.warn("Unable to read stage", e);
		}
		if (stage != null) {
			stage.close();
		}
		e.consume();
	}
}
