package gui.controller;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import control.ASIOController;
import control.CueListener;
import control.FFTListener;
import control.TimeKeeper;
import data.Channel;
import data.Cue;
import data.FileIO;
import data.Group;
import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableView;
import gui.utilities.FXMLUtil;
import gui.utilities.controller.ChannelCell;
import gui.utilities.controller.WaveFormChart;
import gui.utilities.controller.WaveFormChart.Style;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.Main;

public class MainController implements Initializable, Pausable, CueListener {

	private static final String				FFT_PATH			= "/fxml/RTAView.fxml";
	private static final String				RTA_PATH			= "/fxml/FFTView.fxml";
	private static final String				TIMEKEEPER_PATH		= "/fxml/TimeKeeper.fxml";
	private static final String				GROUP_PATH			= "/fxml/GroupView.fxml";
	// private static final String BACKGROUND_PATH = "/fxml/Background.fxml";
	private static final String				DRUM_PATH			= "/fxml/DrumView.fxml";
	private static final String				PHASE_PATH			= "/fxml/VectorScopeView.fxml";
	private static final Logger				LOG					= Logger.getLogger(MainController.class);
	private static final ExtensionFilter	FILTER				= new ExtensionFilter(Main.getOnlyTitle() + " File", "*" + FileIO.ENDING);
	private static MainController			instance;
	@FXML
	private AnchorPane						waveFormPane;
	@FXML
	private HBox							buttonBox;
	@FXML
	private Node							bottomLabel;
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
	private BorderPane						root;
	@FXML
	private SplitPane						contentPane;
	@FXML
	private MenuItem						closeMenu, menuSave;
	@FXML
	private MenuItem						menuTimerStart, menuTimerNext;
	@FXML
	private ListView<Input>					channelList;
	@FXML
	private CheckMenuItem					menuShowCue, menuShowChannels, menuStartFFT;
	@FXML
	private Label							lblDriver, lblLatency, lblCurrentSong, lblNextSong;
	@FXML
	private SplitPane						channelPane;
	@FXML
	private Label							lblStatus;
	@FXML
	private ProgressBar						progStatus;
	private boolean							showHidden			= false;
	private boolean							pause				= false;
	private HashMap<ToggleButton, Node>		contentMap			= new HashMap<>();
	private HashMap<Node, PausableView>		controllerMap		= new HashMap<>();
	private double							channelSplitRatio	= 0.8;
	private ASIOController					controller;
	private TimeKeeperController			timeKeeperController;
	// private DrumController drumController;
	private WaveFormChart					waveFormController;

	public static String deriveColor(final String baseColor, final int index, final int total) {
		String result = baseColor;
		Color color = Color.web(baseColor);
		double value = (double) index / (double) total;
		color = color.deriveColor(1, 1, value, 1);
		result = FXMLUtil.toRGBCode(color);
		return result;
	}

	public static MainController getInstance() {
		return instance;
	}

