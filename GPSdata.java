package com.test;

import java.util.Calendar;

import org.json.JSONObject;

public class GPSdata {

	private float latitude;
	private float longitude;
	private String date;

	public GPSdata(float latitude, float longitude){
		this.latitude = latitude;
		this.longitude = longitude;
		this.date = Calendar.getInstance().getTime().toString();
	};

	public GPSdata(JSONObject json){
		this.latitude = json.getFloat("latitude");
		this.longitude = json.getFloat("longitude");
		this.date =  json.getString("date");
	};

	public JSONObject toJSON(){
		return new JSONObject()
			.put("latitude", this.latitude)
			.put("longitude", this.longitude)
			.put("date", this.date);
	}

	@Override
	public String toString(){
		return this.toJSON().toString();
	}

}

