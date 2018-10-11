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
import java.util.LinkedHashMap;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import data.Channel;
import data.Cue;
import data.FileIO;

public class ChurchToolsAdapter {

	private static final String							PREFERENCES_LOGIN_KEY	= "login.user";
	private static final Logger							LOG						= Logger.getLogger(ChurchToolsAdapter.class);
	private static final String[]						REMOVE_LEAD				= new String[] { "Ltg:", "Ltg.", "Leitung:", "Leitung" };
	private static final String							DATETIME_FORMAT			= "yyyy-MM-dd HH:mm:ss";
	private static final LinkedHashMap<Integer, String>	serviceIDs				= new LinkedHashMap<>();
	static {
		serviceIDs.put(1, "Sprecher");
		serviceIDs.put(3, "Moderator");
		serviceIDs.put(2, "Band-Leitung");
		serviceIDs.put(136, "Celebration-Manager");
		serviceIDs.put(6, "FoH");
		serviceIDs.put(7, "Licht");
		serviceIDs.put(22, "Beamer-Medien");
		serviceIDs.put(29, "Beamer-Liedtexte");
		serviceIDs.put(16, "Stream-Kamera");
		serviceIDs.put(37, "Stream-Schnitt");
		serviceIDs.put(38, "Stream-Supervisor");
		serviceIDs.put(17, "Stream-Sound");
	}
	private transient String				login			= "";
	private transient String				password		= "";
	private static ChurchToolsAdapter		instance;
	private LinkedHashMap<String, String>	additionalInfos	= new LinkedHashMap<>();

	public LinkedHashMap<String, String> getAdditionalInfos() {
		return new LinkedHashMap<>(additionalInfos);
	}

	public static ChurchToolsAdapter getInstance() {
		if (instance == null) {
			instance = new ChurchToolsAdapter();
		}
		return instance;
	}

	private ChurchToolsAdapter() {
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		login = FileIO.readPropertiesString(PREFERENCES_LOGIN_KEY, "");
	}

	public ArrayList<Cue> loadCues() {
		additionalInfos.clear();
		String error = "Unknown error";
		ArrayList<Cue> res = new ArrayList<>();
		try {
			error = "Unable to log in";
			logIn(login, password);
			error = "Unable to find event";
			int eventId = loadEventID();
			LOG.info("Found Event-ID: " + eventId);
			error = "Unable to find agenda for event";
			int agendaId = loadAgendaId(eventId);
			LOG.debug("Found Agenda-ID: " + agendaId);
			error = "Unable to load songs";
			res = loadSongs(agendaId);
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
			if (obj.get("bezeichnung").equals("Song") && obj.getInt("arrangement_id") == songID) {
				return obj.getInt("duration");
			}
		}

		return 0;
	}

