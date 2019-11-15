package control;

import data.Input;

public interface WatchdogListener {

	public void wentSilent(Input c, long time);

	public void reappeared(Input c);

}
