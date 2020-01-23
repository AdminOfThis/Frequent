package main;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Calendar;
import java.util.GregorianCalendar;
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

	/* LICENSE */
	public static final String LICENSE_TEXT = "Copyright " + new GregorianCalendar().get(Calendar.YEAR) + " Florian Hild\r\n" + "\r\n"
			+ "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\r\n"
			+ "\r\n" + "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\r\n" + "\r\n"
			+ "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";

	public static final String GITHUB_REPOSITORY_URL = "https://github.com/AdminOfThis/Frequent";

	/*************** Settings ********************/
	/* Keys */

	/** Setting wether the db Label should display current or peak values */
	public static final String SETTING_DB_LABEL_CURRENT = "gui.label.db.current";
	/** Setting, wether a specific panel should be opened on start */
	public static final String SETTING_RESTORE_PANEL = "gui.panel.restore";
	/** If open specific panel is selected, index of panel to be openend */
	public static final String SETTING_RESTORE_PANEL_SPECIFIC = "gui.panel.restore.specific";
	/**
	 * if open last is selected, stores the last opened panel, gets updated every
	 * time a new panel is selected
	 */
	public static final String SETTING_RESTORE_PANEL_LAST = "gui.panel.restore.last";

	public static final String SETTING_RELOAD_LAST_FILE = "data.file.reloadLast";
	public static final String SETTING_WARN_UNSAVED_CHANGES = "data.save.unsaved.warn";

	public static final String SETTING_ERROR_REPORTING = "log.reporting";

	public static final String SETTING_WATCHDOG_THRESHOLD = "watchdog.threshold";

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

	public static final String LOCK_FILE = "frequent.lock";

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
