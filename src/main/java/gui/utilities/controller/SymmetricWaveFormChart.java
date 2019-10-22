package gui.utilities.controller;

import javafx.scene.effect.Reflection;

public class SymmetricWaveFormChart extends WaveFormChart {

	public SymmetricWaveFormChart() {
		super();
		Reflection reflection = new Reflection();
		reflection.setFraction(1);
		reflection.setTopOpacity(1.0);
		reflection.setBottomOpacity(1.0);
		heightProperty().addListener((e, oldv, newV) -> reflection.setTopOffset(-(newV.doubleValue() / 2.8)));
		setEffect(reflection);
	}

}
