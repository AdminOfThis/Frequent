package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import control.DataHolder;

/**
 * 
 * @author AdminOfThis Data Managing class for Custom colors, implemented as
 *         singleton
 *
 */
public class ColorController implements DataHolder<ColorEntry> {

	private static ColorController instance;
	public static ColorController getInstance() {
		if (instance == null) {
			instance = new ColorController();
		}
		return instance;
	}

	private List<ColorEntry> colors = Collections.synchronizedList(new ArrayList<ColorEntry>());

	private ColorController() {
		FileIO.registerSaveData(this);
	}

	@Override
	public void add(Object t) {
		if (t instanceof ColorEntry) {
			if (!colors.contains(t)) {
				colors.add((ColorEntry) t);
			}
		}
	}

	public void addColor(String name, String color) {
		ColorEntry entry = new ColorEntry(name, color);
		colors.add(entry);
	}

	@Override
	public void clear() {
		colors.clear();
	}

	public List<ColorEntry> getColors() {
		return colors;
	}

	@Override
	public List<ColorEntry> getData() {
		return colors;
	}

	public void removeColor(ColorEntry entry) {
		colors.remove(entry);
	}

	public void removeColor(String name) {
		for (ColorEntry e : colors) {
			if (e.getName().equals(name)) {
				colors.remove(e);
				break;
			}
		}
	}

	@Override
	public void set(List<ColorEntry> list) {
		colors = Collections.synchronizedList(list);
	}
}
