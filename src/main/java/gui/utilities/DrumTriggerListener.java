package gui.utilities;

import data.DrumTrigger;

public interface DrumTriggerListener {

	public void tresholdReached(DrumTrigger trigger, double level, double treshold);
}
