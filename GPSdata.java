package ClientUDP;


import java.util.Calendar;

import org.json.JSONObject;

public class GPSdata {

	private float latitude;
	private float longitude;
	private Calendar date;

	public GPSdata(float latitude, float longitude){
		this.latitude = latitude;
		this.longitude = longitude;
		this.date = Calendar.getInstance();
	};

	public JSONObject toJSON(){
		return new JSONObject()
			.put("latitude", this.latitude)
			.put("longitude", this.longitude)
			.put("date", this.date);
	}

}
