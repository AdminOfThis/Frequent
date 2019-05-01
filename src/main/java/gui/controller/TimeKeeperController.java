package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.TimeKeeper;
import data.Channel;
import data.Cue;
import data.FileIO;
import gui.utilities.DoughnutChart;
import javafx.animation.AnimationTimer;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public class TimeKeeperController implements Initializable {

	private static final Logger			LOG	= LogManager.getLogger(TimeKeeperController.class);
	private static TimeKeeperController	instance;
	@FXML
	private Parent						paneCue;
	@FXML
	private StackPane					piePane;
	@FXML
	private PieChart					timeChart;
	@FXML
	private Button						btnTime;
	@FXML
	private SplitMenuButton				btnStart;
	@FXML
	private MenuItem					btnStop;
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
	@FXML
	private GridPane					infoPane;
	/**************
	 * contextmenu
	 *************/
	@FXML
	private MenuItem					cxtResetChannel, cxtDeleteCue;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		infoPane.setVisible(false);
		initTimeKeeper();
		enableContextMenu(false);
	}

	public static TimeKeeperController getInstance() {
		return instance;
	}

	private void initTimeKeeper() {
		LOG.debug("Loading TimeKeeper");
		initChart();
		initButtons();
		initTable();
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
				txtCueName.setDisable(newValue == null || TimeKeeper.getInstance().getActiveIndex() < 0);
				choiceCueChannel.setDisable(newValue == null || TimeKeeper.getInstance().getActiveIndex() < 0);
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
	}

	private void initButtons() {
		// btnTime.disableProperty().bind(btnStart.selectedProperty().not());
		btnStart.setOnAction(e -> {
			if (TimeKeeper.getInstance().getActiveIndex() < 0) {
				startTimer();
				btnStart.setText("Pause");
			} else {
				TimeKeeper.getInstance().pause();
				if (btnStart.getText().equals("Pause")) {
					btnStart.setText("Continue");
				} else {
					btnStart.setText("Pause");
				}
			}
			e.consume();
		});
		btnStop.setOnAction(e -> stopTimer());
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
	}

	private void stopTimer() {
		LOG.info("Stopping timer");
		txtCueName.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
		choiceCueChannel.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
		TimeKeeper.getInstance().reset();
		// Update GUI
		btnStart.setText("Start");
	}

	private void startTimer() {
		txtCueName.setDisable(true);
		choiceCueChannel.setDisable(true);
		lblTime.setText("00:00");
		timeChart.getData().clear();
		TimeKeeper.getInstance().reset();
		TimeKeeper.getInstance().round();
		AnimationTimer timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				if (TimeKeeper.getInstance().getActiveIndex() >= 0) {
					String name = TimeKeeper.getInstance().getActiveCue().getName();
					Data data = null;
					for (Data temp : timeChart.getData()) {
						if (temp.getName().equals(name)) {
							data = temp;
							break;
						}
					}
					if (data == null) {
						data = new Data(name, TimeKeeper.getInstance().getRoundTime());
						timeChart.getData().add(data);
					}
					data.setPieValue(TimeKeeper.getInstance().getRoundTime());
					TimeKeeper.getInstance().getActiveCue().setTime(TimeKeeper.getInstance().getRoundTime());
					cueTable.refresh();
					// total time
					long time = TimeKeeper.getInstance().getTimeRunning();
					time = time / 1000;
					int mins = (int) (time / 60);
					int secs = (int) (time % 60);
					lblTime.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
				}
			}
		};
		timer.start();
		while (TimeKeeper.getInstance().getActiveIndex() < 0) {}
		cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
		cueTable.getSelectionModel().select(0);
		cueTable.refresh();
		if (TimeKeeper.getInstance().getActiveCue().getChannelToSelect() != null) {
			MainController.getInstance().setSelectedChannel(TimeKeeper.getInstance().getActiveCue().getChannelToSelect());
		}
		// Updating GUI
		if (!cueTable.getItems().isEmpty()) {
			cueTable.getSelectionModel().select(0);
		}
		cueTable.requestFocus();
	}

	private void initTable() {
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
	}

	private void initChart() {
		timeChart = new DoughnutChart(FXCollections.observableArrayList());
		piePane.setOnMouseClicked(e -> {
			if (MouseButton.SECONDARY.equals(e.getButton())) {
				ContextMenu menu = new ContextMenu();
				MenuItem btnClear = new MenuItem("Clear Chart");
				btnClear.setDisable(timeChart.getData().isEmpty() || TimeKeeper.getInstance().getTimeRunning() > 0);
				btnClear.setOnAction(ex -> {
					timeChart.getData().clear();
					lblTime.setText("--:--");
				});
				menu.getItems().add(btnClear);
				menu.show(timeChart, e.getScreenX(), e.getScreenY());
			}
		});
		piePane.getChildren().add(0, timeChart);
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
	private void round(ActionEvent e) {
		TimeKeeper.getInstance().round();
		Cue activeCue = TimeKeeper.getInstance().getActiveCue();
		LOG.info("Jump to next cue: " + activeCue.getName());
		cueTable.getItems().setAll(TimeKeeper.getInstance().getCueList());
		cueTable.getSelectionModel().select(TimeKeeper.getInstance().getActiveIndex());
		if (TimeKeeper.getInstance().getActiveCue().getChannelToSelect() != null) {
			MainController.getInstance().setSelectedChannel(TimeKeeper.getInstance().getActiveCue().getChannelToSelect());
		}
		e.consume();
	}

	@FXML
	private void deleteCue(ActionEvent e) {
		if (TimeKeeper.getInstance().getActiveIndex() < 0 && cueTable.getSelectionModel().getSelectedItem() != null) {
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
		e.consume();
	}

	public SplitMenuButton getStartButton() {
		return btnStart;
	}

	public Button getRoundButton() {
		return btnTime;
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

	private void enableContextMenu(boolean value) {
		cxtResetChannel.setDisable(!value);
		cxtDeleteCue.setDisable(!value);
	}

	@FXML
	private void resetChannel(ActionEvent e) {
		Cue cue = cueTable.getSelectionModel().getSelectedItem();
		if (cue != null) {
			cue.setChannelToSelect(null);
		}
		e.consume();
	}
}
