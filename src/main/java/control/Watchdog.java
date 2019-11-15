package control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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

	private List<WatchdogListener> listeners = Collections.synchronizedList(new ArrayList<WatchdogListener>());
	private List<Input> missingInputs = Collections.synchronizedList(new ArrayList<Input>());

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
		for (Entry<Long, Input> entry : watchMap.entries()) {
			Input input = entry.getValue();
			long lastHeartbeat = 0;
			if (heartBeatMap.containsKey(entry.getValue())) {
				lastHeartbeat = heartBeatMap.get(entry.getValue());
			}
			long time = System.currentTimeMillis();

			// check for heartbeat in allowed time window
			if (time - lastHeartbeat > entry.getKey() * 1000) {

				// the signals last ehartbeat was too long ago
				if (!missingInputs.contains(input)) {
					listeners.forEach(l -> new Thread(() -> l.wentSilent(entry.getValue(), entry.getKey())).start());

					missingInputs.add(input);
					LOG.info(input.getName() + " signal dissapeared (" + entry.getKey() + "s)");
				}
			} else if (missingInputs.contains(entry.getValue())) {
				// if not missing, but still marked as missing
				listeners.forEach(l -> new Thread(() -> l.reappeared(entry.getValue())).start());
				missingInputs.remove(input);
				LOG.info(input.getName() + " signal reappeared");
			}
		}
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

	@Override
	public void levelChanged(Input input, double level, long time) {
		double leveldB = Channel.percentToDB(level);
		if (leveldB > ALIVE_THRESHOLD) {
			heartBeatMap.put(input, System.currentTimeMillis());
		}
	}

	/****** LISTENERS **************/
	public void addListener(WatchdogListener lis) {
		if (!listeners.contains(lis)) {
			listeners.add(lis);
		}
	}

	public boolean removeListener(WatchdogListener lis) {
		return listeners.remove(lis);
	}

	/******* WatchMap functions ***************/
	public void addEntry(long seconds, Input channel) {
		if (!watchMap.containsValue(channel) && channel != null) {
			watchMap.put(seconds, channel);
			heartBeatMap.put(channel, System.currentTimeMillis());
			channel.addListener(this);
		}
	}

	public void removeEntry(Input input) {
		if (input != null) {
			for (Entry<Long, Input> entry : watchMap.entries()) {
				if (entry.getValue().equals(input)) {
					watchMap.remove(entry.getKey());
					break;
				}
			}
			missingInputs.remove(input);
			input.removeListener(this);
		}

	}

	public void clear() {
		watchMap.clear();
	}

	public int getSize() {
		return watchMap.size();
	}

	/************* GETTER AND SETTER ****************/
	public List<Input> getMissingInputs() {
		return new ArrayList<Input>(missingInputs);
	}

	public long getTimeForInput(Input input) {
		long result = 0;
		for (Entry<Long, Input> e : watchMap.entries()) {
			if (Objects.equals(e.getValue(), input)) {
				result = e.getKey();
				break;
			}
		}
		return result;
	}
}