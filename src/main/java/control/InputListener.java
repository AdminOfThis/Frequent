package control;

import data.Input;

public interface InputListener {

	public void levelChanged(final Input input, final double level, final long time);

	public void nameChanged(String name);

	public void colorChanged(String newColor);
}
