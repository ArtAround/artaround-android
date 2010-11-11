package us.artaround.android.commons;

import android.location.LocationManager;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ARTAROUND";
	public static final String STR_SEP = "|";

	public static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_UPDATE_INTERVAL = "updateInterval";
	public static final String KEY_CATEGORIES = "categories";
	public static final String KEY_SUGGEST_LOCATION_SETTINGS = "suggestLocationSettings";
	public static final String KEY_ART_ID = "artId";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";

	public static float E6 = 1000000;

	public static final String[] PROVIDERS = new String[] { LocationManager.GPS_PROVIDER,
			LocationManager.NETWORK_PROVIDER };

	public static GeoPoint geo(float latitude, float longitude) {
		return new GeoPoint((int) (latitude * E6), (int) (longitude * E6));
	}

	public static GeoPoint geo(double latitude, double longitude) {
		return geo((float) latitude, (float) longitude);
	}
}