	private Cue createCue(final String songName, final String lead, final int time) {
		Cue cue = new Cue(songName);
		cue.setTime((long) (time * 1000.0));
		if (lead != null && lead.length() > 2) {
			// normalinzing string
			String leadName;
			for (String remove : REMOVE_LEAD) {
				if (lead.toLowerCase().startsWith(remove.toLowerCase())) {
					leadName = lead.substring(remove.length() + 1, lead.length());
					break;
				}
			}
			leadName = lead.trim();
			// searching channel with equals
			if (ASIOController.getInstance() != null) {
				Channel leadChannel = null;
				for (Channel c : ASIOController.getInstance().getInputList()) {
					if (c.getName().equalsIgnoreCase(leadName)) {
						leadChannel = c;
						break;
					}
				} // if no result, try with contains
				if (leadChannel == null) {
					for (Channel c : ASIOController.getInstance().getInputList()) {
						if (c.getName().toLowerCase().contains(leadName.toLowerCase())) {
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
	private ArrayList<Cue> loadSongs(int agendaId) throws Exception {
		ArrayList<Cue> res = new ArrayList<>();
		TreeMap<Integer, Cue> map = new TreeMap<>();
		String allData = getData("GET", "churchservice/ajax",
			"func=" + URLEncoder.encode("loadAgendaItems", "UTF-8") + "&" + "agenda_id=" + URLEncoder.encode(agendaId + "", "UTF-8"));
		JSONObject json = new JSONObject(allData);
		JSONObject data = json.getJSONObject("data");
		for (String s : data.keySet()) {
			JSONObject obj = data.getJSONObject(s);
			String title = null;
			String lead = null;
			int time = 0;
			if (obj.getString("bezeichnung").equals("Song")) {
				int sortkey = obj.getInt("sortkey");
				int arrangementID = obj.getInt("arrangement_id");
				title = loadSongName(arrangementID);
				time = loadTime(agendaId, sortkey);
			} else if (obj.getString("bezeichnung").startsWith("Song")) {
				try {
					title = obj.getString("bezeichnung").replace("Song", "");
					title = title.trim();
					if (title.startsWith("-")) {
						title = title.replace("-", "");
						title = title.trim();
					}
				} catch (Exception e) {
				}
			}
			if (title != null) {
				int index = 0;
				try {
					lead = obj.getString("note");
					index = obj.getInt("sortkey");
				} catch (Exception e) {
					e.printStackTrace();
				}
				// adding song
				map.put(index, createCue(title, lead, time));
			}
		}
		for (Integer i : map.keySet()) {
			res.add(map.get(i));
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

	private int loadEventID() throws Exception {
		String allData = getData("GET", "churchservice/ajax", "func=" + URLEncoder.encode("getAllEventData", "UTF-8"));
		int eventId = -1;
		GregorianCalendar time = new GregorianCalendar();
		// time.set(GregorianCalendar.HOUR_OF_DAY, 0);
		time.set(GregorianCalendar.MINUTE, 0);
		time.set(GregorianCalendar.SECOND, 0);
		time.set(GregorianCalendar.MILLISECOND, 0);
		SimpleDateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
		JSONObject json = new JSONObject(allData);
		String suc = json.getString("status");
		boolean success = "success".equals(suc);
		if (!success) {
			LOG.warn("Unable to get event data from churchtools");
			return eventId;
		}
		JSONObject data = json.getJSONObject("data");
		while (eventId == -1) {
			String nextGodi = format.format(time.getTime());
			for (String s : data.keySet()) {
				JSONObject obj = data.getJSONObject(s);
				if (obj.get("startdate").equals(nextGodi)) {
					eventId = obj.getInt("id");
					LOG.info("Found event on " + nextGodi + ": ");
					SimpleDateFormat prettyPrint = new SimpleDateFormat("MM.dd HH:mm");
					additionalInfos.put("Time", prettyPrint.format(time.getTime()));
					logAdditionalInfo(obj);
					break;
				}
			}
			if (eventId == -1) {
				time.add(GregorianCalendar.MINUTE, 15);
			}
		}
		return eventId;
	}

	private void logAdditionalInfo(JSONObject obj) {
		try {
			String bezeichnung = obj.getString("bezeichnung");
			additionalInfos.put("Event", bezeichnung);
			JSONArray services = obj.getJSONArray("services");
			for (int serviceID : serviceIDs.keySet()) {
				for (int i = 0; i < services.length(); i++) {
					JSONObject service = services.getJSONObject(i);
					if (serviceID == service.getInt("service_id")) {
						boolean valid = service.getInt("valid_yn") == 1;
						boolean zugesagt = service.getInt("zugesagt_yn") == 1;
						if (valid && zugesagt) {
							String[] besetzung = new String[2];
							String rolle = serviceIDs.get(serviceID);
							String name = service.getString("name");
							besetzung[0] = rolle;
							besetzung[1] = name;
							additionalInfos.put(rolle, name);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public void setLogin(String login) {
		if (!this.login.equals(login)) {
			this.login = login;
			if (isLoggedIn()) {
				FileIO.writeProperties(PREFERENCES_LOGIN_KEY, login);
			}
		}
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

	public String getUserName() {
		return login;
	}
}
