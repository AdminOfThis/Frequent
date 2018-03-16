package data;

import java.util.ArrayList;
import java.util.Comparator;

public class Track {
	private String			name		= "";
	private ArrayList<Tone>	toneList	= new ArrayList<>();

	public Track(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public synchronized ArrayList<Tone> getToneList() {
		return new ArrayList<Tone>(toneList);
	}

	public void addTone(Tone tone) {
		synchronized (this) {
			this.toneList.add(tone);
		}
	}
	
	public void clearToneList() {
		synchronized(this) {
			this.toneList.clear();
		}
	}

	public void setToneList(ArrayList<Tone> toneList) {
		this.toneList = toneList;
	}

	public void sortTones() {
		toneList.sort(new Comparator<Tone>() {
			@Override
			public int compare(Tone o1, Tone o2) {
				return Double.compare(o1.getFrequency(), o2.getFrequency());
			}
		});

	}

}
