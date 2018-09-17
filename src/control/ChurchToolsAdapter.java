package control;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import data.Channel;
import data.Cue;

public class ChurchToolsAdapter {

	private static final Logger			LOG			= Logger.getLogger(ChurchToolsAdapter.class);
	private static final String[]		REMOVE_LEAD	= new String[] { "Ltg:", "Ltg.", "Leitung:", "Leitung" };
	private static final String			DATE_FORMAT	= "yyyy-MM-dd";
	private static final int[]			SERVICES	= new int[] { 9, 11, 16 };
	private transient String			login		= "";
	private transient String			password	= "";
	private static ChurchToolsAdapter	instance;

	public static ChurchToolsAdapter getInstance() {
		if (instance == null) {
			instance = new ChurchToolsAdapter();
		}
		return instance;
	}

	private ChurchToolsAdapter() {
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
	}

	public ArrayList<Cue> loadCues() {
		ArrayList<Cue> res = new ArrayList<>();
		int service = getClosestService();
		String sunday = getNextSundayString() + " " + String.format("%02d:00:00", service);
		LOG.info("Trying to load songs for sunday: " + sunday);
		String error = "Unknown error";
		try {
			error = "Unable to log in";
			logIn(login, password);
			error = "Unable to find event";
			int eventId = loadEventID(sunday);
			LOG.info("Found Event-ID: " + eventId);
			error = "Unable to find agenda for event";
			int agendaId = loadAgendaId(eventId);
			LOG.info("Found Agenda-ID: " + agendaId);
			error = "Unable to load songs";
			TreeMap<Integer, String> songIds = loadSongIDs(agendaId);
			for (int i : songIds.keySet()) {
				try {
					String songName = loadSongName(i);
					int secondsTime = loadTime(agendaId, i);
					String lead = songIds.get(i);
					res.add(createCue(songName, lead, secondsTime));
				} catch (Exception e) {
					LOG.warn("Unable to load data for song " + i);
					LOG.debug("", e);
				}
			}
		} catch (Exception e) {
			LOG.warn("Unable to load data; " + error);
			LOG.debug("", e);
		}
		String log = "Loaded " + res.size() + " cues:";
		for (Cue c : res) {
			log += " " + c.getName();
			if (c.getChannelToSelect() != null) {
				log += " (" + c.getChannelToSelect().getName() + ")";
			}
			log += ",";
		}
		log = log.substring(0, log.length() - 1);
		LOG.info(log);
		return res;
	}

	private int loadTime(int agendaId, int songID) throws Exception {
		String allData = getData("GET", "churchservice/ajax",
			"func=" + URLEncoder.encode("loadAgendaItems", "UTF-8") + "&" + "agenda_id=" + URLEncoder.encode(agendaId + "", "UTF-8"));
		JSONObject json = new JSONObject(allData);
		JSONObject data = json.getJSONObject("data");
		for (String s : data.keySet()) {
			JSONObject obj = data.getJSONObject(s);
			if (obj.get("bezeichnung").equals("Song")) {
				if (obj.getInt("arrangement_id") == songID) {
					return obj.getInt("duration");
				}
			}
		}
		return 0;
	}

	private Cue createCue(String songName, String lead, int time) {
		Cue cue = new Cue(songName);
		cue.setTime((long) (time * 1000.0));
		if (lead != null && lead.length() > 2) {
			// normalinzing string
			for (String remove : REMOVE_LEAD) {
				if (lead.toLowerCase().startsWith(remove.toLowerCase())) {
					lead = lead.substring(remove.length() + 1, lead.length());
					break;
				}
			}
			lead = lead.trim();
			// searching channel with equals
			if (ASIOController.getInstance() != null) {
				Channel leadChannel = null;
				for (Channel c : ASIOController.getInstance().getInputList()) {
					if (c.getName().equalsIgnoreCase(lead)) {
						leadChannel = c;
						break;
					}
				} // if no result, try with contains
				if (leadChannel == null) {
					for (Channel c : ASIOController.getInstance().getInputList()) {
						if (c.getName().toLowerCase().contains(lead.toLowerCase())) {
							leadChannel = c;
							break;
						}
					}
				}
				if (leadChannel != null) {
					cue.setChannelToSelect(leadChannel);
				}
			}
		}
		return cue;
	}

