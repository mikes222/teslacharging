package com.mschwartz.teslacharging.web;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.tesla.TeslaAuth;

public class AuthRestRequest {

	static final Logger logger = LogManager.getLogger(AuthRestRequest.class);

	private RestRequest restRequest;

	private TeslaAuth teslaAuth;

	public AuthRestRequest(RestRequest restRequest, TeslaAuth teslaAuth) {
		this.restRequest = restRequest;
		this.teslaAuth = teslaAuth;
	}

	public String get(String getURL, Map<String, String> headers) throws Exception {
		return restRequest.get(getURL, headers);
	}

	public String post(String postURL, String body, Map<String, String> headers) throws Exception {
		return restRequest.post(postURL, body, headers);
	}

	public void setBearer(String b) {
		restRequest.setBearer(b);
	}

	public <T extends Object> T getJSON(String getURL, Class<T> clazz) throws Exception {
		try {
			return restRequest.getJSON(getURL, clazz);
		} catch (AuthenticationException e) {
			teslaAuth.refreshTokens();
			return restRequest.getJSON(getURL, clazz);
		}
	}

	public <T extends Object> T postJson(String postURL, Object body, Class<T> clazz) throws Exception {
		try {
			return restRequest.postJson(postURL, body, clazz);
		} catch (AuthenticationException e) {
			teslaAuth.refreshTokens();
			return restRequest.postJson(postURL, body, clazz);
		}
	}

}
