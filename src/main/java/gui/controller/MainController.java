package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import control.CueListener;
import control.FFTListener;
import control.TimeKeeper;
import control.bpmdetect.BeatDetector;
import data.Channel;
import data.Cue;
import data.FileIO;
import data.Group;
import data.Input;
import dialog.InformationDialog;
import gui.FXMLUtil;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import gui.pausable.PausableView;
import gui.utilities.controller.ChannelCell;
import gui.utilities.controller.DataChart;
import gui.utilities.controller.SymmetricWaveFormChart;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.Constants;
import main.Constants.RESTORE_PANEL;
import main.Main;
import preferences.PropertiesIO;

public class MainController implements Initializable, Pausable, CueListener {

	private static final String SETTINGS_PATH = "/fxml/Settings.fxml";
	// modules
	private static final String FFT_PATH = "/fxml/RTAView.fxml";
	private static final String RTA_PATH = "/fxml/FFTView.fxml";
	private static final String TIMEKEEPER_PATH = "/fxml/TimeKeeper.fxml";
	private static final String GROUP_PATH = "/fxml/GroupView.fxml";
	private static final String DRUM_PATH = "/fxml/DrumView.fxml";
	private static final String PHASE_PATH = "/fxml/VectorScopeView.fxml";
	private static final String BLEED_PATH = "/fxml/BleedView.fxml";
	private static final Logger LOG = LogManager.getLogger(MainController.class);
	private static final ExtensionFilter FILTER = new ExtensionFilter(Main.getOnlyTitle() + " File", "*" + FileIO.ENDING);
	private static MainController instance;
	@FXML
	private AnchorPane waveFormPane;
	@FXML
	private HBox buttonBox;
	@FXML
	private Node bottomLabel;

	@FXML
	private VBox waveFormPaneParent, vChannelLeft;
	/**
	 * Buttons for cues, get mapped with content to contentMap
	 */
	@FXML
	private ToggleButton toggleFFTView, toggleRTAView, toggleDrumView, toggleGroupsView, togglePhaseView, toggleBleedView;

