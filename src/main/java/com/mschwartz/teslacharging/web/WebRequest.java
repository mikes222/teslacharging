package com.mschwartz.teslacharging.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebRequest {

	protected static CookieManager cookieManager = new CookieManager();

	protected String bearer;

	private static final Logger logger = LogManager.getLogger(WebRequest.class);

	public WebRequest() {
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);
	}

	/**
	 * Set the cookies to be used for requests
	 * 
	 * @param cookies list of Cookies
	 */
//	public void setCookies(ArrayList<Cookie> cookies) {
//		this.cookies = cookies;
//	}

	/**
	 * Set the bearer token to be used for requests
	 * 
	 * @param b Bearer token string
	 */
	public void setBearer(String b) {
		this.bearer = b;
	}

	private void enrichConnection(HttpURLConnection conn, Map<String, String> headers) {
		conn.setReadTimeout(30000);
		conn.setConnectTimeout(15000);
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		if (bearer != null) {
			conn.setRequestProperty("Authorization", "Bearer " + bearer);
		}
		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			conn.setRequestProperty("Cookie", join("; ", cookieManager.getCookieStore().getCookies()));
		}

//		Map<String, List<String>> headerFields = conn.getRequestProperties();
//		for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//			for (String value : entry.getValue()) {
//				logger.info("--> " + entry.getKey() + ": " + value);
//			}
//		}

	}

	private static String join(String separator, List<HttpCookie> input) {

		if (input == null || input.size() <= 0)
			return "";

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < input.size(); i++) {

			sb.append(input.get(i));

			// if not the last item
			if (i != input.size() - 1) {
				sb.append(separator);
			}

		}

		return sb.toString();

	}

	/**
	 * Perform a GET request on a provided URL
	 * 
	 * @param getURL URL to request
	 * @return String of content retrieved from the URL
	 */
	public String get(String getURL, Map<String, String> headers) throws Exception {
		URL url;

		StringWriter returnData = new StringWriter();

		HttpURLConnection conn = null;
		InputStreamReader ir = null;

		logger.debug("GET request to: {}", getURL);

		try {
			url = new URL(getURL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("User-Agent", "TeslaCharging");
//			conn.setRequestProperty("Accept", "application/json, text/plain, */*");
//			conn.setRequestProperty("x-tesla-user-agent", "");
//			conn.setRequestProperty("X-Requested-With", "com.teslamotors.tesla");
			enrichConnection(conn, headers);

			conn.connect();

//			Map<String, List<String>> headerFields = conn.getHeaderFields();
//			for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//				for (String value : entry.getValue()) {
//					logger.info("<-- " + entry.getKey() + ": " + value);
//				}
//			}

			try {
				ir = new InputStreamReader(conn.getInputStream(), "UTF-8");
			} catch (IOException e) {
				if (e.getMessage().contains("401"))
					throw new AuthenticationException(e);
				else if (e.getMessage().contains("408"))
					// seems the tesla is sleeping
					throw new SleepingCarException(e);
				throw e;
			}

			if (ir != null) {
				char[] buffer = new char[1024 * 4];
				int len = 0;
				while ((len = ir.read(buffer)) != -1) {
					returnData.write(buffer, 0, len);
				}
			}

			processHeaderCookies(conn);

		} finally {
			if (ir != null) {
				try {
					ir.close();
				} catch (IOException e) {
				}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		returnData.flush();
		String returnText = returnData.toString();

		returnData.close();
		logger.debug(returnText);

		return returnText;
	}

	/**
	 * Convenience method to perform a POST that does not require a body.
	 * 
	 * @see post(String, String)
	 * @param postURL URL for POST
	 * @return Result of POST
	 * @throws Exception When things go wrong
	 */
	public String post(String postURL) throws Exception {
		return post(postURL, null, null);
	}

	/**
	 * Perform a POST with a body
	 * 
	 * @param postURL URL for POST
	 * @param body    String for POST body
	 * @return Result of POST
	 * @throws Exception When things go wrong
	 */
	public String post(String postURL, String body, Map<String, String> headers) throws Exception {
		URL url;

		StringWriter returnData = new StringWriter();

		HttpURLConnection conn = null;
		InputStreamReader ir = null;

		logger.debug("POST request to: {}", postURL);
		if (body != null) {
			logger.debug("POST body: {}", body);
		}

		try {
			url = new URL(postURL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			HttpURLConnection.setFollowRedirects(false);
//			conn.setRequestProperty("Accept", "application/json, text/plain, */*");
			conn.setRequestProperty("Accept", "*/*");
//			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			if (body != null && body.length() > 0) {
				conn.setRequestProperty("Content-Length", String.valueOf(body.length()));
			}
			conn.setRequestProperty("User-Agent", "TeslaCharging");
//			conn.setRequestProperty("x-tesla-user-agent", "");
//			conn.setRequestProperty("X-Requested-With", "com.teslamotors.tesla");

			enrichConnection(conn, headers);

			conn.connect();

			if (body != null && body.length() > 0) {
				OutputStreamWriter out = null;
				try {
					out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
					out.write(body);
					out.flush();
				} finally {
					if (out != null) {
						out.close();
					}
				}
			}

//			Map<String, List<String>> headerFields = conn.getHeaderFields();
//			for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//				for (String value : entry.getValue()) {
//					logger.info("<-- " + entry.getKey() + ": " + value);
//				}
//			}

			try {
				ir = new InputStreamReader(conn.getInputStream(), "UTF-8");
			} catch (IOException e) {
				if (e.getMessage().contains("401"))
					throw new AuthenticationException(e);
				else if (e.getMessage().contains("408"))
					// seems the tesla is sleeping
					throw new SleepingCarException(e);
				throw e;
			}

			if (ir != null) {
				char[] buffer = new char[1024 * 4];
				int len = 0;
				while ((len = ir.read(buffer)) != -1) {
					returnData.write(buffer, 0, len);
				}
			}

			processHeaderCookies(conn);

		} finally {
			if (ir != null) {
				ir.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		returnData.flush();
		String returnText = returnData.toString();

		returnData.close();
		logger.debug(returnText);

		return returnText;
	}

	private void processHeaderCookies(HttpURLConnection conn) {
//		Map<String, List<String>> headerFields = conn.getHeaderFields();
//		List<String> headerCookies = headerFields.get("Set-Cookie");
//		if (headerCookies != null) {
//			for (String cookie : headerCookies) {
//				if (cookie.length() == 0)
//					continue;
//				List<HttpCookie> cookies = HttpCookie.parse(cookie);
//				for (HttpCookie c : cookies) {
//					cookieManager.getCookieStore().add(null, c);
//					logger.info("Adding cookie " + c);
//				}
//			}
//		}
	}
}
