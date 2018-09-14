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

	private static final Logger			LOG			= Logger.getLogger(ChurchToolsAdapter.class);
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
		try {
			logIn(login, password);
			int eventId = getEventID();
		} catch (Exception e) {
			LOG.warn("Unable to load data");
			LOG.debug("", e);
		}
		return res;
	}

	private int getEventID() {

		return 0;
	}

	private boolean logIn(String user, String password) throws Exception {
		String paramsLogin = "email=" + URLEncoder.encode(user, "UTF-8") + "&" + "password="
		        + URLEncoder.encode(password, "UTF-8") + "&" + "directtool=" + URLEncoder.encode("yes", "UTF-8") + "&"
		        + "func=" + URLEncoder.encode("login", "UTF-8");
		String s = getData("POST", "login/ajax", paramsLogin).get(0);
		boolean success = s.toLowerCase().contains("successful");
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
		} catch (Exception e) {
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
			long time = e.getValue().getTimeInMillis();
			if (difference < 0) {
				difference = Math.abs(c.getTimeInMillis() - time);
				closest = e.getKey();
			} else {
				if (Math.abs(c.getTimeInMillis() - time) < difference) {
					closest = e.getKey();
				}
			}
		}
		return closest;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
