package com.mschwartz.teslacharging.tesla;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

import com.mschwartz.teslacharging.web.RestRequest;

import lombok.Getter;
import lombok.Setter;

public class TeslaCreateAuth {

	private static final Logger logger = LogManager.getLogger(TeslaCreateAuth.class);

	private final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

	@Getter
	@Setter
	private String codeVerifier;

	private String codeChallenge;

	private String state;

	private RestRequest restRequest;

	public TeslaCreateAuth(RestRequest restRequest) {
		this.restRequest = restRequest;
	}

	private String createRandomString(int length) {
		String result = "";
		for (int i = 0; i < length; ++i) {
			char ch = chars.charAt((int) (Math.random() * chars.length()));
			result += ch;
		}
		return result;
	}

	public void createVerifier() throws NoSuchAlgorithmException {
		codeVerifier = createRandomString(86);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedhash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));

		codeChallenge = Base64.getEncoder().encodeToString(encodedhash);
		/// url-encoding the base64 string. It is intentional to remove the "=" chars
		codeChallenge = codeChallenge.replaceAll("\\+", "-").replaceAll("/", "_").replaceAll("=", "");

		state = createRandomString(10);

		// logger.info("Codeverifier is " + codeVerifier + ", codeChallenge is " +
		// codeChallenge + ", state is " + state);
	}

	public String prepareAuthUrl(String email) throws UnsupportedEncodingException {
		String query = "";
		query += "client_id=ownerapi";
		query += "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.toString());
		query += "&code_challenge_method=S256";
		query += "&redirect_uri="
				// +"https://auth.tesla.com/void/calllback";
				+ URLEncoder.encode("https://auth.tesla.com/void/callback", StandardCharsets.UTF_8.toString());
		query += "&response_type=code";
		query += "&scope=" + URLEncoder.encode("openid email offline_access", StandardCharsets.UTF_8.toString());
		query += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.toString());
		if (email != null)
			query += "&login_hint=" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
		return "https://auth.tesla.com/oauth2/v3/authorize?" + query;
	}

	public Map<String, String> authorize(String email) throws Exception {
		String url = prepareAuthUrl(email);
		String result = restRequest.get(url, null);
		// logger.info("authorize returned " + result);

		Map<String, String> res = new HashMap<String, String>();
		int idx = 0;
		while (true) {
			idx = result.indexOf("<input type=\"hidden\" ", idx);
			if (idx < 0)
				break;
			int idx2 = result.indexOf("/>", idx + 6);
			if (idx2 > idx) {
				String field = result.substring(idx + 21, idx2 - 1);
				// logger.info("found " + idx + " and " + idx2 + ": " + field);
				String[] fields = field.split(" ");
				res.put(fields[0].substring(6, fields[0].length() - 1), fields[1].substring(7, fields[1].length() - 1));
			}
			++idx;
		}
		return res;
	}

	public void postAuthorize(String email, String password, Map<String, String> formParams) throws Exception {
		String query = "";
		query += "client_id=ownerapi";
		query += "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.toString());
		query += "&code_challenge_method=S256";
		query += "&redirect_uri="
				// +"https://auth.tesla.com/void/calllback";
				+ URLEncoder.encode("https://auth.tesla.com/void/callback", StandardCharsets.UTF_8.toString());
		query += "&response_type=code";
		query += "&scope=" + URLEncoder.encode("openid email offline_access", StandardCharsets.UTF_8.toString());
		query += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.toString());

		String body = "";
		body += "identity=" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
		// body += "&credential=" + URLEncoder.encode(password,
		// StandardCharsets.UTF_8.toString());
		for (Map.Entry<String, String> entry : formParams.entrySet()) {
			body += "&" + URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()) + "="
					+ URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
		}
		// example from Firefox when executing the get-request from the previous method:
		// _csrf=0ERKbSv6-bypXXrIvUPk5CU463Cb7hvIX3XQ&_phase=identity&cancel=&transaction_id=mDjbjsCV&correlation_id=08d8c750-ffef-4366-b381-8e33cc7668c5&identity=tesla%40mschwartz.eu

		// Post2: POST
		// https://auth.tesla.com/oauth2/v3/authorize?client_id=ownerapi&code_challenge=4FKodQTbC6nf1/krK4tRL87Ztqb+fzqd86FpabyTLvQ=&code_challenge_method=S256&redirect_uri=https://auth.tesla.com/void/callback&response_type=code&scope=openid
		// email offline_access&state=YWhia1ZQRlVFZ0VtYzJMbw==
		// Result:
		// _csrf=dbWPcU0k-i0CTX63ewFf3KrHWh6M8gh1C2Z0&_phase=authenticate&_process=1&cancel=&transaction_id=mDjbjsCV&change_identity=&identity=tesla%40mschwartz.eu&correlation_id=08d8c750-ffef-4366-b381-8e33cc7668c5&fingerPrint=%7B%22auth_method%22%3A%22email-login%22%2C%22devicehash%22%3A%22ac638e0a5ba53745eab48b8b556ddd18%22%2C%22client_id%22%3A%22ownerapi%22%2C%22hardware_concurrency%22%3A8%2C%22screen_resolution%22%3A%5B2560%2C1440%5D%2C%22audio%22%3A35.7383295930922%2C%22touch_support%22%3A%226690a7caa6588891494df1e64b3d185b%22%2C%22web_gl%22%3A%22WebGL+1.0%22%2C%22browser_plugins%22%3A%2273ddd9a85fc01dd86982e0a967643420%22%2C%22browser_canvas%22%3A%225d895febb9694d1dd681519a3b5bc80d%22%2C%22browser_font%22%3A%22bb0fb9d455dd87dd8df801bb296959a8%22%7D&credential=pIEbGg9x73wuDOZCAVqn
		logger.info("POST https://auth.tesla.com/oauth2/v3/authorize?" + query);
		logger.info(body);

		String result = restRequest.post("https://auth.tesla.com/oauth2/v3/authorize?" + query, body, null);
		logger.info(result);
	}

	/**
	 * Retrieves the token
	 * 
	 * @param code
	 * @return
	 * @throws JSONException
	 * @throws Exception
	 */
	public TokenResponse retrieveTokens(String code) throws JSONException, Exception {
		TokenRequest tokenRequest = new TokenRequest(code, codeVerifier);

		TokenResponse response = restRequest.postJson("https://auth.tesla.com/oauth2/v3/token", tokenRequest,
				TokenResponse.class);

//		logger.info(response.toString());
		return response;
	}

	/////////////////////////////////////////////////////////////////////////

	public class TokenRequest {
		public String grant_type = "authorization_code";

		public String client_id = "ownerapi";

		public String code;

		public String code_verifier;

		public String redirect_uri = "https://auth.tesla.com/void/callback";

		TokenRequest(String code, String code_verifier) {
			this.code = code;
			this.code_verifier = code_verifier;
		}
	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	public class TokenResponse {
		public String access_token;

		public String refresh_token;

		public String expires_in;

		public String state;

		public String token_type;

		public String id_token;

		@Override
		public String toString() {
			return "TokenResponse [access_token=" + access_token + ", refresh_token=" + refresh_token + ", expires_in="
					+ expires_in + ", state=" + state + ", token_type=" + token_type + "]";
		}

	}
}
