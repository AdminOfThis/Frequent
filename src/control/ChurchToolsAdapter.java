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

import javax.net.ssl.HttpsURLConnection;


public class ChurchToolsAdapter {


	public ChurchToolsAdapter() {


		// getNextSunday();
		String data = null;
		try {
			data = checkCT().get(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int startIndex = data.indexOf(nextSunday(new GregorianCalendar()));
		GregorianCalendar c = new GregorianCalendar();
		c.add(GregorianCalendar.DAY_OF_YEAR, 7);
		int endIndex = data.indexOf(nextSunday(c));
		System.out.println(data.substring(startIndex, endIndex + 1));
	}

	private static ArrayList<String> checkCT(String mail, String password) throws Exception {
		// login

		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		String paramsLogin = "email=" + URLEncoder.encode(mail , "UTF-8") + "&" + "password="
			+ URLEncoder.encode(password, "UTF-8") + "&" + "directtool=" + URLEncoder.encode("yes", "UTF-8") + "&" + "func="
			+ URLEncoder.encode("login", "UTF-8");
		getData("POST", "login/ajax", paramsLogin);

		System.out.println("POST REQUEST: \r\n");

		return getData("GET", "churchcal/ajax", "func=" + URLEncoder.encode("getMyServices", "UTF-8"));
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

	private static String nextSunday(GregorianCalendar c) {
		while (c.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.SUNDAY) {
			c.add(GregorianCalendar.DAY_OF_YEAR, 1);
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(c.getTime());
	}

}