	private void bindCheckMenuToToggleButton(final CheckMenuItem menu, final ToggleButton button) {
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
		toggleChannels.selectedProperty().bindBidirectional(root.getLeft().visibleProperty());
		toggleChannels.selectedProperty().bindBidirectional(root.getLeft().managedProperty());
		toggleChannels.selectedProperty().addListener(e -> pause(!toggleChannels.isSelected()));
		toggleWaveForm.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
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
		});
		channelList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		channelList.setCellFactory(e -> new ChannelCell());
		// channelList.setOnEditCommit(e ->
		// timeKeeperController.setChannels(channelList.getItems()));
		channelList.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Input>) (observable, oldValue, newValue) -> {
			if (newValue != null) {
				LOG.info("Switching to channel " + newValue.getName());
				for (PausableView v : controllerMap.values()) {
					v.setSelectedChannel(newValue);
				}
				if (newValue instanceof Channel) {
					Channel channel = (Channel) newValue;
					controller.setActiveChannel(channel.getChannel());
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
		Parent p = FXMLUtil.loadFXML(FFT_PATH);
		if (p != null) {
			contentMap.put(toggleFFTView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	private void initDrumMonitor() {
		Parent p = FXMLUtil.loadFXML(DRUM_PATH);
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
		Parent p = FXMLUtil.loadFXML(GROUP_PATH);
		if (p != null) {
			contentMap.put(toggleGroupsView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load FFT Chart");
		}
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		instance = this;
		setStatus("Loading GUI", -1);
		root.setStyle(Main.getStyle());
		Main.getInstance().setProgress(0.45);
		initWaveForm();
		Main.getInstance().setProgress(0.5);
		initTimekeeper();
		initMenu();
		initChannelList();
		Main.getInstance().setProgress(0.55);
		initFullScreen();
		initChart();
		Main.getInstance().setProgress(0.6);
		initRTA();
		Main.getInstance().setProgress(0.65);
		initDrumMonitor();
		Main.getInstance().setProgress(0.7);
		initPhaseMonitor();
		Main.getInstance().setProgress(0.75);
		initGroups();
		Main.getInstance().setProgress(0.8);
		initListener();
		Main.getInstance().setProgress(0.9);
		resetStatus();
		bottomLabel.setVisible(false);
		TimeKeeper.getInstance().addListener(this);
	}

	public void initIO(final String ioName) {
		controller = new ASIOController(ioName);
		controller.addFFTListener((FFTListener) controllerMap.get(contentMap.get(toggleFFTView)));
		controller.addFFTListener((FFTListener) controllerMap.get(contentMap.get(toggleRTAView)));
		timeKeeperController.setChannels(controller.getInputList());
		setChannelList(controller.getInputList());
		lblDriver.setText(ioName);
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
					if (contentPane.getItems().size() < 1) {
						contentPane.getItems().add(0, n);
					} else {
						contentPane.getItems().set(0, n);
					}
					PausableView v = controllerMap.get(n);
					buttonBox.getChildren().clear();
					if (v.getHeader() != null) {
						buttonBox.getChildren().addAll(v.getHeader());
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

	private void initPhaseMonitor() {
		Parent p = FXMLUtil.loadFXML(PHASE_PATH);
		if (p != null) {
			contentMap.put(togglePhaseView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
		} else {
			LOG.warn("Unable to load VectorScope");
		}
	}

	private void initRTA() {
		Parent p = FXMLUtil.loadFXML(RTA_PATH);
		if (p != null) {
			contentMap.put(toggleRTAView, p);
			controllerMap.put(p, (PausableView) FXMLUtil.getController());
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

	private void initWaveForm() {
		LOG.debug("Loading WaveForm");
		waveFormController = new WaveFormChart(Style.NORMAL);
		waveFormController.setParentPausable(this);
		waveFormPane.getChildren().add(waveFormController);
		AnchorPane.setTopAnchor(waveFormController, .0);
		AnchorPane.setBottomAnchor(waveFormController, .0);
		AnchorPane.setLeftAnchor(waveFormController, .0);
		AnchorPane.setRightAnchor(waveFormController, .0);
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
				for (Group group : ASIOController.getInstance().getGroupList()) {
					channelList.getItems().add(group);
				}
			} else {
				for (Channel channel : ASIOController.getInstance().getInputList()) {
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
		if (result != null && timeKeeperController != null) return FileIO.save(result);
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
	}

	public void setStatus(final String text, final double value) {
		setStatus(text);
		setStatus(value);
	}

	public void setTitle(final String title) {
		Stage stage = (Stage) channelList.getScene().getWindow();
		String finalTitle = Main.getTitle();
		if (title != null && !title.isEmpty()) {
			finalTitle += " - " + title;
		}
		stage.setTitle(finalTitle);
	}

	public ButtonType showConfirmDialog(final String confirm) {
		Alert alert = new Alert(AlertType.NONE);
		try {
			DialogPane dialogPane = alert.getDialogPane();
			dialogPane.getStylesheets().add(getClass().getResource(FXMLUtil.STYLE_SHEET).toExternalForm());
		}
		catch (Exception e) {
			LOG.warn("Unable to style dialog");
			LOG.debug("", e);
		}
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
		if (result.isPresent()) return result.get();
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
}
