package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import gui.utilities.controller.InputCell;
import gui.utilities.controller.WaveFormChartController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
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
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import main.Main;

public class MainController implements Initializable, Pausable {

	private static final String				FFT_PATH			= "/gui/gui/FFT.fxml";
	private static final String				TIMEKEEPER_PATH		= "/gui/gui/TimeKeeper.fxml";
	private static final String				GROUP_PATH			= "/gui/gui/Group.fxml";
	// private static final String TUNER_PATH = "/gui/gui/Tuner.fxml";
	// private static final String BACKGROUND_PATH = "/gui/gui/Background.fxml";
	private static final String				DRUM_PATH			= "/gui/gui/Drum.fxml";
	private static final Logger				LOG					= Logger.getLogger(MainController.class);
	private static final ExtensionFilter	FILTER				= new ExtensionFilter(Main.TITLE + " File", "*" + FileIO.ENDING);
	private static MainController			instance;
	@FXML
	private AnchorPane						waveFormPane;
	@FXML
	private StackPane						stack;
	/**
	 * Buttons for cues, get mapped with content to contentMap
	 */
	@FXML
	private ToggleButton					toggleFFTView, toggleDrumView, toggleGroupsView;
	@FXML
	private CheckMenuItem					menuSpectrumView, menuDrumView, menuGroupsView;
	@FXML
	private ToggleButton					toggleWaveForm, toggleCue, toggleChannels, toggleGroupChannels;
	@FXML
	private BorderPane						root, sub;
	@FXML
	private SplitPane						contentPane;
	@FXML
	private Menu							driverMenu;
	@FXML
	private MenuItem						closeMenu, menuSave;
	@FXML
	private TreeView<Input>					channelList;
	@FXML
	private CheckMenuItem					menuShowCue, menuShowChannels, menuStartFFT, menuShowTuner;
	@FXML
	private Label							lblDriver, lblLatency;
	@FXML
	private SplitPane						channelPane;
	@FXML
	private Label							lblStatus;
	@FXML
	private ProgressBar						progStatus;
	private boolean							pause				= false;
	private HashMap<ToggleButton, Node>		contentMap			= new HashMap<>();
	private double							channelSplitRatio	= 0.8;
	private ASIOController					controller;
	private FFTController					fftController;
	private TimeKeeperController			timeKeeperController;
	// private TunerController tunerController;
	// private DrumController drumController;
	private WaveFormChartController			waveFormController;

