package com.mschwartz.teslacharging.web;

public class SleepingCarException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3455802235331219949L;

	SleepingCarException(Exception cause) {
		super(cause);

	}

}
