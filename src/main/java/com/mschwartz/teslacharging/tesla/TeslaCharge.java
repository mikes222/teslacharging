package com.mschwartz.teslacharging.tesla;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.tesla.TeslaVehicle.ChargeState;
import com.mschwartz.teslacharging.web.AuthRestRequest;

import lombok.Getter;

public class TeslaCharge {

	static final Logger logger = LogManager.getLogger(TeslaConfiguration.class);

	private AuthRestRequest authRestRequest;

	private TeslaVehicle teslaVehicle;

	public TeslaCharge(AuthRestRequest authRestRequest, TeslaVehicle teslaVehicle,
			TeslaConfiguration teslaConfiguration) {
		this.authRestRequest = authRestRequest;
		this.teslaVehicle = teslaVehicle;
	}

	/**
	 * Determine whether vehicle is currently charging.
	 * 
	 * @param id ID of the vehicle to use when requesting the charge state
	 * @return Boolean for whether the vehicle is currently charging.
	 * @throws Exception
	 */
	public boolean isVehicleCharging() throws Exception {
		ChargeState chargeState = teslaVehicle.getVehicleChargeState();
		if (chargeState != null) {
			if (chargeState.getCharging_state() != null && chargeState.isCharge_port_door_open()) {
				String chargingState = chargeState.getCharging_state();
				return chargingState.toLowerCase().equals("charging");
			}
		}

		return false;
	}

	/**
	 * Send a charge command to the vehicle
	 * 
	 * @param id            ID of the vehicle to use in the charge command request
	 * @param chargeCommand Command to send (start/stop)
	 * @return <code>null</code> if successful or the reason why the command failed.
	 *         e.g. unknown (no result received), disconnected (cable not connected
	 *         to the car)
	 * @throws Exception
	 */
	private String sendChargeCommand(String chargeCommand) throws Exception {
		String apiEndpoint = TeslaConfiguration.apiBase + "/api/1/vehicles/" + teslaVehicle.getId() + "/command/charge_"
				+ chargeCommand;
		SimpleResult simpleResult = authRestRequest.postJson(apiEndpoint, null, SimpleResult.class);
		if (simpleResult == null || simpleResult.getResponse() == null)
			return "unknown";
		if (simpleResult.getResponse().isResult()) {
			return null;
		}

		return simpleResult.getResponse().getReason();
	}

	/**
	 * 
	 * @param chargePortCommand open/close
	 * @return
	 * @throws Exception
	 */
	private String sendChargePortCommand(String chargePortCommand) throws Exception {
		String apiEndpoint = TeslaConfiguration.apiBase + "/api/1/vehicles/" + teslaVehicle.getId()
				+ "/command/charge_port_door_" + chargePortCommand;
		SimpleResult simpleResult = authRestRequest.postJson(apiEndpoint, null, SimpleResult.class);
		if (simpleResult == null || simpleResult.getResponse() == null)
			return "unknown";
		if (simpleResult.getResponse().isResult()) {
			return null;
		}

		return simpleResult.getResponse().getReason();
	}

	/**
	 * Start charging the vehicle
	 * 
	 * @param id ID of the vehicle to use in the request to start charging
	 * @return <code>null</code> if successful or the reason why the command failed.
	 *         e.g. unknown (no result received), disconnected (cable not connected
	 *         to the car)
	 * @throws Exception
	 */
	public String startCharging() throws Exception {
		return sendChargeCommand("start");
	}

	/**
	 * Stop charging the vehicle
	 * 
	 * @param id ID of the vehicle to use in the request to stop charging
	 * @return <code>null</code> if successful or the reason why the command failed.
	 *         e.g. unknown (no result received), disconnected (cable not connected
	 *         to the car)
	 * @throws Exception
	 */
	public String stopCharging() throws Exception {
		return sendChargeCommand("stop");
	}

	public String chargePortOpen() throws Exception {
		return sendChargePortCommand("open");
	}

	public String chargePortClose() throws Exception {
		return sendChargePortCommand("close");
	}

	public String setChargeLimit(int percent) throws Exception {
		String apiEndpoint = TeslaConfiguration.apiBase + "/api/1/vehicles/" + teslaVehicle.getId()
				+ "/command/set_charge_limit";
		PercentValue percentValue = new PercentValue(percent);
		SimpleResult simpleResult = authRestRequest.postJson(apiEndpoint, percentValue, SimpleResult.class);
		if (simpleResult == null || simpleResult.getResponse() == null)
			return "unknown";
		if (simpleResult.getResponse().isResult()) {
			return null;
		}

		return simpleResult.getResponse().getReason();

	}

	public String setChargingAmps(int chargingAmps) throws Exception {
		String apiEndpoint = TeslaConfiguration.apiBase + "/api/1/vehicles/" + teslaVehicle.getId()
				+ "/command/set_charging_amps";
		ChargingAmpsValue percentValue = new ChargingAmpsValue(chargingAmps);
		SimpleResult simpleResult = authRestRequest.postJson(apiEndpoint, percentValue, SimpleResult.class);
		if (simpleResult == null || simpleResult.getResponse() == null)
			return "unknown";
		if (simpleResult.getResponse().isResult()) {
			return null;
		}

		return simpleResult.getResponse().getReason();

	}

	public ChargeState getChargeState() throws Exception {
		String apiEndpoint = TeslaConfiguration.apiBase + "/api/1/vehicles/" + teslaVehicle.getId()
				+ "/data_request/charge_state";
		ChargeStateData simpleResult = authRestRequest.getJSON(apiEndpoint, ChargeStateData.class);
		if (simpleResult == null || simpleResult.getResponse() == null)
			return null;
		return simpleResult.getResponse();

	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class SimpleResult {

		SimpleResponse response;
	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class SimpleResponse {

		String reason;

		boolean result;
	}

	/////////////////////////////////////////////////////////////////////////

	class PercentValue {

		int percent;

		public PercentValue(int percent) {
			this.percent = percent;
		}

	}

	/////////////////////////////////////////////////////////////////////////

	class ChargingAmpsValue {

		int charging_amps;

		public ChargingAmpsValue(int charging_amps) {
			this.charging_amps = charging_amps;
		}

	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class ChargeStateData {

		ChargeState response;

	}

}
