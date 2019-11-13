package control;

import data.Input;

public interface WatchdogListener {

	public Void wentSilent(Input c);

	public Void reappeared(Input c);

}
