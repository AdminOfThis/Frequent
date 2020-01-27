package control;

import data.Input;

public interface WatchdogListener {

	public void reappeared(Input c);

	public void wentSilent(Input c, long time);

}
