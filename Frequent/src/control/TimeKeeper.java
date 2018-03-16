package control;

import java.util.ArrayList;
import java.util.List;

import data.Cue;

public class TimeKeeper {


	private long			startTime;
	private long			roundStartTime;
	private ArrayList<Cue>	cueList		= new ArrayList<>();
	private int				activeIndex	= 0;


	public TimeKeeper() {
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
			cueList.add(new Cue("Song " + (activeIndex + 1)));
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


}
