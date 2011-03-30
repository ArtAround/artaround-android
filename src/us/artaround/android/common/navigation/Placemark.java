package us.artaround.android.common.navigation;

import com.google.android.maps.GeoPoint;

public class Placemark {
	private GeoPoint geoPoint;
	private String instructions;
	private String distance;

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public void setGeoPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getDistance() {
		return distance;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Placemark [geoPoint=").append(geoPoint).append(", instructions=").append(instructions)
				.append(", distance=").append(distance).append("]");
		return builder.toString();
	}

}
