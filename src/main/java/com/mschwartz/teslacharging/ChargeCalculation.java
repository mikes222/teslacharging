package com.mschwartz.teslacharging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mschwartz.teslacharging.tesla.TeslaCharge;
import com.mschwartz.teslacharging.tesla.TeslaVehicle.ChargeState;

public class ChargeCalculation {

	static final Logger logger = LogManager.getLogger(ChargeCalculation.class);

	private TeslaCharge teslaCharge;

	public ChargeCalculation(TeslaCharge teslaCharge) {
		this.teslaCharge = teslaCharge;
	}

	/**
	 * Changes the charging speed/status of the car depending on the currently
	 * consumed power of the house. If this value is positive we are currently
	 * producing more power than needed.
	 * 
	 * @param powerSurplus, positive values indicate that we are producing more power than needed
	 * @throws Exception
	 */
	public int adaptCharging(ChargeState chargeState, int powerSurplus) throws Exception {
		// calculate the power surplus if the car is NOT charging
		int powerOfCar = calculateCurrentPowerToCar(chargeState);
		int powerSurplusWoCar = powerSurplus + powerOfCar;
		int power1Amp = calculate1AmpPower(chargeState);
		logger.info("The car consumes currently " + powerOfCar + " watts. We have a surplus of " + powerSurplusWoCar
				+ " watts (w/o car), we need at least " + (power1Amp * 4) + "watts.");
		logger.debug("w/o car: " + powerSurplusWoCar + ", 1amp: " + power1Amp);
		if (powerSurplusWoCar <= 0) {
			// if negative we should stop charging
			logger.info("Producing too less power to charge the car");
			stopCharging(chargeState);
			return 0;
		}
		if (powerSurplusWoCar - 3.5 * power1Amp <= 0) {
			// if we are negative when charging with at least 4 amp we should also stop (allow little margin)
			logger.info("Producing too less power to charge the car with at least 4 amps");
			stopCharging(chargeState);
			return 0;
		}
		int amps = (powerSurplusWoCar / power1Amp);
		if (amps != chargeState.getCharger_actual_current() || !chargeState.getCharging_state().equals("Charging")) {
			logger.info("We will charge with " + amps + " amps");
			String reason = teslaCharge.setChargingAmps(amps);
			if (reason != null) {
				System.out.println("Set charging amps to " + amps + " amps failed. Reason: " +  reason);
			}
			if (chargeState.getCharging_state().equals("Stopped")) {
				reason = teslaCharge.startCharging();
				if (reason != null) {
					System.out.println("Start charging failed. Reason: " +  reason);
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
				System.out.println("stop charging failed. Reason: " +  reason);
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
		int power = chargeState.getCharger_phases() * chargeState.getCharger_voltage();
		return power;
	}

	/**
	 * Returns the power in Watts currently consumed by the car
	 * 
	 * @param chargeState
	 * @return
	 */
	public int calculateCurrentPowerToCar(ChargeState chargeState) {
		int power = chargeState.getCharger_phases() * chargeState.getCharger_voltage()
				* chargeState.getCharger_actual_current();
		return power;
	}
}
