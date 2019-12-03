package main;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import control.ASIOController;
import data.Channel;
import javafx.util.StringConverter;

/**
 * 
 * Class for constants
 * 
 * @author AdminOfThis
 *
 */
public final class Constants {
	// Will log on behalf of mains class, since it is used there
	private static final Logger LOG = LogManager.getLogger(Main.class);

	/*************** Settings ********************/
	/* Keys */
	public static final String SETTING_RESTORE_PANEL = "gui.panel.restore";
	public static final String SETTING_RESTORE_PANEL_SPECIFIC = "gui.panel.restore.specific";
	public static final String SETTING_RESTORE_PANEL_LAST = "gui.panel.restore.last";

	public static final String SETTING_RELOAD_LAST_FILE = "data.file.reloadLast";
	public static final String SETTING_WARN_UNSAVED_CHANGES = "data.save.unsaved.warn";

	public static final String SETTING_ERROR_REPORTING = "log.reporting";

	public static final int LOG4J_INDEX_REPORTING = 0;
	public static final int LOG4J_INDEX_VERSION = 1;
	public static final int LOG4J_INDEX_ENVIRONMENT = 2;

	public enum RESTORE_PANEL {
		NOTHING, LAST, SPECIFIC

	};

	/*********** GUI **************/
	public static final double FFT_MIN = -90;
	public static final double RED = -2.0;
	public static final double YELLOW = -5.0;
	public static final UncaughtExceptionHandler EMERGENCY_EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread th, Throwable ex) {
			LOG.fatal("Uncaught exception in thread \"" + th.getName() + "\".", ex);
		}
	};

	public static StringConverter<Channel> CHANNEL_CONVERTER = new StringConverter<Channel>() {

		@Override
		public Channel fromString(final String string) {
			for (Channel c : ASIOController.getInstance().getInputList()) {
				if (Objects.equals(c.getName(), string)) {
					return c;
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
