package main;

import java.util.Comparator;
import java.util.Objects;

import control.ASIOController;
import data.Channel;
import data.Group;
import data.Input;
import javafx.util.StringConverter;

/**
 * 
 * Class for constants
 * 
 * @author AdminOfThis
 *
 */
public final class Constants {
	/*************** Settings ********************/
	/* Keys */
	public static final String SETTING_RESTORE_PANEL = "gui.panel.restore";
	public static final String SETTING_RESTORE_PANEL_SPECIFIC = "gui.panel.restore.specific";
	public static final String SETTING_RESTORE_PANEL_LAST = "gui.panel.restore.last";

	public static final String SETTING_RELOAD_LAST_FILE = "data.file.reloadLast";
	public static final String SETTING_WARN_UNSAVED_CHANGES = "data.save.unsaved.warn";

	public enum RESTORE_PANEL {
		NOTHING, LAST, SPECIFIC

	};

	/*********** GUI **************/
	public static final double FFT_MIN = -90;
	public static final double RED = -2.0;
	public static final double YELLOW = -5.0;
	public static StringConverter<Channel> CHANNEL_CONVERTER = new StringConverter<Channel>() {

		@Override
		public Channel fromString(final String string) {
			if (ASIOController.getInstance() != null) {
				for (Channel c : ASIOController.getInstance().getInputList()) {
					if (Objects.equals(c.getName(), string)) {
						return c;
					}
				}
			}
			return null;
		}

		@Override
		public String toString(final Channel object) {
			if (object == null) {
				return "- NONE -";
			}
			return object.getName();
		}
	};

	public static final Comparator<Input> INPUT_COMPARATOR = (o1, o2) -> {
		if (o1 instanceof Channel && o2 instanceof Channel) {
			return ((Channel) o1).getChannelIndex() - ((Channel) o2).getChannelIndex();
		} else if (o1 instanceof Group && o2 instanceof Group) {
			return ((Group) o1).getName().compareTo(((Group) o2).getName());
		}
		return 0;
	};

	// PRIVATE //

	/**
	 * The caller references the constants using <tt>Consts.EMPTY_STRING</tt>, and
	 * so on. Thus, the caller should be prevented from constructing objects of this
	 * class, by declaring this private constructor.
	 */
	private Constants() {
		// this prevents even the native class from
		// calling this constructor as well :
		throw new AssertionError();
	}
}
