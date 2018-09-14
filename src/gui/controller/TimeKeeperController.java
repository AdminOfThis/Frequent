package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.TimeKeeper;
import data.Channel;
import data.Cue;
import data.FileIO;
import gui.utilities.DoughnutChart;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class TimeKeeperController implements Initializable {

	private static final Logger			LOG				= Logger.getLogger(TimeKeeperController.class);

	private static TimeKeeperController	instance;

	private static final double			REFRESH_RATE	= 1000;
	@FXML
	private Parent						paneCue;
	@FXML
	private VBox						piePane;
	@FXML
	private PieChart					timeChart;
	@FXML
	private Button						btnTime;
	@FXML
	private ToggleButton				btnStart;
	@FXML
	private TableView<Cue>				cueTable;
	@FXML
	private TableColumn<Cue, String>	colName, colTime, colChannel;
	@FXML
	private TextField					txtCue, txtCueName;
	@FXML
	private ComboBox<Channel>			choiceCueChannel;
	@FXML
	private Label						lblTime;

	/**************
	 * contextmenu
	 *************/
	@FXML
	private MenuItem					cxtResetChannel, cxtDeleteCue;

	private Timeline					timeKeeperLine;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;

		initTimeKeeper();
		enableContextMenu(false);
	}

	public static TimeKeeperController getInstance() {
		return instance;
	}

	private void initTimeKeeper() {
		LOG.debug("Loading TimeKeeper");
		piePane.getChildren().clear();
		timeChart = new DoughnutChart(FXCollections.observableArrayList());
		piePane.getChildren().add(timeChart);
		btnTime.disableProperty().bind(btnStart.selectedProperty().not());
		btnStart.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					btnStart.setText("Stop");
					if (!cueTable.getItems().isEmpty()) {
						cueTable.getSelectionModel().select(0);
					}
					cueTable.requestFocus();
				} else {
					btnStart.setText("Start");
				}
			}
		});
		txtCue.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					TimeKeeper.getInstance().addCue(new Cue(txtCue.getText().trim()));
					cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
					txtCue.clear();
					event.consume();
				}
			}
		});
		colName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
		colChannel.setCellValueFactory(e -> {
			String result = "";
			if (e.getValue().getChannelToSelect() != null) {
				result = e.getValue().getChannelToSelect().getName();
			}
			return new SimpleStringProperty(result);
		});
		colTime.setStyle("-fx-alignment: CENTER;");
		colTime.setCellValueFactory(e -> {
			long time = e.getValue().getTime();
			String result = "--:--";
			if (time > 0) {
				time = time / 1000;
				int mins = (int) (time / 60);
				int secs = (int) (time % 60);
				result = String.format("%02d", mins) + ":" + String.format("%02d", secs);
			}
			return new SimpleStringProperty(result);
		});
		choiceCueChannel.setConverter(new StringConverter<Channel>() {

			@Override
			public String toString(Channel object) {
				if (object == null) {
					LOG.info("");
					return "- NONE -";
				}
				return object.getName();
			}

			@Override
			public Channel fromString(String string) {
				return null;
			}
		});
		// EDIT
		cueTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Cue>() {

			@Override
			public void changed(ObservableValue<? extends Cue> observable, Cue oldValue, Cue newValue) {
				enableContextMenu(newValue != null);

				txtCueName.setDisable(newValue == null || btnStart.isSelected());
				choiceCueChannel.setDisable(newValue == null || btnStart.isSelected());
				if (newValue == null) {
					txtCueName.setText(null);
					choiceCueChannel.getSelectionModel().select(-1);
				} else {
					txtCueName.setText(newValue.getName());
					choiceCueChannel.getSelectionModel().select(newValue.getChannelToSelect());
				}
			}

		});
		txtCueName.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				cueTable.getSelectionModel().getSelectedItem().setName(txtCueName.getText());
			} else if (e.getCode() == KeyCode.ESCAPE) {
				txtCueName.setText(cueTable.getSelectionModel().getSelectedItem().getName());
			}
			cueTable.refresh();
		});
		choiceCueChannel.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Channel>() {

			@Override
			public void changed(ObservableValue<? extends Channel> observable, Channel oldValue, Channel newValue) {
				Cue cue = cueTable.getSelectionModel().getSelectedItem();
				if (cue != null) {
					cue.setChannelToSelect(newValue);
				}
				cueTable.refresh();
			}
		});
		// Spacebar
		// paneCue.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
		// System.out.println("Test");
		// });
		cueTable.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				toggleTimer();
			} else if (e.getCode() == KeyCode.SPACE) {
				round();
			}
		});
		btnTime.disableProperty().bind(btnStart.selectedProperty().not());
	}

	public void setChannels(List<Channel> list) {
		Channel selected = choiceCueChannel.getSelectionModel().getSelectedItem();
		choiceCueChannel.getItems().setAll(list);
		if (selected != null) {
			choiceCueChannel.getSelectionModel().select(selected);
		}
	}

	public void toggleTimer() {
		btnStart.fire();
	}

	public void round() {
		btnTime.fire();
	}

	@FXML
	private void toggleTimerStart(ActionEvent e) {
		if (btnStart.isSelected()) {
			LOG.info("Starting timer");
			txtCueName.setDisable(true);
			choiceCueChannel.setDisable(true);
			lblTime.setText("00:00");
			timeChart.getData().clear();
			TimeKeeper.getInstance().reset();
			TimeKeeper.getInstance().round();
			timeKeeperLine = new Timeline(new KeyFrame(Duration.millis(REFRESH_RATE), event -> {
				String name = TimeKeeper.getInstance().getActiveCue().getName();
				Data data = null;
				for (Data temp : timeChart.getData()) {
					if (temp.getName().equals(name)) {
						data = temp;
						break;
					}
				}
				if (data == null) {
					data = new Data(name, TimeKeeper.getInstance().getTimeRunning());
					timeChart.getData().add(data);
				}
				data.setPieValue(TimeKeeper.getInstance().getRoundTime());
				TimeKeeper.getInstance().getActiveCue().setTime(TimeKeeper.getInstance().getRoundTime());
				cueTable.refresh();
				// total time
				long time = System.currentTimeMillis() - TimeKeeper.getInstance().getStartTime();
				time = time / 1000;
				int mins = (int) (time / 60);
				int secs = (int) (time % 60);
				lblTime.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
			}));
			timeKeeperLine.setCycleCount(Timeline.INDEFINITE);
			timeKeeperLine.playFromStart();
			cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
			cueTable.getSelectionModel().select(0);
			if (TimeKeeper.getInstance().getActiveCue().getChannelToSelect() != null) {
				MainController.getInstance().setSelectedChannel(TimeKeeper.getInstance().getActiveCue().getChannelToSelect());
			}
		} else {
			LOG.info("Stopping timer");
			txtCueName.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			choiceCueChannel.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			timeKeeperLine.stop();
			TimeKeeper.getInstance().reset();
		}
	}

	@FXML
	private void round(ActionEvent e) {
		TimeKeeper.getInstance().round();
		Cue activeCue = TimeKeeper.getInstance().getActiveCue();
		LOG.info("Jump to next cue: " + activeCue.getName());
		cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
		cueTable.getSelectionModel().select(TimeKeeper.getInstance().getActiveIndex());
		if (TimeKeeper.getInstance().getActiveCue().getChannelToSelect() != null) {
			MainController.getInstance().setSelectedChannel(TimeKeeper.getInstance().getActiveCue().getChannelToSelect());
		}
	}

	ToggleButton getStartButton() {
		return btnStart;
	}

	Button getRoundButton() {
		return btnTime;
	}

	@FXML
	private void deleteCue(ActionEvent e) {
		if (!btnStart.isSelected()) {
			if (cueTable.getSelectionModel().getSelectedItem() != null) {
				Cue del = cueTable.getSelectionModel().getSelectedItem();
				int index = cueTable.getSelectionModel().getSelectedIndex();
				TimeKeeper.getInstance().removeCue(del);
				cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
				if (cueTable.getItems().size() > index) {
					cueTable.getSelectionModel().select(index);
				} else {
					cueTable.getSelectionModel().select(cueTable.getItems().size() - 1);
				}
			}
		}
	}

	public void show(boolean value) {
		paneCue.setVisible(value);
		paneCue.setManaged(value);
		if (value) {
			cueTable.requestFocus();
		}
	}

	public void save(File file) {
		LOG.info("Saving cues");
		List<Serializable> list = new ArrayList<>();
		for (Cue c : cueTable.getItems()) {
			list.add(c);
		}
		boolean result = FileIO.save(list, file);
		if (result) {
			LOG.info("Saving successful");
		} else {
			LOG.warn("Unable to save");
		}
	}

	public void refresh() {
		cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
		if (ASIOController.getInstance() != null) {
			choiceCueChannel.getItems().setAll(ASIOController.getInstance().getInputList());
		}
	}

	private void enableContextMenu(boolean b) {
		cxtResetChannel.setDisable(!b);
		cxtDeleteCue.setDisable(!b);
	}

	@FXML
	private void resetChannel(ActionEvent e) {
		Cue cue = cueTable.getSelectionModel().getSelectedItem();
		if (cue != null) {
			cue.setChannelToSelect(null);
		}
	}
}
