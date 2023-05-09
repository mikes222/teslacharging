package com.mschwartz.teslacharging.tesla;

import java.util.List;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.web.AuthRestRequest;

import lombok.Getter;

public class TeslaVehicle {

	static final Logger logger = LogManager.getLogger(TeslaVehicle.class);

	private AuthRestRequest authRestRequest;

	@Getter
	private String id;

	private String vin;

	private String displayName;

	private Double homeLatitude;

	private Double homeLongitude;

	private VehicleLocation lastHome;

	// Vehicle location history
	private CircularFifoQueue<VehicleLocation> vehicleLocationHistory = new CircularFifoQueue<VehicleLocation>(250);

	public TeslaVehicle(AuthRestRequest authRestRequest, TeslaConfiguration teslaConfiguration) throws Exception {
		this.authRestRequest = authRestRequest;
		vin = teslaConfiguration.readVin();
		id = teslaConfiguration.readID_S();
		displayName = teslaConfiguration.readDisplayName();
		try {
			homeLatitude = teslaConfiguration.readHomeLatitude();
			homeLongitude = teslaConfiguration.readHomeLongitude();
		} catch (Exception ex) {
			// logger.fatal("Unable to read Home latitude/longitude, Exiting.");
			// System.exit(1);
		}
		if (vin == null || id == null || displayName == null)
			getVehicleMatchForVIN(teslaConfiguration);
	}

	/**
	 * Get a JSON object with the vehicle details matching the configured VIN. If no
	 * VIN is configured and the account only has one Tesla associated with it,
	 * return that vehicle.
	 * 
	 * @return JSON object with vehicle details
	 * @throws Exception
	 */
	public Vehicle getVehicleMatchForVIN(TeslaConfiguration teslaConfiguration) throws Exception {
		Vehicle vehicleMatch = null;

		VehicleList responseJSON = authRestRequest.getJSON(TeslaConfiguration.apiBase + "/api/1/vehicles",
				VehicleList.class);
		if (responseJSON != null) {
			if (responseJSON.response.size() > 0) {

				// When we only have one vehicle and no VIN, we use the identifier of the
				// vehicle we've found.
				if (responseJSON.response.size() == 1 && (vin == null || vin.length() == 0)) {
					Vehicle vehicle = responseJSON.response.get(0);
					if (vehicle.getId_s() != null) {
						vehicleMatch = vehicle;
					}

					// With multiple vehicles or a defined VIN, make sure we find the correct
					// vehicle.
				} else {
					for (Vehicle vehicle : responseJSON.response) {
						if (vehicle.getId_s() != null && vehicle.getVin() != null) {
							if (vehicle.getVin().equals(vin)) {
								vehicleMatch = vehicle;
								break;
							}
						}
					}
				}
			}
		}

		vin = vehicleMatch.getVin();
		id = vehicleMatch.getId_s();
		if (vehicleMatch.getDisplay_name() != null) {
			displayName = vehicleMatch.getDisplay_name();
		}

		teslaConfiguration.updateVin(vin, id, displayName);
		return vehicleMatch;
	}

	/**
	 * Get the full vehicle data set
	 * 
	 * @param id ID of the vehicle to use when requesting vehicle data
	 * @return Vehicle data response JSON object
	 * @throws Exception
	 */
	public Vehicle getVehicleData() throws Exception {
		VehicleData vehicleDataResponse = authRestRequest
				.getJSON(TeslaConfiguration.apiBase + "/api/1/vehicles/" + id + "/vehicle_data", VehicleData.class);

		if (vehicleDataResponse != null && vehicleDataResponse.getResponse() != null) {
			return vehicleDataResponse.getResponse();
		}

		return null;
	}

