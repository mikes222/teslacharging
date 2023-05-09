package com.mschwartz.teslacharging.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class RestRequest {

	private static final Logger logger = LogManager.getLogger(RestRequest.class);

	private WebRequest webRequest = new WebRequest();

	public String get(String getURL, Map<String, String> headers) throws Exception {
		return webRequest.get(getURL, headers);
	}

	public String post(String postURL, String body, Map<String, String> headers) throws Exception {
		return webRequest.post(postURL, body, headers);
	}

	public void setBearer(String b) {
		webRequest.setBearer(b);
	}

	/**
	 * Get a JSONObject representing the content retrieved from the REST endpoint
	 * 
	 * @param method Method to call at endpoint (assumed to be whatever comes after
	 *               $BASEURL/rest/api/2/)
	 * @return JSONObject from retrieved content. Note that if an array is
	 *         retrieved, a JSONObject of the format {d: content} will be returned.
	 */
	public <T extends Object> T getJSON(String getURL, Class<T> clazz) throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json, text/plain, */*");
		headers.put("Content-Type", "application/json");
		Gson gson = new Gson();
		String jsonString = webRequest.get(getURL, null);
		return gson.fromJson(jsonString, clazz);
	}

	/**
	 * Convenience method to return JSONObject from GET request.
	 * 
	 * @see get
	 * @param getUrl URL to request
	 * @return JSONObject from reponse. Null if failure in request or non-JSON
	 *         response received.
	 */
	public JSONObject getJSON(String getUrl, Map<String, String> properties) throws Exception {
		String responseText = webRequest.get(getUrl, properties);
		JSONObject responseJSON = new JSONObject(responseText);

		return responseJSON;
	}

	/**
	 * Convert response data string to JSONObject
	 * 
	 * @param json Response data string (assumed to be JSON)
	 * @return JSONObject or null if string could not be parsed to JSON.
	 */
	private JSONObject processResponseData(String json) {
		if (json != null && (json.startsWith("{") || json.startsWith("["))) {
			try {
				if (json.startsWith("[")) {
					return new JSONObject("{d:" + json + "}");
				}
				return new JSONObject(json);
			} catch (JSONException e) {
				logger.error("Exception while parsing REST JSON response: {}", e);
				throw e;
			}
		}

		return null;
	}

	public <T extends Object> T postJson(String postURL, Object body, Class<T> clazz) throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json, text/plain, */*");
		headers.put("Content-Type", "application/json");
		Gson gson = new Gson();
		String jsonString = webRequest.post(postURL, gson.toJson(body), headers);
		return gson.fromJson(jsonString, clazz);
	}
}
