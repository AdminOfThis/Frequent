package control;

import java.util.ArrayList;
import java.util.List;

import data.Cue;
import data.FileIO;
import gui.controller.DataHolder;

public class TimeKeeper implements DataHolder<Cue> {

	private static final String	DEFAULT_CUE_NAME	= "Song #";
	private static TimeKeeper	instance;
	private long				startTime;
	private long				roundStartTime;
	private List<Cue>			cueList				= new ArrayList<>();
	private int					activeIndex			= 0;

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

	public long getTimeRunning() {
		return System.currentTimeMillis() - startTime;
	}

	public long getRoundTime() {
		return System.currentTimeMillis() - roundStartTime;
	}

	public long round() {
		activeIndex++;
		long currentTime = System.currentTimeMillis();
		long returnTime = currentTime - roundStartTime;
		roundStartTime = currentTime;
		return returnTime;
	}

	public void reset() {
		startTime = System.currentTimeMillis();
		roundStartTime = startTime;
		activeIndex = 0;
	}

	public void addCue(Cue trim) {
		cueList.add(trim);
	}

	public List<Cue> getCueList() {
		return cueList;
	}

	public int getActiveIndex() {
		return activeIndex;
	}

	public Cue getActiveCue() {
		if (cueList.size() < getActiveIndex() + 1) {
			cueList.add(new Cue(DEFAULT_CUE_NAME + (activeIndex + 1)));
		}
		return cueList.get(getActiveIndex());
	}

	public void removeCue(Cue del) {
		if (activeIndex == 0) {
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
