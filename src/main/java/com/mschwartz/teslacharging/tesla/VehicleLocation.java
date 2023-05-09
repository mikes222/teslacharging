package com.mschwartz.teslacharging.tesla;

import java.time.Instant;

public class VehicleLocation {
	private double
		latitude,
		longitude,
		speed,
		heading,
		minutesToArrival
	;
	
	private int timestamp;
	
	private boolean
		goingHome = false,
		goingAwayFromHome = false
	;
	
	VehicleLocation(double lat, double lon) {
		latitude = lat;
		longitude = lon;
		timestamp = (int) Math.floor(Instant.now().toEpochMilli() / 1000);
	}
	
	// Assuming received timestamp is in milliseconds. We convert it to seconds.
	VehicleLocation(double lat, double lon, double spd, double head, double ts) {
		latitude = lat;
		longitude = lon;
		speed = spd;
		heading = head;
		timestamp = (int) Math.floor(ts / 1000);
	}

	// Convenience method to determine distance between two locations without getting lat/long first
	public double distanceFrom(VehicleLocation location2) {
		return distanceFrom(location2.getLatitude(), location2.getLongitude());
	}
	
	// Determine the approximate distance (in miles) between two points on a sphere (the Earth)	
	public double distanceFrom(double lat2, double lon2) {
		double lat1 = this.latitude;
		double lon1 = this.longitude;
		
		// Allow from some variance in GPS readings here, so that we can move a certain distance but
		// still be 0 miles apart. If my math is right this means a single coordinate covers an area
		// about 291' long x 231' feet wide.
		if (Math.abs(lat1 - lat2) <= 0.0008 && Math.abs(lon1 - lon2) <= 0.0008) {
			return 0;
		}

		// Distance between latitudes and longitudes
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		// Convert to radians
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		// Apply formulae
		double a =  Math.pow(Math.sin(dLat / 2), 2) +
					Math.pow(Math.sin(dLon / 2), 2) *
					Math.cos(lat1) *
					Math.cos(lat2);
		double rad = 6371;
		double c = 2 * Math.asin(Math.sqrt(a));

		return (rad * c) * 0.621371;	// Convert to miles
	}

	public int getArrivalTimeSeconds() {
		return (int)Math.floor((double)getTimestamp() + (Math.floor(getMinutesToArrival() * 60)));
	}

	public double getHeading() {
		return this.heading;
	}	
	
	public double getLatitude() {
		return this.latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	public double getMinutesToArrival() {
		return this.minutesToArrival;
	}
	
	public double getSpeed() {
		return this.speed;
	}
	
	public int getTimestamp() {
		return this.timestamp;
	}
	
	public double getTimestampMillis() {
		return (double)this.timestamp * 1000;
	}
	
	public boolean isGoingAwayFromHome() {
		return this.goingAwayFromHome;
	}
	
	public boolean isGoingHome() {
		return this.goingHome;
	}
	
	public VehicleLocation setGoingAwayFromHome(boolean f) {
		this.goingAwayFromHome = f;
		return this;
	}
	
	public VehicleLocation setGoingHome(boolean f) {
		this.goingHome = f;
		return this;
	}
	
	public VehicleLocation setHeading(double head) {
		this.heading = head;
		return this;
	}	
	
	public VehicleLocation setLatitude(double lat) {
		this.latitude = lat;
		return this;
	}
	
	public VehicleLocation setLongitude(double lon) {
		this.longitude = lon;
		return this;
	}
	
	public VehicleLocation setMinutesToArrival(double m) {
		this.minutesToArrival = m;
		return this;
	}
	
	public VehicleLocation setSpeed(double spd) {
		this.speed = spd;
		return this;
	}
	
	public VehicleLocation setTimestamp(double ts) {
		timestamp = (int) Math.floor(ts / 1000);
		return this;
	}
	
	public String toString() {
		return "Lat: " + latitude + ", Long: " + longitude + ", Heading: " + heading + ", Speed: " + speed + ", Timestamp: " + timestamp;
	}
}
