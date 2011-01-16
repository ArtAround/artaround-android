package us.artaround.models;

import com.google.android.maps.GeoPoint;

public class City {
	public String name;
	public int code;
	public GeoPoint center;
	public String serverUrl;

	public City(String name, int code, GeoPoint center, String serverUrl) {
		super();
		this.name = name;
		this.code = code;
		this.center = center;
		this.serverUrl = serverUrl;
	}

	@Override
	public String toString() {
		return "City [name=" + name + ", code=" + code + ", center=" + center + ", serverUrl=" + serverUrl + "]";
	}
}
