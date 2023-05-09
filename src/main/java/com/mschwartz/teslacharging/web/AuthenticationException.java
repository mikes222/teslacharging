package com.mschwartz.teslacharging.web;

public class AuthenticationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7376240560681935086L;

	AuthenticationException(Exception cause) {
		super(cause);

	}

}