	/**
	 * Wake up the vehicle so we know future commands will work.
	 * 
	 * @param id ID of the vehicle to use in the wake_up request
	 * @throws Exception
	 */
	private boolean wakeUpVehicle() throws Exception {
		logger.debug("Wake up, Tesla {}!", id);
		VehicleData wakeResponse = authRestRequest
				.postJson(TeslaConfiguration.apiBase + "/api/1/vehicles/" + id + "/wake_up", null, VehicleData.class);
		if (wakeResponse != null) {
			if (wakeResponse.getResponse() != null) {
				Vehicle response = wakeResponse.getResponse();
				if (response.getState() != null && response.getState().equals("online")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get the current state of the vehicle with the provided ID. NOTE! This call
	 * won't wake a sleeping Tesla!
	 * 
	 * @param id ID of the vehicle to use when requesting the current state
	 * @return String representing vehicle state (online, asleep, offline, waking,
	 *         unknown)
	 * @throws Exception
	 */
	private String getVehicleState() throws Exception {
		VehicleData vehicleResponse = authRestRequest.getJSON("api/1/vehicles/" + id, VehicleData.class);

		if (vehicleResponse != null) {
			Vehicle data = vehicleResponse.getResponse();
			if (data.getState() != null) {
				return data.getState();
			}
		}

		return "unknown";
	}

	/**
	 * Get the drive state of the vehicle
	 * 
	 * @param id ID of the vehicle to use when requesting drive state
	 * @return Drive state response JSON object
	 * @throws Exception
	 */
	private DriveState getVehicleDriveState() throws Exception {
		Vehicle vehicleDataResponse = getVehicleData();
		if (vehicleDataResponse != null && vehicleDataResponse.getDrive_state() != null) {
			DriveState data = vehicleDataResponse.getDrive_state();
			return data;
		}
		return null;
	}

	/**
	 * Get the charge state of the vehicle
	 * 
	 * @param id ID of the vehicle to use when requesting charge state
	 * @return Charge state response JSON object
	 * @throws Exception
	 */
	public ChargeState getVehicleChargeState() throws Exception {
		Vehicle vehicleDataResponse = getVehicleData();
		if (vehicleDataResponse != null && vehicleDataResponse.getCharge_state() != null) {
			return vehicleDataResponse.getCharge_state();
		}
		return null;
	}

	/**
	 * Request current location details from vehicle and store them in our queue,
	 * returning the current location details.
	 * 
	 * Make sure to wake up the vehicle
	 * 
	 * @param id ID of the vehicle to use in the drive state request
	 * @return VehicleLocation object with location details returned from the Tesla
	 *         API
	 * @throws Exception
	 */
	private VehicleLocation updateVehicleLocationDetails() throws Exception {

		DriveState driveStateResponse = getVehicleDriveState();
		VehicleLocation v = null;
		if (driveStateResponse != null) {
			if (driveStateResponse.getLatitude() != null && driveStateResponse.getLongitude() != null) {

				double currentLatitude = driveStateResponse.getLatitude();
				double currentLongitude = driveStateResponse.getLongitude();
				double speed = driveStateResponse.getSpeed() == null ? 0 : driveStateResponse.getSpeed();
				double heading = driveStateResponse.getHeading();
				double timestamp = driveStateResponse.getTimestamp();

				v = new VehicleLocation(currentLatitude, currentLongitude, speed, heading, timestamp);

				// Vehicle is currently navigating. We can use that to schedule the next
				// location poll!
//				if (driveStateResponse.getAhas("active_route_latitude")
//						&& driveStateResponse.has("active_route_longitude")) {
//					double destinationLatitude = driveStateResponse.getDouble("active_route_latitude");
//					double destinationLongitude = driveStateResponse.getDouble("active_route_longitude");
//
//					VehicleLocation destination = new VehicleLocation(destinationLatitude, destinationLongitude);
//					if (driveStateResponse.has("active_route_minutes_to_arrival")) {
//						double destinationMinutesToArrival = driveStateResponse
//								.getDouble("active_route_minutes_to_arrival");
//						if (destination.distanceFrom(homeLatitude, homeLongitude) == 0) {
//							v.setGoingHome(true);
//						} else {
//							v.setGoingAwayFromHome(true);
//						}
//						v.setMinutesToArrival(destinationMinutesToArrival);
//					}
//				}

				logger.debug("Most recent vehicle location: {}", v.toString());

				// When we see vehicle is at home, clear location history so when we start
				// looking at history to
				// determine next polling times we don't have to worry about home -> destination
				// -> home.
				double distanceFromHome = v.distanceFrom(homeLatitude, homeLongitude);
				if (distanceFromHome == 0) {
					vehicleLocationHistory.clear();
					lastHome = v;
				}

				// Only add this new location to the queue if it significantly different from
				// the two entries that came
				// before it. We only need two entries in a row to decide the vehicle has
				// stopped, no point in continuing
				// to fill the queue with stopped entries.
				int locationHistorySize = vehicleLocationHistory.size();
				if (locationHistorySize >= 2) {
					VehicleLocation mostRecentLocation = vehicleLocationHistory.get(locationHistorySize - 1);
					VehicleLocation previousLocation = vehicleLocationHistory.get(locationHistorySize - 2);
					if (!(mostRecentLocation.distanceFrom(previousLocation) == 0
							&& mostRecentLocation.distanceFrom(v) == 0)) {
						vehicleLocationHistory.add(v);
					}
				} else {
					vehicleLocationHistory.add(v);
				}
			}
		}

		return v;
	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class Vehicle {

		String id;

		// from vehicleData
		String user_id;

		String vehicle_id;

		String vin;

		String display_name;

		String color;

		// from vehicleData, e.g. "OWNER"
		String access_type;

		List<String> tokens;

		// e.g. "online"
		String state;

		boolean in_service;

		String id_s;

		boolean calendar_enabled;

		int api_version;

		String backseat_token;

		String backset_token_updated_at;

		// from vehicleData
		DriveState drive_state;

		// from vehicleData
		ChargeState charge_state;

		String option_codes;

		boolean ble_autopair_enrolled;

	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	public class ChargeState {

		boolean battery_heater_on;

		// battery level in percent (e.g. 46)
		int battery_level;

		// in miles (150.2 = 242km)
		double battery_range;

		// charge amps requested, e.g. 32
		int charge_amps;

		// currently requested charging amps
		int charge_current_request;

		// maximum possible charging amps
		int charge_current_request_max;

		boolean charge_enable_request;

		double charge_energy_added;

		// e.g. 50
		int charge_limit_soc;

		// e.g. 100
		int charge_limit_soc_max;

		// e.g. 50
		int charge_limit_soc_min;

		// e.g. 90
		int charge_limit_soc_std;

		double charge_miles_added_ideal;

		double charge_miles_added_rated;

		boolean charge_port_cold_weather_mode;

		// e.g. "<invalid>"
		String charge_port_color;

		// e.g. "Engaged"
		String charge_port_latch;

		// e.g. 7.4
		double charge_rate;

		boolean charge_to_max_range;

		// e.g. 4
		int charger_actual_current;

		// e.g. 2
		Integer charger_phases;

		int charger_pilot_current;

		// e.g. 2
		int charger_power;

		// not always zero if not charging. I see "2" now without connection
		int charger_voltage;

		// e.g. "Disconnected", "Stopped", "Charging"
		String charging_state;

		// e.g. "<invalid>", IEC
		String conn_charge_cable;

		// e.g. 108.11
		double est_battery_range;

		// e.g. "<invalid>"
		String fast_charger_brand;

		boolean fast_charger_present;

		// e.g. "<invalid>", "ACSingleWireCAN"
		String fast_charger_type;

		double ideal_battery_range;

		boolean managed_charging_active;

		Long managed_charging_start_time;

		boolean managed_charging_user_canceled;

		int max_range_charge_counter;

		// e.g. 370
		int minutes_to_full_charge;

		Boolean not_enough_power_to_heat;

		boolean scheduled_charging_pending;

		Long scheduled_charging_start_time;

		double time_to_full_charge;

		long timestamp;

		boolean trip_charging;

		// e.g. 35
		int usable_battery_level;

		String user_charge_enable_request;

		boolean charge_port_door_open;

		boolean off_peak_charging_enabled;

		// e.g. "all_week"
		String off_peak_charging_times;

		// e.g. 360
		int off_peak_hours_end_time;

		boolean preconditioning_enabled;

		// e.g. "all_week"
		String preconditioning_times;

		// e.g. "Off"
		String scheduling_charging_mode;

		boolean scheduling_charging_pending;

		String scheduling_charging_start_time;

		int scheduling_charging_start_time_app;

		long scheduled_departure_time;

		int scheduled_departure_time_minutes;

		boolean supercharger_session_trip_planner;

		@Override
		public String toString() {
			return "battery_heater_on=" + battery_heater_on + "\nbattery_level=" + battery_level + "\nbattery_range="
					+ battery_range + "\ncharge_amps=" + charge_amps + "\ncharge_current_request="
					+ charge_current_request + "\ncharge_current_request_max=" + charge_current_request_max
					+ "\ncharge_enable_request=" + charge_enable_request + "\ncharge_energy_added="
					+ charge_energy_added + "\ncharge_limit_soc=" + charge_limit_soc + "\ncharge_limit_soc_max="
					+ charge_limit_soc_max + "\ncharge_limit_soc_min=" + charge_limit_soc_min
					+ "\ncharge_limit_soc_std=" + charge_limit_soc_std + "\ncharge_miles_added_ideal="
					+ charge_miles_added_ideal + "\ncharge_miles_added_rated=" + charge_miles_added_rated
					+ "\ncharge_port_cold_weather_mode=" + charge_port_cold_weather_mode + "\ncharge_port_color="
					+ charge_port_color + "\ncharge_port_latch=" + charge_port_latch + "\ncharge_rate=" + charge_rate
					+ "\ncharge_to_max_range=" + charge_to_max_range + "\ncharger_actual_current="
					+ charger_actual_current + "\ncharger_phases=" + charger_phases + "\ncharger_pilot_current="
					+ charger_pilot_current + "\ncharger_power=" + charger_power + "\ncharger_voltage="
					+ charger_voltage + "\ncharging_state=" + charging_state + "\nconn_charge_cable="
					+ conn_charge_cable + "\nest_battery_range=" + est_battery_range + "\nfast_charger_brand="
					+ fast_charger_brand + "\nfast_charger_present=" + fast_charger_present + "\nfast_charger_type="
					+ fast_charger_type + "\nideal_battery_range=" + ideal_battery_range + "\nmanaged_charging_active="
					+ managed_charging_active + "\nmanaged_charging_start_time=" + managed_charging_start_time
					+ "\nmanaged_charging_user_canceled=" + managed_charging_user_canceled
					+ "\nmax_range_charge_counter=" + max_range_charge_counter + "\nminutes_to_full_charge="
					+ minutes_to_full_charge + "\nnot_enough_power_to_heat=" + not_enough_power_to_heat
					+ "\nscheduled_charging_pending=" + scheduled_charging_pending + "\nscheduled_charging_start_time="
					+ scheduled_charging_start_time + "\ntime_to_full_charge=" + time_to_full_charge + "\ntimestamp="
					+ timestamp + "\ntrip_charging=" + trip_charging + "\nusable_battery_level=" + usable_battery_level
					+ "\nuser_charge_enable_request=" + user_charge_enable_request + "\ncharge_port_door_open="
					+ charge_port_door_open + "\noff_peak_charging_enabled=" + off_peak_charging_enabled
					+ "\noff_peak_charging_times=" + off_peak_charging_times + "\noff_peak_hours_end_time="
					+ off_peak_hours_end_time + "\npreconditioning_enabled=" + preconditioning_enabled
					+ "\npreconditioning_times=" + preconditioning_times + "\nscheduling_charging_mode="
					+ scheduling_charging_mode + "\nscheduling_charging_pending=" + scheduling_charging_pending
					+ "\nscheduling_charging_start_time=" + scheduling_charging_start_time
					+ "\nscheduling_charging_start_time_app=" + scheduling_charging_start_time_app
					+ "\nscheduled_departure_time=" + scheduled_departure_time + "\nscheduled_departure_time_minutes="
					+ scheduled_departure_time_minutes + "\nsupercharger_session_trip_planner="
					+ supercharger_session_trip_planner;
		}

	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class DriveState {

		int gps_as_of;

		int heading;

		Double latitude;

		Double longitude;

		double native_latitude;

		double native_longitude;

		int native_location_supported;

		String native_type;

		int power;

		String shift_state;

		Double speed;

		long timestamp;
	}

	/////////////////////////////////////////////////////////////////////////

	class VehicleList {

		List<Vehicle> response;

		int count;

	}

	/////////////////////////////////////////////////////////////////////////

	@Getter
	class VehicleData {

		Vehicle response;

	}
}
