package us.artaround.android.common.navigation;

import java.util.List;

import com.google.android.maps.GeoPoint;

public class Route {
	private List<GeoPoint> geoPoints;
	private List<Placemark> placemarks;
	private String totalDistance;

	public List<GeoPoint> getGeoPoints() {
		return geoPoints;
	}

	public void setGeoPoints(List<GeoPoint> geoPoints) {
		this.geoPoints = geoPoints;
	}

	public List<Placemark> getPlacemarks() {
		return placemarks;
	}

	public void setPlacemarks(List<Placemark> placemakers) {
		this.placemarks = placemakers;
	}

	public String getTotalDistance() {
		return totalDistance;
	}

	public void setTotalDistance(String totalDistance) {
		this.totalDistance = totalDistance;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Route [geoPoints=").append(geoPoints).append(", placemarks=").append(placemarks)
				.append(", totalDistance=").append(totalDistance).append("]");
		return builder.toString();
	}
}
