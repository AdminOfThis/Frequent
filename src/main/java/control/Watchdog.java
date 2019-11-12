package control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mortbay.log.Log;

import data.Channel;
import data.Input;

public class Watchdog implements InputListener {

	private static final Logger LOG = LogManager.getLogger(Watchdog.class);
	private static final double ALIVE_THRESHOLD = -85;

	private static Watchdog instance;

	private ArrayListValuedHashMap<Long, Input> watchMap = new ArrayListValuedHashMap<Long, Input>();
	private Map<Input, Long> heartBeatMap = Collections.synchronizedMap(new HashMap<Input, Long>());
	private ScheduledExecutorService exec;

	private Watchdog() {
		exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(() -> {
			try {
				check();
			} catch (Exception e) {
				LOG.warn("Problem on watchdog", e);
			}
		}, 5, 1, TimeUnit.SECONDS);
	}

	public static Watchdog getInstance() {
		if (instance == null) {
			instance = new Watchdog();
		}
		return instance;
	}

	private void check() {

	}

	public void finish() {
		if (exec != null) {
			exec.shutdown();
		}
		for (Input input : watchMap.values()) {
			input.removeListener(this);
		}
		Log.info("Watchdog terminated");
	}

	public void addEntry(long seconds, Input channel) {
		if (!watchMap.containsValue(channel) && channel != null) {
			watchMap.put(seconds, channel);
			channel.addListener(this);
		}
	}

	public void removeEntry(Input input) {
		if (input != null) {
			watchMap.remove(input);
			input.removeListener(this);
		}

	}

	public void clear() {
		watchMap.clear();
	}

	public int getSize() {
		return watchMap.size();
	}

	@Override
	public void levelChanged(Input input, double level, long time) {
		double leveldB = Channel.percentToDB(level);
		if (leveldB > ALIVE_THRESHOLD) {
			heartBeatMap.put(input, System.currentTimeMillis());
		}
	}

}
