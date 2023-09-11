package com.mschwartz.teslacharging.tesla;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.web.RestRequest;

import lombok.Getter;

public class TeslaAuth {

	private static final Logger logger = LogManager.getLogger(TeslaAuth.class);

	private String accessToken;

	private String refreshToken;

	private RestRequest restRequest;

	private TeslaConfiguration teslaConfiguration;

	public TeslaAuth(RestRequest restRequest, TeslaConfiguration teslaConfiguration) {
		this.restRequest = restRequest;
		this.teslaConfiguration = teslaConfiguration;
		accessToken = teslaConfiguration.readAccessToken();
		refreshToken = teslaConfiguration.readRefreshToken();
		if (accessToken == null || refreshToken == null) {
			logger.fatal("Tesla API access token and/or refresh token missing.");
			throw new RuntimeException();
		}
		restRequest.setBearer(accessToken);
	}

	/**
	 * Refresh the access and refresh tokens for Tesla API access
	 * 
	 * @throws Exception
	 */
	public void refreshTokens() throws Exception {
		assert (refreshToken != null);
		RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(refreshToken);

		RefreshTokenResponse response = restRequest.postJson("https://auth.tesla.com/oauth2/v3/token",
				refreshTokenRequest, RefreshTokenResponse.class);

		if (response.getRefresh_token() != null & response.getAccess_token() != null) {
			accessToken = response.getAccess_token();
			refreshToken = response.getRefresh_token();
			restRequest.setBearer(accessToken);

			teslaConfiguration.updateTokens(accessToken, refreshToken);
		} else {
			logger.error("Exception while handling new access token for Tesla Owner API: Null new access token");
		}

	}

	/////////////////////////////////////////////////////////////////////////

	public class RefreshTokenRequest {
		public String grant_type = "refresh_token";

		public String client_id = "ownerapi";

		public String refresh_token;

		public String scope = "openid email offline_access";

		RefreshTokenRequest(String refresh_token) {
			this.refresh_token = refresh_token;
		}
	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	public class RefreshTokenResponse {
		public String access_token;

		public String refresh_token;

		public String expires_in;

		public String state;

		public String token_type;

		public String id_token;

		@Override
		public String toString() {
			return "RefreshTokenResponse [access_token=" + access_token + ", refresh_token=" + refresh_token
					+ ", expires_in=" + expires_in + ", state=" + state + ", token_type=" + token_type + "]";
		}

	}
}