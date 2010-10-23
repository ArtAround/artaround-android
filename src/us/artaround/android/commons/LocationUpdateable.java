package us.artaround.android.commons;

import android.location.Location;

public interface LocationUpdateable {
	void onLocationUpdate(String provider, Location location);

	void onLocationUpdateError(String provider, int code);
}
