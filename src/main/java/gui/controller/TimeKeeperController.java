package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.ChurchToolsAdapter;
import control.TimeKeeper;
import data.Channel;
import data.Cue;
import data.FileIO;
import dialog.InformationDialog;
import gui.FXMLUtil;
import gui.utilities.DoughnutChart;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.util.Pair;
import javafx.util.StringConverter;
import main.Main;

public class TimeKeeperController implements Initializable {

	private static final Logger			LOG	= Logger.getLogger(TimeKeeperController.class);
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
	private ToggleButton				btnInfo;
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
		btnInfo.selectedProperty().bindBidirectional(infoPane.visibleProperty());
		btnInfo.selectedProperty().addListener((o, oldV, newV) -> {
			if (newV) {
				refreshAdditionalInfos();
			}
		});
		initTimeKeeper();
		enableContextMenu(false);
	}

	private void refreshAdditionalInfos() {
		LinkedHashMap<String, String> map = ChurchToolsAdapter.getInstance().getAdditionalInfos();
		StackPane p = (StackPane) infoPane.getParent();
		p.getChildren().remove(infoPane);
		infoPane = new GridPane();
		infoPane.setHgap(25.0);
		infoPane.setVgap(5.0);
		infoPane.setStyle("-fx-background-color: -fx-background");
		btnInfo.selectedProperty().bindBidirectional(infoPane.visibleProperty());
		infoPane.addColumn(1);
		infoPane.getColumnConstraints().add(new ColumnConstraints());
		infoPane.getColumnConstraints().add(new ColumnConstraints());
		infoPane.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);
		p.getChildren().add(1, infoPane);
		HBox top = new HBox();
		top.setAlignment(Pos.CENTER);
		top.setSpacing(20.0);
		Label lblEvent = new Label(map.get("Event"));
		lblEvent.setStyle("-fx-font-size: 20px");
		top.getChildren().add(lblEvent);
		Label lblTime = new Label(map.get("Time"));
		top.getChildren().add(lblTime);
		infoPane.getChildren().add(top);
		GridPane.setColumnIndex(top, 0);
		GridPane.setRowIndex(top, 0);
		GridPane.setColumnSpan(top, 2);
		GridPane.setHalignment(top, HPos.CENTER);
		map.remove("Event");
		map.remove("Time");
		int i = 1;
		for (String key : map.keySet()) {
			Label lblKey = new Label(key + ":");
			infoPane.addRow(i, lblKey);
			infoPane.getRowConstraints().add(new RowConstraints(30));
			Label lblValue = new Label(map.get(key));
			infoPane.getChildren().add(lblValue);
			GridPane.setColumnIndex(lblValue, 1);
			GridPane.setRowIndex(lblValue, i);
			i++;
		}
		for (int j = 0; j < infoPane.getRowConstraints().size(); j++) {
			infoPane.getRowConstraints().get(j).setPrefHeight(-1);
			infoPane.getRowConstraints().get(j).setVgrow(Priority.SOMETIMES);
		}
		infoPane.addRow(i);
		infoPane.getRowConstraints().add(new RowConstraints(-1));
		infoPane.getRowConstraints().get(infoPane.getRowConstraints().size() - 1).setVgrow(Priority.ALWAYS);
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

	@FXML
	private void loadFromChurchTools(ActionEvent e) {
		MainController.getInstance().setStatus("Loading from Churchtools", -1);
		boolean isLoginSet = ChurchToolsAdapter.getInstance().isLoggedIn();
		if (!isLoginSet) {
			Pair<String, String> loginCredentials = getLoginData();
			if (loginCredentials != null) {
				ChurchToolsAdapter.getInstance().setPassword(loginCredentials.getValue());
				ChurchToolsAdapter.getInstance().setLogin(loginCredentials.getKey());
			} else {
				MainController.getInstance().setStatus("Loading from Churchtools cancelled", 0);
				return;
			}
		}
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() {
				if (!ChurchToolsAdapter.getInstance().isLoggedIn()) {
					Platform.runLater(() -> {
						Dialog<?> dialog = new InformationDialog("Unable to log into ChurchTools");
						dialog.showAndWait();
					});
				}
				ArrayList<Cue> cues = ChurchToolsAdapter.getInstance().loadCues();
				if (cues != null) {
					TimeKeeper.getInstance().set(cues);
					Platform.runLater(() -> {
						MainController.getInstance().setStatus("Loading finished", 0);
						refresh();
					});
				} else {
					Platform.runLater(() -> {
						MainController.getInstance().setStatus("Unable to load from ChurchTools", 0);
					});
				}
				return null;
			}
		};
		new Thread(task).start();
		e.consume();
	}

	private Pair<String, String> getLoginData() {
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Login ChurchTools");
		dialog.setHeaderText("Please enter your ChurchTools Login Credentials");
		dialog.initModality(Modality.NONE);
		dialog.initOwner(paneCue.getScene().getWindow());
		FXMLUtil.setStyleSheet(dialog.getDialogPane());
		dialog.getDialogPane().setStyle(Main.getStyle());
		// Set the icon (must be included in the project).
		// Set the button types.
		ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));
		TextField username = new TextField();
		username.setText(ChurchToolsAdapter.getInstance().getUserName());
		username.setPromptText("Username");
		PasswordField password = new PasswordField();
		password.setPromptText("Password");
		grid.add(new Label("Username:"), 0, 0);
		grid.add(username, 1, 0);
		grid.add(new Label("Password:"), 0, 1);
		grid.add(password, 1, 1);
		// Enable/Disable login button depending on whether a username was
		// entered.
		Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
		loginButton.setDisable(username.getText().isEmpty());
		// Do some validation (using the Java 8 lambda syntax).
		username.textProperty().addListener((observable, oldValue, newValue) -> {
			loginButton.setDisable(newValue.trim().isEmpty());
		});
		dialog.getDialogPane().setContent(grid);
		// Convert the result to a username-password-pair when the login button
		// is clicked.
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == loginButtonType) { return new Pair<>(username.getText(), password.getText()); }
			return null;
		});
		// Request focus on the username field by default.
		if (username.getText().isEmpty()) {
			Platform.runLater(() -> username.requestFocus());
		} else {
			Platform.runLater(() -> password.requestFocus());
		}
		Optional<Pair<String, String>> result = dialog.showAndWait();
		if (result.isPresent()) { return result.get(); }
		return null;
	}
}
