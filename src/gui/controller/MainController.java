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
import gui.pausable.Pausable;
import gui.utilities.FXMLUtil;
import gui.utilities.controller.InputCell;
import gui.utilities.controller.WaveFormChartController;
import javafx.application.Platform;
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
import javafx.scene.control.SelectionMode;
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

	private static final String				FFT_PATH			= "/gui/gui/RTAView.fxml";
	private static final String				RTA_PATH			= "/gui/gui/FFTView.fxml";
	private static final String				TIMEKEEPER_PATH		= "/gui/gui/TimeKeeper.fxml";
	private static final String				GROUP_PATH			= "/gui/gui/GroupView.fxml";
	// private static final String BACKGROUND_PATH = "/gui/gui/Background.fxml";
	private static final String				DRUM_PATH			= "/gui/gui/DrumView.fxml";
	private static final String				PHASE_PATH			= "/gui/gui/VectorScopeView.fxml";
	private static final Logger				LOG					= Logger.getLogger(MainController.class);
	private static final ExtensionFilter	FILTER				= new ExtensionFilter(Main.getOnlyTitle() + " File", "*" + FileIO.ENDING);
	private static MainController			instance;
	@FXML
	private AnchorPane						waveFormPane;
	@FXML
	private StackPane						stack;
	/**
	 * Buttons for cues, get mapped with content to contentMap
	 */
	@FXML
	private ToggleButton					toggleFFTView, toggleRTAView, toggleDrumView, toggleGroupsView, togglePhaseView;
	@FXML
	private CheckMenuItem					menuSpectrumView, menuRTAView, menuDrumView, menuGroupsView, menuPhaseView;
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
	private MenuItem						menuTimerStart, menuTimerNext;
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
	private boolean							showHidden			= false;
	private boolean							pause				= false;
	private HashMap<ToggleButton, Node>		contentMap			= new HashMap<>();
	private double							channelSplitRatio	= 0.8;
	private ASIOController					controller;
	private RTAViewController				fftController;
	private FFTViewController				rtaController;
	private TimeKeeperController			timeKeeperController;
	// private DrumController drumController;
	private WaveFormChartController			waveFormController;

	public static MainController getInstance() {
		return instance;
	}

	public void setTitle(String title) {
		Stage stage = (Stage) channelList.getScene().getWindow();
		String finalTitle = Main.getTitle();
		if (title != null && !title.isEmpty()) {
			finalTitle += " - " + title;
		}
		stage.setTitle(finalTitle);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		setStatus("Loading GUI", -1);
		root.setStyle(Main.getStyle());
		initWaveForm();
		initTimekeeper();
		initMenu();
		initChannelList();
		initFullScreen();
		initChart();
		initRTA();
		initDrumMonitor();
		initPhaseMonitor();
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
		for (ToggleButton toggleButton : contentMap.keySet()) {
			toggleButton.setOnAction(e -> {
				if (!toggleButton.isSelected()) {
					toggleButton.setSelected(true);
					e.consume();
					return;
				}
				if (toggleButton.getToggleGroup().getSelectedToggle() == null && contentPane.getItems().size() > 0) {
					contentPane.getItems().remove(0);
				} else if (toggleButton.isSelected()) {
					Node n = contentMap.get(toggleButton);
					if (toggleButton.equals(toggleGroupsView)) {
						GroupViewController.getInstance().refresh();
					}
					if (contentPane.getItems().size() < 1) {
						contentPane.getItems().add(0, n);
					} else {
						contentPane.getItems().set(0, n);
					}
				}
				fftController.pause(!toggleFFTView.isSelected());
				rtaController.pause(!toggleRTAView.isSelected());
				GroupViewController.getInstance().pause(!toggleGroupsView.isSelected());
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
			fftController = (RTAViewController) FXMLUtil.getController();
			contentMap.put(toggleFFTView, p);
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initRTA() {
		Parent p = FXMLUtil.loadFXML(RTA_PATH);
		rtaController = (FFTViewController) FXMLUtil.getController();
		if (p != null) {
			contentMap.put(toggleRTAView, p);
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
		channelList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
		toggleGroupChannels.selectedProperty().addListener((obs, oldV, newV) -> {
			if (oldV != newV) {
				refreshInputs();
			}
		});
	}

	private void refreshInputs() {
		TreeItem<Input> root = new TreeItem<>();
		channelList.setRoot(root);
		if (ASIOController.getInstance() != null) {
			if (toggleGroupChannels.isSelected()) {
				for (Group g : ASIOController.getInstance().getGroupList()) {
					root.getChildren().add(new TreeItem<Input>(g));
				}
			} else {
				for (Channel channel : ASIOController.getInstance().getInputList()) {
					if (!channel.isHidden() || showHidden) {
						root.getChildren().add(new TreeItem<>(channel));
					}
				}
			}
		}
		root.setExpanded(true);
	}

	private void initMenu() {
		menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		bindCheckMenuToToggleButton(menuSpectrumView, toggleFFTView);
		bindCheckMenuToToggleButton(menuRTAView, toggleRTAView);
		bindCheckMenuToToggleButton(menuGroupsView, toggleGroupsView);
		bindCheckMenuToToggleButton(menuDrumView, toggleDrumView);
		bindCheckMenuToToggleButton(menuPhaseView, togglePhaseView);
		menuTimerStart.setOnAction(e -> TimeKeeperController.getInstance().toggleTimer());
		menuTimerNext.setOnAction(e -> TimeKeeperController.getInstance().round());
		menuTimerStart.disableProperty().bind(TimeKeeperController.getInstance().getStartButton().disabledProperty());
		menuTimerNext.disableProperty().bind(TimeKeeperController.getInstance().getRoundButton().disabledProperty());
		toggleCue.selectedProperty().bindBidirectional(menuShowCue.selectedProperty());
		toggleChannels.selectedProperty().bindBidirectional(menuShowChannels.selectedProperty());
		// toggleTuner.selectedProperty().bindBidirectional(menuShowTuner.selectedProperty());
		menuShowCue.selectedProperty().addListener(e -> timeKeeperController.show(menuShowCue.isSelected()));
		// Close Button
		closeMenu.setOnAction(e -> Main.close());
	}

	private void bindCheckMenuToToggleButton(CheckMenuItem menu, ToggleButton button) {
		menu.setOnAction(e -> button.fire());
		button.selectedProperty().addListener((obs, oldVal, newVal) -> {
			menu.setSelected(newVal);
		});
	}

	public void initIO(String ioName) {
		controller = new ASIOController(ioName);
		controller.addFFTListener(fftController);
		controller.addFFTListener(rtaController);
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
		fftController.refresh();
		GroupViewController.getInstance().refresh();
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

	private void initPhaseMonitor() {
		Parent p = FXMLUtil.loadFXML(PHASE_PATH);
		if (p != null) {
			contentMap.put(togglePhaseView, p);
		} else {
			LOG.warn("Unable to load VectorScope");
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

	public void setStatus(String text) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				lblStatus.setText(text);
				progStatus.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
				progStatus.setVisible(true);
			}
		});
	}

	public void setStatus(double value) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progStatus.setProgress(value);
				if (progStatus.getProgress() == 0) {
					progStatus.setVisible(false);
				} else {
					progStatus.setVisible(true);
				}
			}
		});
	}

	public void setStatus(String text, double value) {
		setStatus(text);
		setStatus(value);
	}

	public void resetStatus() {
		setStatus("", 0);
	}

	public boolean isShowHidden() {
		return showHidden;
	}

	public void setShowHidden(boolean showHidden) {
		this.showHidden = showHidden;
	}

	/**
	 * toggles hide for all selected items
	 */
	public void hideAllSelected() {
		try {
			TreeItem<Input> first = channelList.getSelectionModel().getSelectedItems().get(0);
			if (first != null) {
				if (first.getValue() instanceof Channel) {
					Channel channel = (Channel) first.getValue();
					boolean hide = !channel.isHidden();
					for (TreeItem<Input> item : channelList.getSelectionModel().getSelectedItems()) {
						if (item.getValue() instanceof Channel) {
							((Channel) item.getValue()).setHidden(hide);
						}
					}
				}
			}
			refresh();
		}
		catch (Exception e) {
			LOG.warn("Error while hiding items");
			LOG.debug("", e);
		}
	}

	public void groupAllSelected(Group g) {
		try {
			for (TreeItem<Input> item : channelList.getSelectionModel().getSelectedItems()) {
				if (item.getValue() instanceof Channel) {
					g.addChannel((Channel) item.getValue());
				}
			}
			refresh();
		}
		catch (Exception e) {
			LOG.warn("Error while grouping items");
			LOG.debug("", e);
		}
	}
}
