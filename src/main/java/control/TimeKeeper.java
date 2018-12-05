package control;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import data.Cue;
import data.FileIO;

public final class TimeKeeper implements DataHolder<Cue> {

	private static final Logger					LOG					= Logger.getLogger(TimeKeeper.class);

	public static final String					DEFAULT_CUE_NAME	= "Song #";
	private static TimeKeeper					instance;
	private long								startTime;
	private long								roundStartTime;
	private long								pauseStarttime;
	private long								pauseTime, pauseRoundTime;
	private List<Cue>							cueList				= new ArrayList<>();
	private int									activeIndex			= -1;
	private static final ArrayList<CueListener>	listeners			= new ArrayList<>();
	private boolean										pause				= false;

	public static TimeKeeper getInstance() {
		if (instance == null) {
			instance = new TimeKeeper();
		}
		return instance;
	}


	private TimeKeeper() {
		FileIO.registerSaveData(this);
		reset();
	}

	public void addListener(CueListener lis) {
		if (lis != null && !listeners.contains(lis)) {
			listeners.add(lis);
		}
	}

	public void removeListener(CueListener lis) {
		listeners.remove(lis);
	}

	public long getTimeRunning() {
		// runtime
		long result = (System.currentTimeMillis() - startTime);
		// minus pause time
		result = result - pauseTime;
		// if pause running minus running pause time
		if (pause) {
			result = result - (System.currentTimeMillis() - pauseStarttime);
		}
		return result;
	}

	public long getRoundTime() {
		// runtime
		long result = (System.currentTimeMillis() - roundStartTime);
		// minus pause time
		result = result - pauseRoundTime;
		// if pause running minus running pause time
		if (pause) {
			result = result - (System.currentTimeMillis() - pauseStarttime);
		}
		return result;
	}

	public long round() {
		LOG.info("Adding round to timer");
		activeIndex++;
		pauseRoundTime = 0;
		long currentTime = System.currentTimeMillis();
		long returnTime = currentTime - roundStartTime;
		roundStartTime = currentTime;
		notifyObservers();
		return returnTime;
	}

	private void notifyObservers() {
		for (CueListener lis : listeners) {
			new Thread(() -> lis.currentCue(getActiveCue(), getNextCue())).start();
		}

	}

	public void reset() {
		LOG.info("Resetting timer");
		startTime = System.currentTimeMillis();
		pauseTime = 0;
		pause = false;
		roundStartTime = startTime;
		activeIndex = -1;
		notifyObservers();
	}

	public void pause() {
		pause(!pause);
	}

	public void pause(boolean pause) {
		this.pause = pause;
		if (pause) {
			LOG.info("Pausing timer");
			pauseStarttime = System.currentTimeMillis();

		} else {

			LOG.info("Unpausing timer");
			pauseTime = pauseTime + System.currentTimeMillis() - pauseStarttime;
			pauseRoundTime = pauseRoundTime + System.currentTimeMillis() - pauseStarttime;
		}
	}

	public boolean isPaused() {
		return pause;
	}

	public void addCue(Cue trim) {
		cueList.add(trim);
		notifyObservers();
	}

	public List<Cue> getCueList() {
		return cueList;
	}

	public int getActiveIndex() {
		return activeIndex;
	}

	public Cue getActiveCue() {
		if (getActiveIndex() < 0) {
			return null;
		}
		if (cueList.size() < getActiveIndex() + 1) {
			cueList.add(new Cue(DEFAULT_CUE_NAME + (activeIndex + 1)));
		}
		return cueList.get(getActiveIndex());
	}

	public Cue getNextCue() {
		if (cueList.size() < getActiveIndex() + 2) {
			return null;
		} else {
			return cueList.get(getActiveIndex() + 1);
		}
	}

	public void removeCue(Cue del) {
		if (activeIndex == -1) {
			cueList.remove(del);
		}
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public void add(Cue t) {
		if (!cueList.contains(t)) {
			cueList.add(t);
		}
	}

	@Override
	public void set(List<Cue> list) {
		cueList = list;
	}

	@Override
	public List<Cue> getData() {
		return cueList;
	}

	@Override
	public void clear() {
		cueList.clear();
	}
}
