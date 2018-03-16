package gui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.TimeKeeper;
import data.Channel;
import data.Cue;
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
import javafx.scene.control.ChoiceBox;
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

public class TimeKeeperController implements Initializable {

	private static final Logger			LOG	= Logger.getLogger(TimeKeeperController.class);

	@FXML
	private Parent						paneCue;
	@FXML
	private VBox						piePane;
	@FXML
	private PieChart					timeChart;
	@FXML
	private MenuItem					menuCueRound;
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
	private ChoiceBox<Channel>			choiceCueChannel;
	@FXML
	private Label						lblTime;

	private TimeKeeper					timeKeeper;
	private Timeline					timeKeeperLine;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initTimeKeeper();

	}

	private void initTimeKeeper() {
		LOG.info("Loading TimeKeeper");
		piePane.getChildren().clear();
		timeChart = new DoughnutChart(FXCollections.observableArrayList());
		piePane.getChildren().add(timeChart);
		timeKeeper = new TimeKeeper();
		menuCueRound.disableProperty().bind(btnStart.selectedProperty().not());
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
					if (timeKeeper != null) {
						timeKeeper.addCue(new Cue(txtCue.getText().trim()));
						cueTable.getItems().setAll(timeKeeper.getCueList());
						txtCue.clear();
					}
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
		// EDIT
		choiceCueChannel.getItems().setAll(ASIOController.getInstance().getInputList());
		cueTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Cue>() {

			@Override
			public void changed(ObservableValue<? extends Cue> observable, Cue oldValue, Cue newValue) {
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
		// Spacebar
		// paneCue.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
		// System.out.println("Test");
		// });
		cueTable.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.DELETE) {
				deleteCue(new ActionEvent());
			}
		});
		btnTime.disableProperty().bind(btnStart.selectedProperty().not());
	}

	@FXML
	private void toggleTimerStart(ActionEvent e) {
		if (btnStart.isSelected()) {
			txtCueName.setDisable(true);
			choiceCueChannel.setDisable(true);
			lblTime.setText("00:00");
			timeChart.getData().clear();
			timeKeeper.reset();
			timeKeeper.getActiveCue();
			timeKeeperLine = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
				String name = timeKeeper.getActiveCue().getName();
				Data data = null;
				for (Data temp : timeChart.getData()) {
					if (temp.getName().equals(name)) {
						data = temp;
						break;
					}
				}
				if (data == null) {
					data = new Data(name, timeKeeper.getTimeRunning());
					timeChart.getData().add(data);
				}
				data.setPieValue(timeKeeper.getRoundTime());
				timeKeeper.getActiveCue().setTime(timeKeeper.getRoundTime());
				cueTable.refresh();
				// total time
				long time = System.currentTimeMillis() - timeKeeper.getStartTime();
				time = time / 1000;
				int mins = (int) (time / 60);
				int secs = (int) (time % 60);
				lblTime.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
			}));
			timeKeeperLine.setCycleCount(Timeline.INDEFINITE);
			timeKeeperLine.playFromStart();
			cueTable.getItems().setAll(timeKeeper.getCueList());
			cueTable.getSelectionModel().select(0);
		} else {
			txtCueName.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			choiceCueChannel.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			timeKeeperLine.stop();
			timeKeeper.reset();
		}
	}


	public void timerToggle() {
		toggleTimerStart(new ActionEvent());
	}


	@FXML
	private void round(ActionEvent e) {
		if (timeKeeper != null) {
			timeKeeper.round();
			timeKeeper.getActiveCue();
			cueTable.getItems().setAll(timeKeeper.getCueList());
			cueTable.getSelectionModel().select(timeKeeper.getActiveIndex());
			if (timeKeeper.getActiveCue().getChannelToSelect() != null) {
				MainController.getInstance().setSelectedChannel(timeKeeper.getActiveCue().getChannelToSelect());

			}
		}
	}

	@FXML
	private void deleteCue(ActionEvent e) {
		if (!btnStart.isSelected()) {
			if (cueTable.getSelectionModel().getSelectedItem() != null) {
				Cue del = cueTable.getSelectionModel().getSelectedItem();
				if (timeKeeper != null) {
					int index = cueTable.getSelectionModel().getSelectedIndex();
					timeKeeper.removeCue(del);
					cueTable.getItems().setAll(timeKeeper.getCueList());
					if (cueTable.getItems().size() > index) {
						cueTable.getSelectionModel().select(index);
					} else {
						cueTable.getSelectionModel().select(cueTable.getItems().size() - 1);
					}
				}
			}
		}
	}

	public void show(boolean value) {
		paneCue.setVisible(value);
		paneCue.setManaged(!value);
		if (value) {
			cueTable.requestFocus();
		}
	}

}