	public static MainController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		setStatus("Loading GUI", -1);
		root.setStyle(Main.getStyle());
		initWaveForm();
		initMenu();
		initChannelList();
		initFullScreen();
		// initTuner();
		initTimekeeper();
		initChart();
		initDrumMonitor();
		initGroups();
		initListener();
		resetStatus();
	}

	private void initGroups() {
		Parent p = FXMLUtil.loadFXML(GROUP_PATH);
		if (p != null) {
			contentMap.put(toggleGroupsView, p);
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initListener() {
		for (ToggleButton b : contentMap.keySet()) {
			b.setOnAction(e -> {
				if (b.getToggleGroup().getSelectedToggle() == null) {
					contentPane.getItems().remove(0);
				} else if (b.isSelected()) {
					Node n = contentMap.get(b);
					if (b.equals(toggleGroupsView)) {
						GroupController.getInstance().refresh();
					}
					if (contentPane.getItems().size() < 1) {
						contentPane.getItems().add(0, n);
					} else {
						contentPane.getItems().set(0, n);
					}
				}
				fftController.pause(!toggleFFTView.isSelected());
				GroupController.getInstance().pause(!toggleGroupsView.isSelected());
			});
		}
	}

	private void initWaveForm() {
		LOG.debug("Loading WaveForm");
		Parent p = FXMLUtil.loadFXML(WaveFormChartController.PATH);
		waveFormController = (WaveFormChartController) FXMLUtil.getController();
		waveFormController.setParentPausable(this);
		waveFormPane.getChildren().add(p);
		AnchorPane.setTopAnchor(p, .0);
		AnchorPane.setBottomAnchor(p, .0);
		AnchorPane.setLeftAnchor(p, .0);
		AnchorPane.setRightAnchor(p, .0);
	}

	private void initChart() {
		Parent p = FXMLUtil.loadFXML(FFT_PATH);
		if (p != null) {
			fftController = (FFTController) FXMLUtil.getController();
			contentMap.put(toggleFFTView, p);
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
		toggleChannels.selectedProperty().addListener(e -> pause(!toggleChannels.isSelected()));

		toggleWaveForm.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				waveFormController.pause(newValue);
				if (newValue) {
					if (!channelPane.getItems().contains(waveFormPane)) {
						channelPane.getItems().add(waveFormPane);
						channelPane.setDividerPosition(0, channelSplitRatio);
					}
				} else {
					channelSplitRatio = channelPane.getDividerPositions()[0];
					channelPane.getItems().remove(waveFormPane);
				}
			}
		});
		channelList.setCellFactory(e -> new InputCell());
		channelList.setRoot(new TreeItem<>());
		channelList.setShowRoot(false);
		// channelList.setOnEditCommit(e ->
		// timeKeeperController.setChannels(channelList.getItems()));
		channelList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Input>>() {

			@Override
			public void changed(ObservableValue<? extends TreeItem<Input>> observable, TreeItem<Input> oldValue, TreeItem<Input> newValue) {
				if (newValue != null && newValue.getValue() != null) {
					if (newValue.getValue() instanceof Channel) {
						Channel channel = (Channel) newValue.getValue();
						controller.setActiveChannel(channel.getChannel());
						fftController.setChannel(channel);
						LOG.info("Switching to channel " + channel.getName());
					}
					waveFormController.setChannel(newValue.getValue());
				}
			}
		});
		// Edit channel list
		channelList.setEditable(true);
		//
		toggleGroupChannels.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue == oldValue) {
					return;
				}
				refreshInputs();
			}
		});
	}

	private void refreshInputs() {
		TreeItem<Input> root = new TreeItem<>();
		channelList.setRoot(root);
		if (ASIOController.getInstance() != null) {
			if (toggleGroupChannels.isSelected()) {
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
							groupItem.setExpanded(true);
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
		root.setExpanded(true);
	}

	private void initMenu() {


		menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

		menuSpectrumView.selectedProperty().bindBidirectional(toggleFFTView.selectedProperty());
		menuGroupsView.selectedProperty().bindBidirectional(toggleGroupsView.selectedProperty());
		menuDrumView.selectedProperty().bindBidirectional(toggleDrumView.selectedProperty());

		toggleCue.selectedProperty().bindBidirectional(menuShowCue.selectedProperty());
		toggleChannels.selectedProperty().bindBidirectional(menuShowChannels.selectedProperty());
		// toggleTuner.selectedProperty().bindBidirectional(menuShowTuner.selectedProperty());
		menuShowCue.selectedProperty().addListener(e -> timeKeeperController.show(menuShowCue.isSelected()));
		// Close Button
		closeMenu.setOnAction(e -> {
			Main.close();
		});
	}

	// private void initTuner() {
	// Parent p = FXMLUtil.loadFXML(TUNER_PATH);
	// tunerController = (TunerController) FXMLUtil.getController();
	// sub.setBottom(p);
	// toggleTuner.selectedProperty().addListener(e -> {
	// tunerController.show(toggleTuner.isSelected());
	// });
	// tunerController.show(false);
	// }
	public void initIO(String ioName) {
		controller = new ASIOController(ioName);
		controller.addFFTListener(fftController);
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
				if (findAndSelect(i, channel)) {
					return true;
				}
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
		setStatus("Saving cues", -1);
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
		resetStatus();
	}

	@FXML
	private void saveChannels(ActionEvent e) {
		setStatus("Saving channels", -1);
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
		resetStatus();
	}

	@FXML
	private void save(ActionEvent e) {
		setStatus("Saving", -1);
		if (FileIO.getCurrentFile() != null) {
			FileIO.save(FileIO.getCurrentFile());
		} else {
			saveAs(e);
		}
		resetStatus();
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
			setStatus("Loading file", -1);
			for (File f : e.getDragboard().getFiles()) {
				FileIO.open(f);
			}
			resetStatus();
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
			refreshInputs();
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

	private void initDrumMonitor() {
		Parent p = FXMLUtil.loadFXML(DRUM_PATH);
		if (p != null) {
			contentMap.put(toggleDrumView, p);
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	@Override
	public void pause(boolean pause) {
		if (this.pause != pause) {
			this.pause = pause;
			if (pause) {
				LOG.debug(getClass().getSimpleName() + "; pausing animations");
			} else {
				LOG.debug(getClass().getSimpleName() + "; playing animations");
			}
		}
	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	@Override
	public void setParentPausable(Pausable parent) {
		LOG.error("Uninplemented method called: addParentPausable");
	}

	public void setStatus(String text) {
		lblStatus.setText(text);
	}

	public void setStatus(double value) {
		progStatus.setProgress(value);
		if (progStatus.getProgress() == 0) {
			progStatus.setVisible(false);
		} else {
			progStatus.setVisible(true);
		}
	}

	public void setStatus(String text, double value) {
		setStatus(text);
		setStatus(value);
	}

	public void resetStatus() {
		setStatus("", 0);
	}
}
