package gui.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import gui.utilities.FXMLUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.Main;

public class MainController implements Initializable {

	private static final String		FFT_PATH		= "./../gui/FFT.fxml";
	private static final String		TIMEKEEPER_PATH	= "./../gui/TimeKeeper.fxml";
	private static final Logger		LOG				= Logger.getLogger(MainController.class);
	private static MainController	instance;
	@FXML
	private ToggleButton			toggleFFT, toggleCue;
	@FXML
	private BorderPane				root;
	@FXML
	private SplitPane				contentPane;
	@FXML
	private Menu					driverMenu;
	@FXML
	private MenuItem				closeMenu;
	@FXML
	private ListView<Channel>		channelList;
	@FXML
	private CheckMenuItem			menuShowCue, menuStartFFT;
	@FXML
	private Label					lblDriver, lblLatency;
	private ASIOController			controller;
	private FFTController			fftController;
	private TimeKeeperController	timeKeeperController;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		initMenu();
		initChannelList();
		initFullScreen();
		initTimekeeper();
		initChart();
	}

	private void initChart() {
		Parent p = FXMLUtil.loadFXML(FFT_PATH);
		if (p != null) {
			fftController = (FFTController) FXMLUtil.getController();
			fftController.setDriver(controller);
			contentPane.getItems().add(0, p);
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initTimekeeper() {
		Parent p = FXMLUtil.loadFXML(TIMEKEEPER_PATH);
		if (p != null) {
			timeKeeperController = (TimeKeeperController) FXMLUtil.getController();
			root.setRight(p);
			timeKeeperController.show(toggleCue.selectedProperty().get());
		} else {
			LOG.warn("Unable to load TimeKeeper");
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

	private void initChannelList() {
		channelList.setCellFactory(e -> {
			TextFieldListCell<Channel> cell = new TextFieldListCell<Channel>() {

				@Override
				public void updateItem(Channel item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setText(null);
					} else {
						setText(item.getName());
					}
				}
			};
			cell.setConverter(new StringConverter<Channel>() {

				@Override
				public String toString(Channel object) {
					return object.getName();
				}

				@Override
				public Channel fromString(String string) {
					Channel channel = cell.getItem();
					channel.setName(string);
					return channel;
				}
			});
			return cell;
		});
		channelList.setOnEditCommit(e -> timeKeeperController.setChannels(channelList.getItems()));
		channelList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Channel>() {

			@Override
			public void changed(ObservableValue<? extends Channel> observable, Channel oldValue, Channel newValue) {
				controller.setActiveChannel(newValue.getChannel());
				if (newValue != null) {
					LOG.info("Switching to channel " + newValue.getName());
				}
			}
		});
		// Edit channel list
		channelList.setEditable(true);
	}

	private void initMenu() {
		root.addEventHandler(KeyEvent.ANY, e -> {
			if (e.getCode() == KeyCode.SPACE) {
				toggleFFT(new ActionEvent());
				e.consume();
			}
		});
		toggleFFT.selectedProperty().bindBidirectional(menuStartFFT.selectedProperty());
		toggleFFT.selectedProperty().addListener(e -> {
			Image image;
			if (toggleFFT.isSelected()) {
				image = new Image("./gui/res/sample_selected.png");
			} else {
				image = new Image("./gui/res/sample.png");
			}
			ImageView view = new ImageView(image);
			view.setFitWidth(25.0);
			view.setFitHeight(25.0);
			toggleFFT.setGraphic(view);
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
		menuShowCue.selectedProperty().addListener(e -> timeKeeperController.show(menuShowCue.isSelected()));
		// Close Button
		closeMenu.setOnAction(e -> {
			Main.close();
		});
	}

	public void initIO(String ioName) {
		controller = new ASIOController(ioName);
		fftController.setDriver(controller);
		channelList.getItems().setAll(controller.getInputList());
		timeKeeperController.setChannels(controller.getInputList());
		lblDriver.setText(ioName);
		lblLatency.setText(controller.getLatency() + " ms");
		if (channelList.getItems().size() > 0) {
			channelList.getSelectionModel().select(0);
		}
	}

	public void setChannelList(List<Channel> list) {
		channelList.getItems().setAll(list);
	}

	@FXML
	private void toggleFFT(ActionEvent e) {
		fftController.play(!fftController.isPlaying());
		toggleFFT.setSelected(fftController.isPlaying());
	}

	public void setSelectedChannel(Channel channel) {
		channelList.getSelectionModel().select(channel);
	}
}
