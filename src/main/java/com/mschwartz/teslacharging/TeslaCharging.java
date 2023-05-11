/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mschwartz.teslacharging;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.mschwartz.teslacharging.tesla.TeslaAuth;
import com.mschwartz.teslacharging.tesla.TeslaAuth.TokenResponse;
import com.mschwartz.teslacharging.tesla.TeslaCharge;
import com.mschwartz.teslacharging.tesla.TeslaConfiguration;
import com.mschwartz.teslacharging.tesla.TeslaVehicle;
import com.mschwartz.teslacharging.tesla.TeslaVehicle.ChargeState;
import com.mschwartz.teslacharging.tesla.TeslaVehicle.DriveState;
import com.mschwartz.teslacharging.web.AuthRestRequest;
import com.mschwartz.teslacharging.web.RestRequest;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.choice.RangeArgumentChoice;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class TeslaCharging {

	static final Logger logger = LogManager.getLogger(TeslaCharging.class);

	public static void main(String[] args) throws Exception {
		// System.exit is common for Batch applications since the exit code can be used
		// to
		// drive a workflow
		new TeslaCharging(args);
		// System.exit(SpringApplication.exit(SpringApplication.run(TeslaCharging.class,
		// args)));
	}

	public TeslaCharging(String[] args) throws Exception {

		ArgumentParser parser = ArgumentParsers.newFor("TeslaCharging").build().defaultHelp(true)
				.description("Controls your Tesla");
		parser.addArgument("-a", "--auth").choices("step1", "step2").help("Starts Authentication step 1 or 2");
		parser.addArgument("-c", "--code").type(String.class).help("Authorization code from --auth1");
		parser.addArgument("-v", "--verifier").type(String.class).help("CodeVerifier from --auth1");
		parser.addArgument("-r", "--charge").choices("start", "stop").type(String.class).help("Start or stop charging");
		parser.addArgument("-p", "--chargeport").choices("open", "close").type(String.class)
				.help("Opens or closes the charge port door");
		parser.addArgument("-l", "--chargelimit").type(Integer.class).choices(new RangeArgumentChoice<Integer>(30, 100))
				.help("Sets the charging limit");
		parser.addArgument("-m", "--chargingamps").type(Integer.class).choices(new RangeArgumentChoice<Integer>(3, 32))
				.help("Sets the charging amps");
		parser.addArgument("-s", "--chargestate").choices("get").type(String.class).help("Gets the charge state");
		parser.addArgument("-g", "--chargecalculation").type(Integer.class)
				.help("Calculates the charging amps based on the given power surplus in watts");
		parser.addArgument("-f", "--propertyfile").type(String.class)
				.help("name and location of the propertyfile. Default is app.properties");
		parser.addArgument("-w", "--wakeup").type(String.class).help("Wakeup a sleeping tesla");

		Namespace ns = null;
		try {
			ns = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		RestRequest restRequest = new RestRequest();
		TeslaConfiguration teslaConfiguration = new TeslaConfiguration(
				ns.getString("propertyfile") != null ? ns.getString("propertyfile") : "app.properties");
		TeslaAuth teslaAuth = new TeslaAuth(restRequest, teslaConfiguration);
		AuthRestRequest authRestRequest = new AuthRestRequest(restRequest, teslaAuth);

		TeslaVehicle teslaVehicle = new TeslaVehicle(authRestRequest, teslaConfiguration);
		TeslaCharge teslaCharge = new TeslaCharge(authRestRequest, teslaVehicle, teslaConfiguration);

		if (ns.getString("auth") != null && ns.getString("auth").equalsIgnoreCase("step1")) {
			teslaAuth.createVerifier();
			String url = teslaAuth.prepareAuthUrl(null);

			System.out.println(
					"Copy the following URL to your browser, and enter your tesla credentials. When the browser responds with 'Page Not Found' copy the parameter code from the URL and start step 2 of the authentication process\n");
			System.out.println(url);
			System.out.println("\n" + "java -jar teslacharging.jar --auth step2 --verifier "
					+ teslaAuth.getCodeVerifier() + " --code <code>");
			return;
		} else if (ns.getString("auth") != null && ns.getString("auth").equalsIgnoreCase("step2")) {
			teslaAuth.setCodeVerifier(ns.getString("verifier"));
			TokenResponse response = teslaAuth.retrieveTokens(ns.getString("code"));
			teslaConfiguration.updateTokens(response.access_token, response.refresh_token);
			System.out.println("Congratulations. The tokens are now stored for future use");
		} else if (ns.getString("auth") != null && ns.getString("auth").equalsIgnoreCase("all")) {
			String email = "tesla@mschwartz.eu";

			teslaAuth.createVerifier();
			Map<String, String> params = teslaAuth.authorize(email);
			for (Map.Entry<String, String> entry : params.entrySet()) {
				System.out.println("val: " + entry.getKey() + " = " + entry.getValue());
			}
			teslaAuth.postAuthorize(email, "pIEbGg9x73wuDOZCAVqn", params);
		} else if (ns.getString("charge") != null && ns.getString("charge").equalsIgnoreCase("start")) {
			String result = teslaCharge.startCharging();
			if (result != null) {
				System.out.println("Start charging failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Start charging successful");
			}
		} else if (ns.getString("charge") != null && ns.getString("charge").equalsIgnoreCase("stop")) {
			String result = teslaCharge.stopCharging();
			if (result != null) {
				System.out.println("Stop charging failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Stop charging successful");
			}
		} else if (ns.getString("chargeport") != null && ns.getString("chargeport").equalsIgnoreCase("open")) {
			String result = teslaCharge.chargePortOpen();
			if (result != null) {
				System.out.println("Charge port door open failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Charge port door open successful");
			}
		} else if (ns.getString("chargeport") != null && ns.getString("chargeport").equalsIgnoreCase("close")) {
			String result = teslaCharge.chargePortClose();
			if (result != null) {
				System.out.println("Charge port door close failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Charge port door close successful");
			}
		} else if (ns.getString("chargelimit") != null) {
			String result = teslaCharge.setChargeLimit(ns.getInt("chargelimit"));
			if (result != null) {
				System.out
						.println("Setting charge limit to " + ns.getInt("chargelimit") + " failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Setting charge limit successful");
			}
		} else if (ns.getString("chargingamps") != null) {
			String result = teslaCharge.setChargingAmps(ns.getInt("chargingamps"));
			if (result != null) {
				System.out.println(
						"Setting charging amps to " + ns.getInt("chargingamps") + " failed. Reason: " + result);
				System.exit(1);
			} else {
				System.out.println("Setting charging amps successful");
			}
		} else if (ns.getString("chargestate") != null && ns.getString("chargestate").equalsIgnoreCase("get")) {
			ChargeState result = teslaVehicle.getVehicleChargeState();
			if (result != null) {
				System.out.println(result);
			} else {
				System.out.println("getting charge state failed");
				System.exit(1);
			}
		} else if (ns.getString("chargecalculation") != null) {
			
			DriveState driveState = teslaVehicle.getVehicleDriveState();
			if (driveState != null && driveState.getLatitude() != null
					&& driveState.getLongitude() != null) {
				double distance = teslaVehicle.getHomePosition().distanceFrom(driveState.getLatitude(), driveState.getLongitude());
				if (distance > 5) {
					System.out.println("Car is more than 5 miles away from home.");
					System.exit(1);
				}
			} else {
				System.out.println("getting drive state failed");
				System.exit(1);
			}
		
			ChargeState chargeState = teslaVehicle.getVehicleChargeState();
			if (chargeState != null) {
				if (chargeState.getCharging_state().equals("Disconnected")) {
					System.out.println("Car is disconnected.");
					System.exit(1);
				}
				int power = ns.getInt("chargecalculation");
				ChargeCalculation calculation = new ChargeCalculation(teslaCharge);
				int watts = calculation.adaptCharging(chargeState, power);
				System.out.println("Charging power (watts): " + watts);
			} else {
				System.out.println("getting charge state failed");
				System.exit(1);
			}
		} else if (ns.getString("wakeup") != null) {
			boolean ok = teslaVehicle.wakeUpVehicle();
			if (ok) {
				System.out.println("Tesla is now awake");
			} else {
				System.out.println("Tesla is still sleeping");
			}
		}

	}

}
