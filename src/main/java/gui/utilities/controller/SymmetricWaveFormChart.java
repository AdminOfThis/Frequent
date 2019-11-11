package gui.utilities.controller;

import data.Input;
import gui.pausable.Pausable;
import gui.pausable.PausableComponent;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.AnchorPane;

public class SymmetricWaveFormChart extends AnchorPane implements PausableComponent {

	private WaveFormChart chart = new WaveFormChart();

	public SymmetricWaveFormChart() {
		super();
		getChildren().add(chart);
		AnchorPane.setTopAnchor(chart, .0);
		heightProperty().addListener((e, oldV, newV) -> AnchorPane.setBottomAnchor(chart, newV.doubleValue() / 2.0));
//		AnchorPane.setBottomAnchor(chart, .0);
		AnchorPane.setLeftAnchor(chart, .0);
		AnchorPane.setRightAnchor(chart, .0);
		Reflection reflection = new Reflection();
		reflection.setFraction(1);
		reflection.setTopOpacity(1.0);
		reflection.setBottomOpacity(1.0);
		reflection.setTopOffset(-50);
//		heightProperty().addListener((e, oldv, newV) -> {
//			
//		});
		chart.setEffect(reflection);
//		chart.setStyle("-fx-border-color: red; -fx-border-width: 1px; -fx-insets: 0;-fx-margin: 0px;-fx-padding: 0px;");

//		chart.heightProperty().addListener((e, oldV, newV) -> {
//			setMinHeight(newV.doubleValue() * 2.0);
//			setMaxHeight(newV.doubleValue() * 2.0);
//			setPrefHeight(newV.doubleValue() * 2.0);
//		});
	}

	@Override
	public void pause(final boolean pause) {
		chart.pause(pause);
	}

	@Override
	public boolean isPaused() {
		return chart.isPaused();
	}

	@Override
	public void setParentPausable(final Pausable parent) {
		chart.setParentPausable(parent);
	}

	public void setChannel(Input newValue) {
		chart.setChannel(newValue);
	}

	public void showTreshold(boolean b) {
		chart.showTreshold(b);
	}

	public void setThreshold(double abs) {
		chart.setThreshold(abs);
	}

}
