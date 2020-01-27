package control;

import data.Input;

public interface InputListener {

	public void colorChanged(String newColor);

	public void levelChanged(final Input input, final double level, final long time);

	public void nameChanged(String name);
}
