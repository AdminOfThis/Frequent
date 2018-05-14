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
import data.Input;
import gui.utilities.FXMLUtil;
import gui.utilities.controller.ChannelCell;
import gui.utilities.controller.WaveFormChartController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import main.Main;

public class MainController implements Initializable {

	private static final String				FFT_PATH		= "/gui/gui/FFT.fxml";
	private static final String				TIMEKEEPER_PATH	= "/gui/gui/TimeKeeper.fxml";
	private static final String				TUNER_PATH		= "/gui/gui/Tuner.fxml";
	private static final String				BACKGROUND_PATH	= "/gui/gui/Background.fxml";
	private static final String				DRUM_PATH		= "/gui/gui/Drum.fxml";
	private static final Logger				LOG				= Logger.getLogger(MainController.class);
	private static final ExtensionFilter	FILTER			= new ExtensionFilter(Main.TITLE + " File", "*" + FileIO.ENDING);
	private static MainController			instance;
	@FXML
	private AnchorPane						waveFormPane;
	@FXML
	private StackPane						stack;
	@FXML
	private ToggleButton					toggleFFT, toggleCue, toggleTuner, toggleChannels, toggleGroupChannels;
	@FXML
	private BorderPane						root, sub;
	@FXML
	private SplitPane						contentPane;
	@FXML
	private Menu							driverMenu;
	@FXML
	private MenuItem						closeMenu, menuSave;
	@FXML
	private Menu							groupMenu;
	@FXML
	private TreeView<Input>					channelList;
	@FXML
	private CheckMenuItem					menuShowCue, menuStartFFT, menuShowTuner;
	@FXML
	private Label							lblDriver, lblLatency;
	@FXML
	private ContextMenu						contextMenu;
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
	private DrumController					drumController;
	private WaveFormChartController			waveFormController;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		root.setStyle(Main.getStyle());
		initWaveForm();
		initMenu();
		initContextMenu();
		initChannelList();
		initFullScreen();
		initTuner();
		initTimekeeper();
		initChart();
		initStackPane();
	}

	private void initContextMenu() {
		// adding colorPicker
		ColorPicker picker = new ColorPicker();
		picker.valueProperty().addListener(new ChangeListener<Color>() {

			@Override
			public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
				TreeItem<Input> in = channelList.getSelectionModel().getSelectedItem();
				if (in != null && in.getValue() != null && newValue != null) {
					in.getValue().setColor(toRGBCode(newValue));
					channelList.refresh();
				}
			}
		});
		MenuItem colorPicker = new MenuItem(null, picker);
		contextMenu.getItems().add(0, colorPicker);
		// on opening
		contextMenu.setOnShowing(e -> {
			try {
				Input item = channelList.getSelectionModel().getSelectedItem().getValue();
				if (item == null) {
					contextMenu.hide();
				} else {
					if (item instanceof Channel) {
						for (MenuItem g : groupMenu.getItems()) {
							if (g instanceof RadioMenuItem) {
								String text = ((Label) g.getGraphic()).getText();
								Group group = ((Channel) item).getGroup();
								if (group != null && text.equals(group.getName())) {
									((RadioMenuItem) g).setSelected(true);
									break;
								} else {
									((RadioMenuItem) g).setSelected(false);
								}
							}
						}
					}
				}
			}
			catch (Exception ex) {
				LOG.warn("Problems on opening context menu", ex);
			}
		});
	}

	private void initWaveForm() {
		LOG.info("Loading WaveForm");
		Parent p = FXMLUtil.loadFXML(WaveFormChartController.PATH);
		waveFormController = (WaveFormChartController) FXMLUtil.getController();
		waveFormPane.getChildren().add(p);
		AnchorPane.setTopAnchor(p, .0);
		AnchorPane.setBottomAnchor(p, .0);
		AnchorPane.setLeftAnchor(p, .0);
		AnchorPane.setRightAnchor(p, .0);
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
		toggleChannels.selectedProperty().bindBidirectional(root.getLeft().visibleProperty());
		toggleChannels.selectedProperty().bindBidirectional(root.getLeft().managedProperty());
		channelList.setCellFactory(e -> new ChannelCell());
		channelList.setRoot(new TreeItem<>());
		channelList.setShowRoot(false);
		// channelList.setOnEditCommit(e ->
		// timeKeeperController.setChannels(channelList.getItems()));
		channelList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Input>>() {

			@Override
			public void changed(ObservableValue<? extends TreeItem<Input>> observable, TreeItem<Input> oldValue, TreeItem<Input> newValue) {
				if (newValue != null && newValue.getValue() != null && newValue.getValue() instanceof Channel) {
					Channel channel = (Channel) newValue.getValue();
					controller.setActiveChannel(channel.getChannel());
					fftController.setChannel(channel);
					waveFormController.setChannel(channel);
					LOG.info("Switching to channel " + channel.getName());
				}
				enableContextMenu(newValue != null);
			}
		});
		// Edit channel list
		channelList.setEditable(true);
		//
		toggleGroupChannels.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue == oldValue) { return; }
				TreeItem<Input> root = new TreeItem<>();
				channelList.setRoot(root);
				if (newValue) {
					for (Channel c : ASIOController.getInstance().getInputList()) {
						if (c.getGroup() == null) {
							root.getChildren().add(new TreeItem<>(c));
						} else {
							TreeItem<Input> groupItem = null;
							for (TreeItem<Input> g : root.getChildren()) {
								if (g.getValue() instanceof Group && g.getValue().equals(c.getGroup())) {
									groupItem = g;
									break;
								}
							}
							if (groupItem == null) {
								groupItem = new TreeItem<>(c.getGroup());
								root.getChildren().add(groupItem);
							}
							groupItem.getChildren().add(new TreeItem<>(c));
						}
					}
				} else {
					for (Channel channel : ASIOController.getInstance().getInputList()) {
						root.getChildren().add(new TreeItem<>(channel));
					}
				}
			}
		});
	}

	private void initMenu() {
		menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
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
		timeKeeperController.setChannels(controller.getInputList());
		setChannelList(controller.getInputList());
		lblDriver.setText(ioName);
		lblLatency.setText(controller.getLatency() + "ms ");
		if (channelList.getRoot().getChildren().size() > 0) {
			channelList.getSelectionModel().select(0);
		}
	}

	public void setChannelList(List<Channel> list) {
		channelList.getRoot().getChildren().clear();
		for (Channel c : list) {
			channelList.getRoot().getChildren().add(new TreeItem<Input>(c));
		}
	}

	@FXML
	private void toggleFFT(ActionEvent e) {
		fftController.play(!fftController.isPlaying());
		toggleFFT.setSelected(fftController.isPlaying());
	}

	public void setSelectedChannel(Channel channel) {
		findAndSelect(channelList.getRoot(), channel);
	}

	/**
	 * rekursive slection in treeview
	 * 
	 * @param item,
	 *            leaf which children get searched
	 * @param channel,
	 *            the channel to find
	 * @return result, true if found to bubble upwards
	 */
	private boolean findAndSelect(TreeItem<Input> item, Channel channel) {
		for (TreeItem<Input> i : item.getChildren()) {
			if (i != null && i.getValue().equals(channel)) {
				channelList.getSelectionModel().select(i);
				return true;
			}
			if (!i.isLeaf()) {
				if (findAndSelect(i, channel)) { return true; }
			}
		}
		return false;
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
			channelList.getRoot().getChildren().clear();
			for (Channel c : controller.getInputList()) {
				channelList.getRoot().getChildren().add(new TreeItem<Input>(c));
			}
			ToggleGroup group = new ToggleGroup();
			for (Group g : controller.getGroupList()) {
				RadioMenuItem groupItem = new RadioMenuItem(null, new Label(g.getName()));
				groupItem.setToggleGroup(group);
				if (g.getColor() != null) {
					Circle circle = new Circle();
					circle.setStroke(null);
					circle.setFill(Color.valueOf(g.getColor()));
					groupItem.setGraphic(circle);
				}
				groupItem.setOnAction(e -> {
					if (channelList.getSelectionModel().getSelectedItem().getValue() instanceof Channel) {
						Channel c = (Channel) channelList.getSelectionModel().getSelectedItem().getValue();
						c.setGroup(g);
					}
				});
				groupMenu.getItems().add(groupItem);
			}
		}
		timeKeeperController.refresh();
	}

	@FXML
	private void resetName(ActionEvent e) {
		Input channel = channelList.getSelectionModel().getSelectedItem().getValue();
		if (channel != null && channel instanceof Channel) {
			((Channel) channel).resetName();
			refresh();
		}
	}

	private void enableContextMenu(boolean value) {
		cxtResetName.setDisable(!value);
		cxtUngroup.setDisable(!value);
	}

	@FXML
	private void openDrumMonitor(ActionEvent e) {
		if (drumController == null) {
			Parent p = FXMLUtil.loadFXML(DRUM_PATH);
			drumController = (DrumController) FXMLUtil.getController();
			Stage secondStage = new Stage();
			secondStage.setOnCloseRequest(ev -> {
				LOG.info("Closing DrumStage");
				secondStage.hide();
			});
			secondStage.setScene(new Scene(p));
			secondStage.centerOnScreen();
			secondStage.setWidth(1280);
			secondStage.setHeight(960);
			secondStage.show();
		} else {
			drumController.show();
		}
	}

	protected void setDrumController(DrumController con) {
		this.drumController = con;
	}

	public static String toRGBCode(Color color) {
		return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
	}
}
