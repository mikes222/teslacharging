package com.mschwartz.teslacharging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.tesla.TeslaCharge;
import com.mschwartz.teslacharging.tesla.TeslaVehicle.ChargeState;

/**
 * Changes the charging speed/status of the car depending on the currently
 * consumed power. It calculates the new charging speed based on this value. 
 * The tesla API will be used to stop/start charging or set the charging amps 
 * accordingly. Note that the amps will be set between 4 and 32. 4 amps works 
 * without problems though the original tesla app does not allow below 5 amps. 
 * Note that efficiency drops with low amp values so whereas technically even 
 * 1 amp is possible we decided to limit to 4.
 * 
 * Note also that the amps are calculated based on rounded values so the system
 * may consume a little bit of power from the grid (or sends a little bit of power 
 * back to grid) depending on the rounded value. 1 amp is approx 700 Watts. 
 * 
 * We recommend to call this program not more often than every 5-15 minutes. We do not 
 * know if there is any side effect if the charging speed is constantly changing. 
 * 
 *  Note that the program needs to be checked for 1-phase Systems as well as for 110V
 *  systems. It is currently to my knowledge only tested for 3-phase 220V installations.
 * 
 * @author Mike
 *
 */
public class ChargeCalculation {

	static final Logger logger = LogManager.getLogger(ChargeCalculation.class);
	
	final int minAmps = 1;

	private TeslaCharge teslaCharge;

	public ChargeCalculation(TeslaCharge teslaCharge) {
		this.teslaCharge = teslaCharge;
	}

	/**
	 * Changes the charging speed/status of the car depending on the currently
	 * consumed power of the house. If this value is positive we are currently
	 * producing more power than needed.
	 * 
	 * @param powerSurplus, positive values indicate that we are producing more
	 *                      power than needed
	 * @throws Exception
	 */
	public int adaptCharging(ChargeState chargeState, int powerSurplus) throws Exception {
		// calculate the power surplus if the car is NOT charging
		int powerOfCar = calculateCurrentPowerToCar(chargeState);
		int powerSurplusWoCar = powerSurplus + powerOfCar;
		int power1Amp = calculate1AmpPower(chargeState);
		logger.info("The car consumes currently " + powerOfCar + " watts. We have a surplus of " + powerSurplusWoCar
				+ " watts (w/o car).");
		logger.debug("w/o car: " + powerSurplusWoCar + ", 1amp: " + power1Amp);
		if (powerSurplusWoCar <= 0) {
			// if negative we should stop charging
			logger.info("Producing too less power to charge the car");
			stopCharging(chargeState);
			return 0;
		}
		if (powerSurplusWoCar - (minAmps - 0.5) * power1Amp <= 0) {
			// if we are negative when charging with at least 1 amp we should also stop
			// (allow little margin)
			logger.info("Producing too less power to charge the car with at least " + minAmps + " amps");
			stopCharging(chargeState);
			return 0;
		}
		int amps = (int) Math.round((double) powerSurplusWoCar / power1Amp);
		if (amps > 32) {
			logger.info("Too much power available, tesla can only handle 32 amps in api, restrict to 32");
			amps = 32;
		}
		if (amps != chargeState.getCharger_actual_current() || !chargeState.getCharging_state().equals("Charging")) {
			logger.info("Charging with " + amps + " amps");
			String reason = teslaCharge.setChargingAmps(amps);
			if (reason != null) {
				System.out.println("Set charging amps to " + amps + " amps failed. Reason: " + reason);
			}
			if (chargeState.getCharging_state().equals("Stopped")) {
				reason = teslaCharge.startCharging();
				if (reason != null) {
					System.out.println("Start charging failed. Reason: " + reason);
				}
			}
		} else {
			logger.info("No change needed");
		}
		return amps * power1Amp;
	}

	public void stopCharging(ChargeState chargeState) throws Exception {
		if (chargeState.getCharging_state().equals("Charging")) {
			String reason = teslaCharge.stopCharging();
			if (reason != null) {
				System.out.println("stop charging failed. Reason: " + reason);
			}
		}
	}

	/**
	 * Calculate the power in Watt which would be consumed by incrementing the amps
	 * by one
	 * 
	 * @param chargeState
	 * @return
	 */
	public int calculate1AmpPower(ChargeState chargeState) {
		if (chargeState.getCharger_phases() == 0 || chargeState.getCharger_voltage() < 100) {
			// invalid data, calculate base on our own experience
			return 3 * 220;
		}
		// the onboard charger itself has only 2 phases but the wallcharger uses 3.
		int power = /* chargeState.getCharger_phases() */ 3 * chargeState.getCharger_voltage();
		return power;
	}

	/**
	 * Returns the power in Watts currently consumed by the car
	 * 
	 * @param chargeState
	 * @return
	 */
	public int calculateCurrentPowerToCar(ChargeState chargeState) {
		int power = calculate1AmpPower(chargeState) * chargeState.getCharger_actual_current();
		return power;
	}
}
