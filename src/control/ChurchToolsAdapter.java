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

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import data.Cue;

public class ChurchToolsAdapter {

	private static final Logger			LOG				= Logger.getLogger(ChurchToolsAdapter.class);
	private static final String			SPLIT2			= "\"id\":";
	private static final String			AGENDA_SPLIT	= "\"bezeichnung\":\"Song\"";
	private static final String			DATE_FORMAT		= "yyyy-MM-dd";
	private static final int[]			SERVICES		= new int[] { 9, 11, 16 };
	private transient String			login			= "";
	private transient String			password		= "";
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
		try {
			logIn(login, password);
			int eventId = loadEventID(sunday);
			LOG.info("Found Event-ID: " + eventId);
			int agendaId = loadAgendaId(eventId);
			LOG.info("Found Agenda-ID: " + agendaId);
			ArrayList<Integer> songIds = loadSongIDs(agendaId);
			System.out.println(songIds);
			for (int i : songIds) {
				loadSongName(i);
			}
		}
		catch (Exception e) {
			LOG.warn("Unable to load data");
			LOG.debug("", e);
		}
		return res;
	}

	private void loadSongName(int songId) throws Exception {
		String allData = getData("GET", "churchser" + "vice/ajax", "func=" + URLEncoder.encode("getAllSongs", "UTF-8")).get(0);
// System.out.println(allData);
	}

	private ArrayList<Integer> loadSongIDs(int agendaId) throws Exception {
		ArrayList<Integer> res = new ArrayList<>();
		String allData = getData("GET", "churchser" + "vice/ajax", "func=" + URLEncoder.encode("loadAgendaItems", "UTF-8") + "&" + "agenda_id=" + URLEncoder.encode(agendaId + "", "UTF-8")).get(0);
		System.out.println(allData);
		int beginIndex = allData.substring(0, allData.indexOf(AGENDA_SPLIT)).lastIndexOf(SPLIT2);
		String searchString = allData.substring(beginIndex);
		int endIndex = searchString.lastIndexOf(AGENDA_SPLIT);
		searchString = searchString.substring(0, endIndex);
		String[] songs = searchString.split(SPLIT2);
		for (String s : songs) {
			if (s.contains("Song")) {
				String sub = s.substring(s.indexOf("\"") + 1);
				sub = sub.substring(0, sub.indexOf("\",\""));
				int songId = Integer.parseInt(sub);
				res.add(songId);
			}
		}
		return res;
	}

	private int loadAgendaId(int eventId) throws Exception {
		String allData = getData("GET", "churchservice/ajax", "func=" + URLEncoder.encode("loadAgendaForEvent", "UTF-8") + "&" + "event_id=" + URLEncoder.encode(eventId + "", "UTF-8")).get(0);
		int beginIndex = allData.indexOf(SPLIT2);
		String searchString = allData.substring(beginIndex + SPLIT2.length() + 1);
		int endIndex = searchString.indexOf("\"");
		searchString = searchString.substring(0, endIndex);
		int agendaId = Integer.parseInt(searchString);
		return agendaId;
	}

	private int loadEventID(String nexGoDi) throws Exception {
		String allData = getData("GET", "churchservice/ajax", "func=" + URLEncoder.encode("getAllEventData", "UTF-8")).get(0);
		int sundayBeginIndex = allData.indexOf(nexGoDi);
		String searchString = allData.substring(0, sundayBeginIndex);
		int lastId = searchString.lastIndexOf(SPLIT2);
		searchString = searchString.substring(lastId + SPLIT2.length() + 1, searchString.length());
		searchString = searchString.substring(0, searchString.indexOf("\""));
		int eventID = Integer.parseInt(searchString);
		return eventID;
	}

	private boolean logIn(String user, String password) throws Exception {
		String paramsLogin = "email=" + URLEncoder.encode(user, "UTF-8") + "&" + "password=" + URLEncoder.encode(password, "UTF-8") + "&" + "directtool=" + URLEncoder.encode("yes", "UTF-8") + "&"
			+ "func=" + URLEncoder.encode("login", "UTF-8");
		String s = getData("POST", "login/ajax", paramsLogin).get(0);
		boolean success = s.toLowerCase().contains("success");
		if (success) {
			LOG.info("Logged in to Churchtools");
		}
		return success;
	}

	private static ArrayList<String> getData(String method, String request, String params) {
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	private String getNextSundayString() {
		return getNextSundayString(new GregorianCalendar());
	}

	private String getNextSundayString(GregorianCalendar c) {
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
			}
			catch (Exception e) {
				LOG.warn("Problem checking Login");
				return false;
			}
		}
	}
}
