package com.mschwartz.teslacharging.tesla;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class TeslaConfiguration {

	private static final Logger logger = LogManager.getLogger(TeslaConfiguration.class);

	// Tesla API base URL
	static final String apiBase = "https://owner-api.teslamotors.com";

	// API call retry settings
	static final int MAX_RETRIES = 5;

	static final int RETRY_INTERVAL_SECONDS = 15;

	// Property file keys
	static final String ACCESS_TOKEN = "ACCESS_TOKEN", HOME_LATITUDE = "HOME_LATITUDE",
			HOME_LONGITUDE = "HOME_LONGITUDE", MAX_ELECTRICITY_PRICE = "MAX_ELECTRICITY_PRICE",
			MINIMUM_DEPARTURE_SOC = "MINIMUM_DEPARTURE_SOC", POLL_INTERVAL_SECONDS = "POLL_INTERVAL_SECONDS",
			REFRESH_TOKEN = "REFRESH_TOKEN", RESTART_ON_CURRENT_DROP = "RESTART_ON_CURRENT_DROP",
			SOC_GAIN_PER_HOUR = "SOC_GAIN_PER_HOUR", VIN = "VIN", ID_S = "ID_S", DISPLAY_NAME = "DISPLAY_NAME";

	private String propertiesFile = "app.properties";

	static long lastConfigurationModification = 0;

	private Properties prop;

	public TeslaConfiguration(String propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Store any updated configuration values in the properties file
	 * 
	 * @param configChanges HashMap of configuration keys and values to update
	 * @throws IOException
	 */
	private void updateConfiguration(HashMap<String, Object> configChanges) throws IOException {
		logger.debug("Update configuration with {}", new JSONObject(configChanges).toString());
		ArrayList<String> newLines = new ArrayList<String>();
		Scanner scanner;
		File file = new File(propertiesFile);
		if (file.exists()) {
			scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.indexOf("=") > 0) {
					String[] parts = line.split("=", 2);
					String key = parts[0];
					String value = parts[1];
					if (configChanges.containsKey(key)) {
						value = configChanges.get(key).toString();
						configChanges.remove(key);
					}
					line = key + "=" + value;
				}
				newLines.add(line);
			}
			scanner.close();
		}

		for (HashMap.Entry<String, Object> entry : configChanges.entrySet()) {
			Object v = entry.getValue();
			String value;
			if (v != null)
				value = v.toString();
			else 
				value = "";
			newLines.add(entry.getKey() + "=" + value);
		}

		if (newLines.size() > 0) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile));
			for (String line : newLines) {
				writer.write(line + "\n");
			}
			writer.close();

			File config = new File(propertiesFile);
			lastConfigurationModification = config.lastModified();
		}
	}

	public void updateTokens(String accessToken, String refreshToken) throws IOException {
		HashMap<String, Object> newTokens = new HashMap<String, Object>();
		newTokens.put(ACCESS_TOKEN, accessToken);
		newTokens.put(REFRESH_TOKEN, refreshToken);
		updateConfiguration(newTokens);
	}

	public void updateVin(String vin, String id_s, String displayName) throws IOException {
		HashMap<String, Object> newTokens = new HashMap<String, Object>();
		newTokens.put(VIN, vin);
		newTokens.put(ID_S, id_s);
		newTokens.put(DISPLAY_NAME, displayName);
		updateConfiguration(newTokens);
	}

	public void updateHomeLocation(Double latitude, Double longitude) throws IOException {
		HashMap<String, Object> newTokens = new HashMap<String, Object>();
		newTokens.put(HOME_LATITUDE, latitude);
		newTokens.put(HOME_LONGITUDE, longitude);
		updateConfiguration(newTokens);
	}

	private void openPropertiesFile() {
		if (prop != null)
			return;

		File config = new File(propertiesFile);

		boolean configurationUpdated = false;
		if (lastConfigurationModification > 0 && config.lastModified() > lastConfigurationModification) {
			configurationUpdated = true;
			logger.info("Configuration has changed");
		} else if (lastConfigurationModification > 0) {
			return;
		}
		lastConfigurationModification = config.lastModified();

		prop = new Properties();
		try (FileInputStream fis = new FileInputStream(propertiesFile)) {
			prop.load(fis);
		} catch (FileNotFoundException ex) {
			logger.fatal(propertiesFile + " not found in current directory.");
			throw new RuntimeException();
		} catch (IOException ex) {
			logger.fatal(propertiesFile + " could not be read, Exiting.");
			System.exit(1);
		}

	}

	public String readAccessToken() {
		openPropertiesFile();
		return prop.getProperty(ACCESS_TOKEN);
	}

	public String readRefreshToken() {
		openPropertiesFile();
		return prop.getProperty(REFRESH_TOKEN);
	}

	public String readVin() {
		openPropertiesFile();
		return prop.getProperty(VIN).isEmpty() ? null : prop.getProperty(VIN);
	}

	public String readID_S() {
		openPropertiesFile();
		return prop.getProperty(ID_S).isEmpty() ? null : prop.getProperty(ID_S);
	}

	public String readDisplayName() {
		openPropertiesFile();
		return prop.getProperty(DISPLAY_NAME).isEmpty() ? null : prop.getProperty(DISPLAY_NAME);
	}

	public Double readHomeLatitude() {
		openPropertiesFile();
		return Double.parseDouble(prop.getProperty(HOME_LATITUDE));
	}

	public Double readHomeLongitude() {
		openPropertiesFile();
		return Double.parseDouble(prop.getProperty(HOME_LONGITUDE));
	}

	/**
	 * Load configuration into the variables we use. Also handles reloading
	 * configuration when the properties file is changed.
	 */
	public static void loadConfiguration() {

		// Departure charge settings
		boolean departureChargeUpdated = false;
//		try {
//			int newMinimumDepartureSoC = Integer.parseInt(prop.getProperty(MINIMUM_DEPARTURE_SOC));
//			if (configurationUpdated && newMinimumDepartureSoC != minimumDepartureSoC) {
//				log("New minimum departure SoC: " + newMinimumDepartureSoC + "%");
//				departureChargeUpdated = true;
//			}
//			minimumDepartureSoC = newMinimumDepartureSoC;
//		} catch (Exception ex) {
//			minimumDepartureSoC = 0;
//		}
//
//		try {
//			double newSoCGainPerHour = Double.parseDouble(prop.getProperty(SOC_GAIN_PER_HOUR));
//			if (configurationUpdated && newSoCGainPerHour != soCGainPerHour) {
//				log("New SoC gain per hour: " + newSoCGainPerHour + "%");
//				departureChargeUpdated = true;
//			}
//			soCGainPerHour = newSoCGainPerHour;
//		} catch (Exception ex) {
//			soCGainPerHour = 0;
//			minimumDepartureSoC = 0;
//		}
//
//		shouldChargeForDepature = (minimumDepartureSoC > 0 && soCGainPerHour > 0);
//		if (configurationUpdated && departureChargeUpdated) {
//			log("Vehicle will" + (!shouldChargeForDepature ? " not" : "") + " be charged to reach minimum departure SoC"
//					+ (minimumDepartureSoC > 0 ? " of " + minimumDepartureSoC + "%" : "") + ".");
//		}
//
//		// Restart on current drop setting
//		boolean newRestartOnCurrentDrop = false;
//		String restartOnCurrentDropSetting = prop.getProperty(RESTART_ON_CURRENT_DROP);
//		if (restartOnCurrentDropSetting != null && restartOnCurrentDropSetting.toLowerCase().equals("y")) {
//			newRestartOnCurrentDrop = true;
//		}
//
//		if (configurationUpdated && newRestartOnCurrentDrop != restartOnCurrentDrop) {
//			log("New restart on current drop flag: " + (newRestartOnCurrentDrop ? "Y" : "N"));
//		}
//		restartOnCurrentDrop = newRestartOnCurrentDrop;
	}

}