	private String loadSongName(int songId) throws Exception {
		String allData = getData("GET", "churchservice/ajax", "func=" + URLEncoder.encode("getAllSongs", "UTF-8"));
		try {
			JSONObject json = new JSONObject(allData);
			JSONObject data = json.getJSONObject("data");
			JSONObject songs = data.getJSONObject("songs");
			for (String key : songs.keySet()) {
				JSONObject song = songs.getJSONObject(key);
				JSONObject arrangements = song.getJSONObject("arrangement");
				for (String arrangementKey : arrangements.keySet()) {
					JSONObject arrangement = arrangements.getJSONObject(arrangementKey);
					int arrangementID = arrangement.getInt("id");
					if (arrangementID == songId) {
						return song.getString("bezeichnung");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "NOT FOUND";
	}

	/**
	 * loads arrangement ids and lead singer from agenda, and sorts them by
	 * sortkey
	 * 
	 * @param agendaId
	 * @return
	 * @throws Exception
	 */
	private TreeMap<Integer, String> loadSongIDs(int agendaId) throws Exception {
		TreeMap<Integer, Integer> map = new TreeMap<>();
		TreeMap<Integer, String> leadMap = new TreeMap<>();
		String allData = getData("GET", "churchservice/ajax",
			"func=" + URLEncoder.encode("loadAgendaItems", "UTF-8") + "&" + "agenda_id=" + URLEncoder.encode(agendaId + "", "UTF-8"));
		JSONObject json = new JSONObject(allData);
		JSONObject data = json.getJSONObject("data");
		for (String s : data.keySet()) {
			JSONObject obj = data.getJSONObject(s);
			if (obj.get("bezeichnung").equals("Song")) {
				int sortkey = obj.getInt("sortkey");
				map.put(sortkey, obj.getInt("arrangement_id"));
				String lead = null;
				try {
					lead = obj.getString("note");
				} catch (Exception e) {
				}
				leadMap.put(sortkey, lead);
			}
		}
		// sorting by sortkey
		TreeMap<Integer, String> res = new TreeMap<>();
		for (Integer i : map.keySet()) {
			res.put(map.get(i), leadMap.get(i));
		}
		return res;
	}

	private int loadAgendaId(int eventId) throws Exception {
		String allData = getData("GET", "churchservice/ajax",
			"func=" + URLEncoder.encode("loadAgendaForEvent", "UTF-8") + "&" + "event_id=" + URLEncoder.encode(eventId + "", "UTF-8"));
		JSONObject json = new JSONObject(allData);
		JSONObject data = json.getJSONObject("data");
		return data.getInt("id");
	}

	private int loadEventID(String nexGoDi) throws Exception {
		String allData = getData("GET", "churchservice/ajax", "func=" + URLEncoder.encode("getAllEventData", "UTF-8"));
		JSONObject json = new JSONObject(allData);
		JSONObject data = json.getJSONObject("data");
		for (String s : data.keySet()) {
			JSONObject obj = data.getJSONObject(s);
			if (obj.get("startdate").equals(nexGoDi)) {
				return obj.getInt("id");
			}
		}
		return -1;
	}

	private boolean logIn(String user, String password) throws Exception {
		String paramsLogin = "email=" + URLEncoder.encode(user, "UTF-8") + "&" + "password=" + URLEncoder.encode(password, "UTF-8") + "&"
			+ "directtool=" + URLEncoder.encode("yes", "UTF-8") + "&" + "func=" + URLEncoder.encode("login", "UTF-8");
		String s = getData("POST", "login/ajax", paramsLogin);
		JSONObject json = new JSONObject(s);
		String suc = json.getString("status");
		boolean success = suc.equals("success");
		if (success) {
			LOG.info("Logged in to Churchtools");
		}
		return success;
	}

	private static String getData(String method, String request, String params) {
		ArrayList<String> res = new ArrayList<>();
		try {
			URL churchTestURL = new URL("https://gospelforum.church.tools/?q=" + request);
			HttpsURLConnection connection = (HttpsURLConnection) churchTestURL.openConnection();
			connection.setRequestMethod(method);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			if (params != null) {
				connection.setRequestProperty("Content-Length", String.valueOf(params.length()));
			}
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			if (params != null) {
				writer.write(params);
			}
			writer.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			for (String line; (line = reader.readLine()) != null;) {
				if (!line.isEmpty()) {
					res.add(line);
				}
			}
			writer.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res.get(0);
	}

	private String getNextSundayString() {
		return getNextSundayString(new GregorianCalendar());
	}

	private String getNextSundayString(GregorianCalendar c) {
		// DEBUG
		c = getNextSunday(c);
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
		return format.format(c.getTime());
	}

	private GregorianCalendar getNextSunday() {
		return getNextSunday(new GregorianCalendar());
	}

	private GregorianCalendar getNextSunday(GregorianCalendar c) {
		while (c.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.SUNDAY) {
			c.add(GregorianCalendar.DAY_OF_YEAR, 1);
		}
		return c;
	}

	private int getClosestService() {
		GregorianCalendar c = new GregorianCalendar();
		HashMap<Integer, GregorianCalendar> map = new HashMap<>();
		for (int i : SERVICES) {
			GregorianCalendar s = getNextSunday();
			s.set(GregorianCalendar.HOUR_OF_DAY, i);
			map.put(i, s);
		}
		int closest = 0;
		long difference = -1;
		for (Entry<Integer, GregorianCalendar> e : map.entrySet()) {
			long testTime = e.getValue().getTimeInMillis();
			if (difference < 0) {
				difference = Math.abs(c.getTimeInMillis() - testTime);
				closest = e.getKey();
			} else {
				long currentTime = c.getTimeInMillis();
				if (Math.abs(testTime - currentTime) < difference) {
					difference = testTime - currentTime;
					closest = e.getKey();
				}
			}
		}
		return closest;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isLoggedIn() {
		if (login == null || login.isEmpty() || password == null || password.isEmpty()) {
			return false;
		} else {
			try {
				return logIn(login, password);
			} catch (Exception e) {
				LOG.warn("Problem checking Login");
				return false;
			}
		}
	}
}
