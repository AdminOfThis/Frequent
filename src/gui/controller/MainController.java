package gui.controller;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import data.Channel;
import data.FileIO;
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
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.Main;

public class MainController implements Initializable, DataHolder<Channel> {

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
		FileIO.registerSaveData(this);
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
			SplitPane.setResizableWithParent(p, false);

			timeKeeperController = (TimeKeeperController) FXMLUtil.getController();
			// contentPane.getItems().add(p);
			toggleCue.selectedProperty().addListener(e -> {

				if (!toggleCue.isSelected()) {
					contentPane.getItems().remove(p);
				} else {
					if (!contentPane.getItems().contains(p)) {
						contentPane.getItems().add(p);
						contentPane.setDividerPosition(0, 0.72);
					}
				}
			});
			// timeKeeperController.show(toggleCue.selectedProperty().get());
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

	@FXML
	private void open(ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		File result = chooser.showOpenDialog(root.getScene().getWindow());
		FileIO.open(result);
	}


	@FXML
	private void save(ActionEvent e) {
		if (FileIO.getCurrentFile() != null) {
			FileIO.save(FileIO.getCurrentFile());
		} else {
			saveAs(e);
		}
	}

	@FXML
	private void saveAs(ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save to Directory");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null) {
			if (timeKeeperController != null) {
				FileIO.save(result);
			}
		}
	}

	public TimeKeeperController getTimeKeeperController() {
		return timeKeeperController;
	}

	@Override
	public void add(Channel t) {
		if (!channelList.getItems().contains(t)) {
			channelList.getItems().add(t);
		}
	}

	@Override
	public void set(List<Channel> list) {
		channelList.getItems().setAll(list);
	}

	@FXML
	public void dragOver(DragEvent e) {
		if (e.getDragboard().hasFiles()) {
			e.acceptTransferModes(TransferMode.MOVE);
		}
	}

	@FXML
	public void dragDropped(DragEvent e) {
		if (e.getDragboard().hasFiles()) {
			for (File f : e.getDragboard().getFiles()) {
				try {
					FileIO.open(f);
					LOG.info("File dropped");
				} catch (Exception ex) {
					LOG.info("Unable to load from " + f.getAbsolutePath());
				}
			}
		}
	}

	@Override
	public List<Channel> getData() {
		return channelList.getItems();
	}

	@Override
	public void clear() {
		// TODO this is not finished, needs some merging
		channelList.getItems().clear();

	}
}