	@FXML
	private RadioMenuItem menuSpectrumView, menuRTAView, menuDrumView, menuGroupsView, menuPhaseView, menuBleedView;
	@FXML
	private ToggleButton togglePreview, toggleCue, toggleChannels, toggleGroupChannels, toggleBtmRaw, toggleBtmWave;
	@FXML
	private BorderPane root;
	@FXML
	private SplitPane contentPane, rootSplit;
	@FXML
	private MenuItem closeMenu, menuSave, menuSettings;
	@FXML
	private MenuItem menuTimerStart, menuTimerNext;
	@FXML
	private ListView<Input> channelList;
	@FXML
	private CheckMenuItem menuShowCue, menuShowChannels, menuShowHiddenChannels;
	@FXML
	private Label lblDriver, lblLatency, lblCurrentSong, lblNextSong;
	@FXML
	private SplitPane channelPane;
	@FXML
	private Label lblStatus;
	@FXML
	private ProgressBar progStatus;
	private boolean showHidden = false;
	private boolean pause = false;
	private HashMap<ToggleButton, Node> contentMap = new HashMap<>();
	private HashMap<Node, PausableView> controllerMap = new HashMap<>();
	private double channelSplitRatio = 0.8, rootSplitRatio = 0.8;
	private ASIOController controller;
	private TimeKeeperController timeKeeperController;
	// private DrumController drumController;
	private SymmetricWaveFormChart waveFormChart;
	private DataChart dataChart;
	private double minHeaderButtonWidth = 0;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		instance = this;
		setStatus("Loading GUI", -1);
		root.setStyle(Main.getStyle());
		Main.getInstance().setProgress(0.3);
		initWaveForm();
		Main.getInstance().setProgress(0.35);
		initTimekeeper();
		initMenu();
		initChannelList();
		Main.getInstance().setProgress(0.4);
		initFullScreen();
		initChart();
		Main.getInstance().setProgress(0.45);
		initRTA();
		Main.getInstance().setProgress(0.5);
		initDrumMonitor();
		Main.getInstance().setProgress(0.55);
		initPhaseMonitor();
		Main.getInstance().setProgress(0.6);
		initBleedView();
		Main.getInstance().setProgress(0.8);
		initGroups();
		Main.getInstance().setProgress(0.85);
		initListener();
		Main.getInstance().setProgress(0.9);
		applyLoadedProperties();
		Main.getInstance().setProgress(0.95);
		hideAllDebugModules();
		FileIO.registerDatahandlers();
		resetStatus();
		bottomLabel.setVisible(false);
		TimeKeeper.getInstance().addListener(this);
	}

	private void hideAllDebugModules() {
		if (!Main.isDebug()) {
			toggleDrumView.setVisible(false);
			toggleDrumView.setManaged(false);
			menuDrumView.setVisible(false);
		}

	}

	public static MainController getInstance() {
		return instance;
	}

	private void bindCheckMenuToToggleButton(final RadioMenuItem menu, final ToggleButton button) {
		menu.setOnAction(e -> button.fire());
		button.selectedProperty().addListener((obs, oldVal, newVal) -> {
			menu.setSelected(newVal);
		});
	}

	@Override
	public void currentCue(final Cue cue, final Cue next) {
		Platform.runLater(() -> setSongs(cue, next));
	}

	@FXML
	public void dragDropped(final DragEvent e) {
		if (e.getDragboard().hasFiles()) {
			setStatus("Loading file", -1);
			for (File f : e.getDragboard().getFiles()) {
				FileIO.open(f);
			}
			resetStatus();
		}
	}

	@FXML
	public void dragOver(final DragEvent e) {
		if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().get(0).getName().endsWith(FileIO.ENDING)) {
			e.acceptTransferModes(TransferMode.MOVE);
		}
	}

	public ArrayList<Input> getSelectedChannels() {
		return new ArrayList<>(channelList.getSelectionModel().getSelectedItems());
	}

	public TimeKeeperController getTimeKeeperController() {
		return timeKeeperController;
	}

	private void initChannelList() {

		menuShowHiddenChannels.selectedProperty().addListener((e, oldV, newV) -> {
			setShowHidden(newV);
			refreshInputs();
		});

		toggleChannels.setSelected(true);
		toggleChannels.selectedProperty().addListener((e, oldV, newV) -> {
			if (newV.booleanValue()) {
				if (!vChannelLeft.equals(rootSplit.getItems().get(0))) {
					rootSplit.getItems().add(0, vChannelLeft);
					rootSplit.setDividerPosition(0, rootSplitRatio);
				}
			} else if (rootSplit.getItems().contains(vChannelLeft)) {
				rootSplitRatio = rootSplit.getDividerPositions()[0];
				rootSplit.getItems().remove(0);
			}

		});

		// pauses the animations for all the vuMeters in the channels section
		toggleChannels.selectedProperty().addListener(e -> pause(!toggleChannels.isSelected()));
		togglePreview.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			if (newValue) {
				if (!channelPane.getItems().contains(waveFormPaneParent)) {
					channelPane.getItems().add(waveFormPaneParent);
					channelPane.setDividerPosition(0, channelSplitRatio);
				}
			} else {
				channelSplitRatio = channelPane.getDividerPositions()[0];
				channelPane.getItems().remove(waveFormPaneParent);
			}
		});
		channelList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		channelList.setCellFactory(e -> new ChannelCell(this));
		// channelList.setOnEditCommit(e ->
		// timeKeeperController.setChannels(channelList.getItems()));
		channelList.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Input>) (observable, oldValue, newValue) -> {
			waveFormChart.setChannel(newValue);
			if (newValue instanceof Channel) {
				dataChart.setChannel((Channel) newValue);
			}
			if (newValue != null) {
				LOG.debug("Switching to channel " + newValue.getName());
				for (PausableView v : controllerMap.values()) {
					v.setSelectedChannel(newValue);
				}
				if (newValue instanceof Channel) {
					Channel channel = (Channel) newValue;
					controller.setActiveChannel(channel);
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

	private void initChart() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(FFT_PATH));
		if (p != null) {
			contentMap.put(toggleFFTView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initDrumMonitor() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(DRUM_PATH));
		if (p != null) {
			contentMap.put(toggleDrumView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
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

	private void initGroups() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(GROUP_PATH));
		if (p != null) {
			contentMap.put(toggleGroupsView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	public void initIO(final String ioName) {
		controller = new ASIOController(ioName);
		controller.addFFTListener((FFTListener) controllerMap.get(contentMap.get(toggleFFTView)));
		controller.addFFTListener((FFTListener) controllerMap.get(contentMap.get(toggleRTAView)));
		timeKeeperController.setChannels(controller.getInputList());
		setChannelList(controller.getInputList());
		resetInfosFromDevice();
		if (!BeatDetector.isInitialized()) {
			BeatDetector.initialize();
		}
	}

	public void resetInfosFromDevice() {
		lblDriver.setText(ASIOController.getInstance().getDevice());
		lblLatency.setText(controller.getLatency() + "ms ");
		if (channelList.getItems().size() > 0) {
			channelList.getSelectionModel().select(0);
		}
	}

	private void initListener() {
		for (ToggleButton toggleButton : contentMap.keySet()) {
			toggleButton.setOnAction(e -> {
				if (!toggleButton.isSelected()) {
					// unable to unselect the current view, and get the view to
					// blank
					toggleButton.setSelected(true);
					e.consume();
					return;
				}
				if (toggleButton.getToggleGroup().getSelectedToggle() == null && contentPane.getItems().size() > 0) {
					contentPane.getItems().remove(0);
				} else if (toggleButton.isSelected()) {
					// on selection
					Node n = contentMap.get(toggleButton);

					// Saving which view is selected
					PropertiesIO.setProperty(Constants.SETTING_RESTORE_PANEL_LAST, toggleButton.getText());
					if (contentPane.getItems().size() < 1) {
						contentPane.getItems().add(0, n);
					} else {
						contentPane.getItems().set(0, n);
					}
					PausableView v = controllerMap.get(n);
					buttonBox.getChildren().clear();
					if (v.getHeader() != null) {
						buttonBox.getChildren().addAll(v.getHeader());
						for (Node headerButton : buttonBox.getChildren()) {
							setHeaderButtonWidth(headerButton);
						}
					}
				}
				for (Entry<Node, PausableView> v : controllerMap.entrySet()) {
					// Pausing if not shown
					if (contentPane.getItems().contains(v.getKey())) {
						// shown
						v.getValue().refresh();
						v.getValue().pause(false);
					} else {
						v.getValue().pause(true);
					}
				}
			});
		}
	}

	private double setHeaderButtonWidth(Node headerButton) {
		if (headerButton instanceof Region) {
			if (headerButton instanceof HBox || headerButton instanceof VBox) {
				Parent parent = (Parent) headerButton;
				double sum = 0;
				for (Node child : parent.getChildrenUnmodifiable()) {
					double childWidth = setHeaderButtonWidth(child);
					sum += childWidth;
				}

				((Region) headerButton).setMinWidth(sum);
				((Region) headerButton).setPrefWidth(sum);
//				((Region) headerButton).setPrefWidth(max);
				return sum;
			} else {
				Region headerRegion = (Region) headerButton;
				int minSize = Math.max(0, (int) Math.floor(Math.max(headerRegion.getPrefWidth(), headerRegion.getMinWidth())));

				int factor = (int) ((minSize / minHeaderButtonWidth) + 1);
				double size = factor * minHeaderButtonWidth;

				double spacing = 0;

				if (headerRegion.getParent() != null && headerRegion.getParent() instanceof HBox) {
					HBox parent = (HBox) headerRegion.getParent();
					spacing = (parent.getSpacing() * (parent.getChildren().size() - 1));
				}
				size = size - spacing;
				headerRegion.setMinWidth(size);
//				headerRegion.setMaxWidth(factor * minHeaderButtonWidth);
				headerRegion.setPrefWidth(size);
				return size + spacing;
			}
		}
		return minHeaderButtonWidth;
	}

	private void initMenu() {
		// Fit buttons to size
		toggleFFTView.widthProperty().addListener((e, oldV, newV) -> Platform
				.runLater(() -> minHeaderButtonWidth = FXMLUtil.setPrefWidthToMaximumRequired(toggleFFTView, toggleRTAView, toggleGroupsView, toggleDrumView, togglePhaseView, toggleBleedView)));

		toggleChannels.widthProperty().addListener((e, oldV, newV) -> Platform.runLater(() -> FXMLUtil.setPrefWidthToMaximumRequired(toggleChannels, toggleCue)));

		// Add Accelerator manually, makes working in the scen builder easier, because
		// save still works
		menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

		bindCheckMenuToToggleButton(menuSpectrumView, toggleFFTView);
		bindCheckMenuToToggleButton(menuRTAView, toggleRTAView);
		bindCheckMenuToToggleButton(menuGroupsView, toggleGroupsView);
		bindCheckMenuToToggleButton(menuDrumView, toggleDrumView);
		bindCheckMenuToToggleButton(menuPhaseView, togglePhaseView);
		bindCheckMenuToToggleButton(menuBleedView, toggleBleedView);
		menuTimerStart.setOnAction(e -> TimeKeeperController.getInstance().toggleTimer());
		menuTimerNext.setOnAction(e -> TimeKeeperController.getInstance().round());
		menuTimerStart.disableProperty().bind(TimeKeeperController.getInstance().getStartButton().disabledProperty());
		menuTimerNext.disableProperty().bind(TimeKeeperController.getInstance().getRoundButton().disabledProperty());
		toggleCue.selectedProperty().bindBidirectional(menuShowCue.selectedProperty());
		toggleChannels.selectedProperty().bindBidirectional(menuShowChannels.selectedProperty());
		// toggleTuner.selectedProperty().bindBidirectional(menuShowTuner.selectedProperty());
		menuShowCue.selectedProperty().addListener(e -> timeKeeperController.show(menuShowCue.isSelected()));
	}

	private void initBleedView() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(BLEED_PATH));
		if (p != null) {
			contentMap.put(toggleBleedView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load Bleed View");
		}
	}

	private void initPhaseMonitor() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(PHASE_PATH));
		if (p != null) {
			contentMap.put(togglePhaseView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load VectorScope");
		}
	}

	private void initRTA() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(RTA_PATH));
		if (p != null) {
			contentMap.put(toggleRTAView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initTimekeeper() {
		Parent p = FXMLUtil.loadFXML(getClass().getResource(TIMEKEEPER_PATH));
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

	private void initWaveForm() {
		LOG.debug("Loading WaveForm");
		waveFormChart = new SymmetricWaveFormChart();
		waveFormChart.setParentPausable(this);
		dataChart = new DataChart();
		for (PausableComponent n : new PausableComponent[] { dataChart, waveFormChart }) {
			n.setParentPausable(this);
			AnchorPane.setTopAnchor((Node) n, .0);
			AnchorPane.setBottomAnchor((Node) n, .0);
			AnchorPane.setLeftAnchor((Node) n, .0);
			AnchorPane.setRightAnchor((Node) n, .0);
			((Node) n).setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					if (toggleBtmRaw.isSelected()) {
						toggleBtmWave.fire();
					} else {
						toggleBtmRaw.fire();
					}
				} else if (e.getClickCount() == 1) {
					n.pause(!n.isPaused());
				}
			});
		}

		waveFormPane.getChildren().add(dataChart);
	}

	@FXML
	public void swapWaveRawEvent(ActionEvent e) {

		if (Objects.equals(e.getSource(), toggleBtmRaw) && dataChart.getParent() == null) {
			swapWaveAndDataChart();
		} else if (Objects.equals(e.getSource(), toggleBtmWave) && waveFormChart.getParent() == null) {
			swapWaveAndDataChart();
		} else {
			ToggleButton btn = (ToggleButton) e.getSource();
			btn.setSelected(true);
			e.consume();
		}
	}

	private void swapWaveAndDataChart() {
		LOG.debug("Swaping WaveForm and DataChart");
		Node n = waveFormPane.getChildren().get(0);
		waveFormPane.getChildren().clear();
		if (Objects.equals(n, dataChart)) {
			waveFormPane.getChildren().add(waveFormChart);
		} else if (Objects.equals(n, waveFormChart)) {
			waveFormPane.getChildren().add(dataChart);
		}
		dataChart.pause(Objects.equals(n, dataChart));
		waveFormChart.pause(Objects.equals(n, waveFormChart));

	}

	@Override
	public boolean isPaused() {
		return pause;
	}

	public boolean isShowHidden() {
		return showHidden;
	}

	@FXML
	private void newGroup(final ActionEvent e) {
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
		e.consume();
	}

	@FXML
	private void open(final ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showOpenDialog(root.getScene().getWindow());
		FileIO.open(result);
		e.consume();
	}

	@Override
	public void pause(final boolean pause) {
		if (this.pause != pause) {
			this.pause = pause;
			if (pause) {
				LOG.debug(getClass().getSimpleName() + "; pausing animations");
			} else {
				LOG.debug(getClass().getSimpleName() + "; playing animations");
			}
		}
	}

	public void refresh() {
		ObservableList<Integer> selectedItems = FXCollections.observableArrayList(channelList.getSelectionModel().getSelectedIndices());
		if (controller != null) {
			refreshInputs();
		}
		for (PausableView v : controllerMap.values()) {
			v.refresh();
		}
		for (Integer i : selectedItems) {
			channelList.getSelectionModel().select(i);
		}
	}

	private void refreshInputs() {
		channelList.getItems().clear();
		if (ASIOController.getInstance() != null) {
			if (toggleGroupChannels.isSelected()) {
				// add groups
				for (Group group : ASIOController.getInstance().getGroupList()) {
					channelList.getItems().add(group);
				}
			} else {
				// add channels
				// New Channel List
				List<Channel> newChanneList = ASIOController.getInstance().getInputList();
				// channels that are currently in the channelList, but should be removed since
				// they are not in the new list
				ArrayList<Channel> removeList = new ArrayList<>();

				// Searching all curent channels that are not in the new list
				for (Input cNew : channelList.getItems()) {
					Channel channelNew = (Channel) cNew;
					if (!newChanneList.contains(channelNew) || (channelNew.isHidden() && !menuShowHiddenChannels.isSelected())) {
						removeList.add(channelNew);
					}
				}
				// Removing all old channels
				channelList.getItems().removeAll(newChanneList);

				for (Channel channel : newChanneList) {
					// if channel is not hidden, or showHidden, and if
					// sterechannel isn't already added to list
					if ((!channel.isHidden() || showHidden) && (channel.getStereoChannel() == null || !channelList.getItems().contains(channel.getStereoChannel()))) {
						channelList.getItems().add(channel);
					}
				}
			}
			channelList.getItems().sort(Input.COMPARATOR);
		}
	}

	@FXML
	private void resetName(final ActionEvent e) {
		Input channel = channelList.getSelectionModel().getSelectedItem();
		if (channel != null && channel instanceof Channel) {
			((Channel) channel).resetName();
			refresh();
		}
		e.consume();
	}

	public void resetStatus() {
		setStatus("", 0);
	}

	@FXML
	public boolean save(final ActionEvent e) {
		setStatus("Saving", -1);
		boolean result;
		if (FileIO.getCurrentFile() != null) {
			result = FileIO.save(FileIO.getCurrentFile());
		} else {
			result = saveAs(e);
		}
		if (result) {
			InformationDialog dialog = new InformationDialog("Successfully saved");
			dialog.showAndWait();
		}
		resetStatus();
		e.consume();
		return result;
	}

	@FXML
	private boolean saveAs(final ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save to Directory");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null && timeKeeperController != null)
			return FileIO.save(result);
		e.consume();
		return false;
	}

	@FXML
	private void saveChannels(final ActionEvent e) {
		setStatus("Saving channels", -1);
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save channels");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null && timeKeeperController != null && ASIOController.getInstance() != null) {
			FileIO.save(new ArrayList<>(ASIOController.getInstance().getData()), result);
		}
		resetStatus();
		e.consume();
	}

	@FXML
	private void saveCues(final ActionEvent e) {
		setStatus("Saving cues", -1);
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save cues");
		chooser.setInitialDirectory(FileIO.getCurrentDir());
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File result = chooser.showSaveDialog(root.getScene().getWindow());
		if (result != null && timeKeeperController != null) {
			FileIO.save(new ArrayList<Serializable>(TimeKeeper.getInstance().getData()), result);
		}
		resetStatus();
		e.consume();
	}

	public void setChannelList(final List<Channel> list) {
		channelList.getItems().setAll(list);
	}

	public void setSelectedChannel(final Channel channel) {
		channelList.selectionModelProperty().get().select(channel);
	}

	public void setShowHidden(final boolean showHidden) {
		this.showHidden = showHidden;
		menuShowHiddenChannels.setSelected(showHidden);
	}

	public void setSongs(final Cue current, final Cue next) {
		showSong(current, lblCurrentSong);
		showSong(next, lblNextSong);
	}

	public void setStatus(final double value) {
		Platform.runLater(() -> {
			progStatus.setProgress(value);
			progStatus.setVisible(progStatus.getProgress() != 0);
			progStatus.setManaged(progStatus.getProgress() != 0);
		});
	}

	public void setStatus(final String text) {
		Platform.runLater(() -> {
			if (text != null && !text.isEmpty()) {
				lblStatus.setText(text);
				lblStatus.setVisible(true);
				Timeline line = new Timeline();
				KeyFrame key = new KeyFrame(Duration.seconds(5), e -> {
					if (progStatus.getProgress() == 0) {
						if (lblStatus.getText().equals(text)) {
							lblStatus.setText("");
							lblStatus.setVisible(false);
						}
					} else {
						line.playFrom(Duration.seconds(1));
					}
				});
				line.getKeyFrames().add(key);
				line.playFromStart();
			}
		});
	}

	public void setStatus(final String text, final double value) {
		setStatus(text);
		setStatus(value);
	}

	public void setTitle(final String title) {
		Stage stage = (Stage) channelList.getScene().getWindow();
		String finalTitle = Main.getReadableTitle();
		if (title != null && !title.isEmpty()) {
			finalTitle += " - " + title;
		}
		stage.setTitle(finalTitle);
	}

	public ButtonType showConfirmDialog(final String confirm) {
		Alert alert = new Alert(AlertType.NONE);
		FXMLUtil.setStyleSheet(alert.getDialogPane());
		alert.getDialogPane().setStyle(Main.getStyle());
		alert.initOwner(root.getScene().getWindow());
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.setTitle("Confirmation");
		alert.setHeaderText("Action is required");
		alert.setContentText(confirm);
		alert.getButtonTypes().clear();
		alert.getButtonTypes().add(ButtonType.CLOSE);
		alert.getButtonTypes().add(ButtonType.CANCEL);
		alert.getButtonTypes().add(ButtonType.OK);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent())
			return result.get();
		return ButtonType.CANCEL;
	}

	private void showSong(final Cue cue, final Label label) {
		if (cue != null) {
			String s = cue.getName();
			if (cue.getChannelToSelect() != null) {
				s += " " + cue.getChannelToSelect().getName();
			}
			label.setText(s);
		} else {
			label.setText("");
		}
		boolean hide = lblCurrentSong.getText().isEmpty() && lblNextSong.getText().isEmpty();
		bottomLabel.setVisible(!hide);
	}

	public Stage getStage() {
		return (Stage) channelList.getScene().getWindow();
	}

	@FXML
	private void close(ActionEvent e) {
		Main.getInstance().close();
	}

	@FXML
	private void openSettings(ActionEvent e) {
		Parent setting = FXMLUtil.loadFXML(getClass().getResource(SETTINGS_PATH));
		Stage settingStage = new Stage();
		settingStage.setTitle("Settings");
		FXMLUtil.setIcon(settingStage, Main.getLogoPath());
		FXMLUtil.setStyleSheet(setting);
		setting.setStyle(Main.getStyle());
		settingStage.setScene(new Scene(setting));
		settingStage.initOwner(root.getScene().getWindow());
		settingStage.initModality(Modality.NONE);
		settingStage.show();
	}

	public List<String> getPanels() {
		List<String> result = new ArrayList<>();
		// TODO Auto-generated method stub
		for (Toggle t : toggleFFTView.getToggleGroup().getToggles()) {
			ToggleButton tglBtn = (ToggleButton) t;
			result.add(tglBtn.getText());

		}
		return result;
	}

	private void applyLoadedProperties() {
		applyPanelProperty();
	}

	private void applyPanelProperty() {
		try {
			String panel = null;
			if (PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL) != null) {
				switch (RESTORE_PANEL.valueOf(PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL))) {
				case LAST:
					panel = PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_LAST);
					break;
				case SPECIFIC:
					panel = PropertiesIO.getProperty(Constants.SETTING_RESTORE_PANEL_SPECIFIC);
					break;
				case NOTHING:
				default:
					break;
				}
				if (panel != null) {
					for (ToggleButton b : contentMap.keySet()) {
						if (b.getText().equals(panel)) {
							b.fire();
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Unable to load panel restore settings");
		}
	}
}
