package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.TimeKeeper;
import data.Channel;
import data.FileIO;
import data.Group;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.Main;

public class MainController implements Initializable {

	private static final String				FFT_PATH		= "./../gui/FFT.fxml";
	private static final String				TIMEKEEPER_PATH	= "./../gui/TimeKeeper.fxml";
	private static final String				TUNER_PATH		= "./../gui/Tuner.fxml";
	private static final String				BACKGROUND_PATH	= "./../gui/Background.fxml";
	private static final Logger				LOG				= Logger.getLogger(MainController.class);
	private static final ExtensionFilter	FILTER			= new ExtensionFilter(Main.TITLE + " File", "*" + FileIO.ENDING);
	private static MainController			instance;
	@FXML
	private StackPane						stack;
	@FXML
	private ToggleButton					toggleFFT, toggleCue, toggleTuner;
	@FXML
	private BorderPane						root, sub;
	@FXML
	private SplitPane						contentPane;
	@FXML
	private Menu							driverMenu;
	@FXML
	private MenuItem						closeMenu;
	@FXML
	private Menu							groupMenu;
	@FXML
	private ListView<Channel>				channelList;
	@FXML
	private CheckMenuItem					menuShowCue, menuStartFFT, menuShowTuner;
	@FXML
	private Label							lblDriver, lblLatency;
	/**************
	 * contextmenu
	 **************/
	@FXML
	private MenuItem						cxtResetName, cxtUngroup;
	private ASIOController					controller;
	private FFTController					fftController;
	private TimeKeeperController			timeKeeperController;
	private TunerController					tunerController;
	private BackgroundController			backgroundController;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		initMenu();
		initChannelList();
		initFullScreen();
		initTuner();
		initTimekeeper();
		initChart();
		initStackPane();
	}

	private void initStackPane() {
		if (Main.isFUI()) {
			Parent p = FXMLUtil.loadFXML(BACKGROUND_PATH);
			if (p != null) {
				backgroundController = (BackgroundController) FXMLUtil.getController();
				stack.getChildren().add(0, p);
			} else {
				LOG.warn("Unable to load Background");
			}
		}
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
			root.setRight(p);
			timeKeeperController.show(false);
			toggleCue.selectedProperty().addListener(e -> {
				timeKeeperController.show(toggleCue.isSelected());
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
				if (newValue != null) {
					controller.setActiveChannel(newValue.getChannel());
					LOG.info("Switching to channel " + newValue.getName());
				}
				enableContextMenu(newValue != null);
			}
		});
		// Edit channel list
		channelList.setEditable(true);
	}

	private void initMenu() {
		toggleFFT.selectedProperty().bindBidirectional(menuStartFFT.selectedProperty());
		toggleCue.selectedProperty().bindBidirectional(menuShowCue.selectedProperty());
		toggleTuner.selectedProperty().bindBidirectional(menuShowTuner.selectedProperty());
		menuShowCue.selectedProperty().addListener(e -> timeKeeperController.show(menuShowCue.isSelected()));
		// Close Button
		closeMenu.setOnAction(e -> {
			Main.close();
		});
		enableContextMenu(false);
	}

	private void initTuner() {
		Parent p = FXMLUtil.loadFXML(TUNER_PATH);
		tunerController = (TunerController) FXMLUtil.getController();
		sub.setBottom(p);
		toggleTuner.selectedProperty().addListener(e -> {
			tunerController.show(toggleTuner.isSelected());
		});
		tunerController.show(false);
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
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showOpenDialog(root.getScene().getWindow());
		FileIO.open(result);
	}

	@FXML
	private void saveCues(ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save cues");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null) {
			if (timeKeeperController != null) {
				FileIO.save(new ArrayList<Serializable>(TimeKeeper.getInstance().getData()), result);
			}
		}
	}

	@FXML
	private void saveChannels(ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save channels");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null) {
			if (timeKeeperController != null) {
				FileIO.save(new ArrayList<>(ASIOController.getInstance().getData()), result);
			}
		}
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
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
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

	@FXML
	public void dragOver(DragEvent e) {
		if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().get(0).getName().endsWith(FileIO.ENDING)) {
			e.acceptTransferModes(TransferMode.MOVE);
		}
	}

	@FXML
	public void dragDropped(DragEvent e) {
		if (e.getDragboard().hasFiles()) {
			for (File f : e.getDragboard().getFiles()) {
				FileIO.open(f);
			}
		}
	}

	@FXML
	private void newGroup(ActionEvent e) {
		TextInputDialog dialog = new TextInputDialog("");
		dialog.initStyle(((Stage) root.getScene().getWindow()).getStyle());
		dialog.setTitle("New Group");
		dialog.setHeaderText("Choose a name for the new Group");
		dialog.setContentText("Please enter the name:");
		// Traditional way to get the response value.
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			if (ASIOController.getInstance() != null) {
				ASIOController.getInstance().addGroup(new Group(result.get()));
			}
			refresh();
		}
	}

	public void refresh() {
		if (controller != null) {
			channelList.getItems().setAll(controller.getInputList());
			for (Group g : controller.getGroupList()) {
				MenuItem groupItem = new MenuItem(g.getName());
				if (g.getColor() != null) {
					Circle circle = new Circle();
					circle.setStroke(null);
					circle.setFill(Color.valueOf(g.getColor()));
					groupItem.setGraphic(circle);
				}
				groupItem.setOnAction(e -> channelList.getSelectionModel().getSelectedItem().setGroup(g));
				groupMenu.getItems().add(groupItem);
			}
		}
		timeKeeperController.refresh();
	}

	@FXML
	private void resetName(ActionEvent e) {
		Channel channel = channelList.getSelectionModel().getSelectedItem();
		if (channel != null) {
			channel.resetName();
			refresh();
		}
	}

	private void enableContextMenu(boolean value) {
		cxtResetName.setDisable(!value);
		cxtUngroup.setDisable(!value);
	}
}
