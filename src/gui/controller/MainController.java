package gui.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.synthbot.jasiohost.AsioChannel;

import control.ASIOController;
import control.TimeKeeper;
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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.Main;

public class MainController implements Initializable {

	private static final Logger			LOG	= Logger.getLogger(MainController.class);
	private static MainController		instance;
	@FXML
	private PieChart					timeChart;
	@FXML
	private ToggleButton				toggleStart, toggleCue, btnStart;
	@FXML
	private Button						btnTime;
	@FXML
	private TextField					txtCue, txtCueName;
	@FXML
	private ChoiceBox<AsioChannel>		choiceCueChannel;
	@FXML
	private BorderPane					root;
	@FXML
	private SplitPane					contentPane;
	@FXML
	private Menu						driverMenu;
	@FXML
	private MenuItem					closeMenu, menuCueRound, menuTimerStart, menuCueDelete;
	@FXML
	private ListView<AsioChannel>		channelList;
	@FXML
	private TableView<Cue>				cueTable;
	@FXML
	private TableColumn<Cue, String>	colName, colTime, colChannel;
	@FXML
	private CheckMenuItem				menuShowCue, menuStartFFT;
	@FXML
	private Parent						paneCue;
	@FXML
	private VBox						piePane;
	@FXML
	private Label						lblDriver, lblLatency, lblTime;
	private ASIOController				controller;
	private TimeKeeper					timeKeeper;
	private Timeline					timeKeeperLine;
	private FFTController				fftcontroller;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		initMenu();
		initChannelList();
		initTimekeeper();
		initFullScreen();
		initChart();
	}

	private void initChart() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("./../gui/FFT.fxml"));
			Parent p = loader.load();
			fftcontroller = loader.getController();
			fftcontroller.setDriver(controller);
			contentPane.getItems().add(0, p);
		}
		catch (Exception e) {
			LOG.error("Unable to load FFT Chart", e);
		}
	}

	private void initFullScreen() {
		root.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.F11) {
				Stage stage = (Stage) root.getScene().getWindow();
				stage.setFullScreenExitHint("");
				stage.setFullScreen(!stage.isFullScreen());
			}
		});
	}

	private void initTimekeeper() {
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
				result = e.getValue().getChannelToSelect().getChannelName();
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
		choiceCueChannel.setItems(channelList.getItems());
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

	private void initChannelList() {
		channelList.setCellFactory(e -> new ListCell<AsioChannel>() {

			@Override
			public void updateItem(AsioChannel item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
				} else {
					setText(item.getChannelName());
				}
			}
		});
		channelList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<AsioChannel>() {

			@Override
			public void changed(ObservableValue<? extends AsioChannel> observable, AsioChannel oldValue, AsioChannel newValue) {
				controller.setActiveChannel(newValue);
			}
		});
	}

	private void initMenu() {
		root.addEventHandler(KeyEvent.ANY, e -> {
			if (e.getCode() == KeyCode.SPACE) {
				toggleFFT(new ActionEvent());
				e.consume();
			}
		});
		toggleStart.selectedProperty().bindBidirectional(menuStartFFT.selectedProperty());
		toggleStart.selectedProperty().addListener(e -> {
			Image image;
			if (toggleStart.isSelected()) {
				image = new Image("./gui/res/sample_selected.png");
			} else {
				image = new Image("./gui/res/sample.png");
			}
			ImageView view = new ImageView(image);
			view.setFitWidth(25.0);
			view.setFitHeight(25.0);
			toggleStart.setGraphic(view);
		});
		// final ToggleGroup group = new ToggleGroup();
		// KeyCombination comb = KeyCombination.keyCombination("D");
		// driverMenu.setAccelerator(comb);
		// for (String driverName :
		// RadioMenuItem driverCheckMenu = new RadioMenuItem(driverName);
		// driverCheckMenu.setToggleGroup(group);
		// driverMenu.getItems().add(driverCheckMenu);
		// }
		toggleCue.selectedProperty().addListener(e -> {
			Image image;
			if (toggleCue.isSelected()) {
				image = new Image("./gui/res/cue_selected.png");
			} else {
				image = new Image("./gui/res/cue.png");
			}
			ImageView view = new ImageView(image);
			view.setFitWidth(25.0);
			view.setFitHeight(25.0);
			toggleCue.setGraphic(view);
		});
		toggleCue.selectedProperty().bindBidirectional(menuShowCue.selectedProperty());
		paneCue.visibleProperty().bind(menuShowCue.selectedProperty());
		paneCue.managedProperty().bind(menuShowCue.selectedProperty());
		menuShowCue.selectedProperty().addListener(e -> {
			if (menuShowCue.isSelected()) {
				cueTable.requestFocus();
			}
		});
		// Timer
		menuTimerStart.setOnAction(e -> btnStart.fire());
		menuCueDelete.disableProperty().bind(btnStart.selectedProperty().or(cueTable.getSelectionModel().selectedItemProperty().isNull()));
		// Close Button
		closeMenu.setOnAction(e -> {
			Main.close();
		});
	}

	public void initIO(String ioName) {
		controller = new ASIOController(ioName);
		fftcontroller.setDriver(controller);
		channelList.getItems().setAll(controller.getInputList());
		lblDriver.setText(ioName);
		lblLatency.setText(controller.getLatency() + " ms");
	}

	public void setChannelList(List<AsioChannel> list) {
		channelList.getItems().setAll(list);
	}

	@FXML
	private void toggleTimerStart(ActionEvent e) {
		if (btnStart.isSelected()) {
			menuTimerStart.setText("Stop Timer");
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
			menuTimerStart.setText("Start Timer");
			txtCueName.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			choiceCueChannel.setDisable(cueTable.getSelectionModel().selectedItemProperty().isNull().get());
			timeKeeperLine.stop();
			timeKeeper.reset();
		}
	}

	@FXML
	private void round(ActionEvent e) {
		if (timeKeeper != null) {
			timeKeeper.round();
			timeKeeper.getActiveCue();
			cueTable.getItems().setAll(timeKeeper.getCueList());
			cueTable.getSelectionModel().select(timeKeeper.getActiveIndex());
			if (timeKeeper.getActiveCue().getChannelToSelect() != null) {
				channelList.getSelectionModel().select(timeKeeper.getActiveCue().getChannelToSelect());
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

	@FXML
	private void toggleFFT(ActionEvent e) {
		fftcontroller.play(!fftcontroller.isPlaying());
		toggleStart.setSelected(fftcontroller.isPlaying());
	}
}
