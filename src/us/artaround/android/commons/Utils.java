package us.artaround.android.commons;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ARTAROUND";
	public static final String STR_SEP = "|";

	public static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_UPDATE_INTERVAL = "updateInterval";
	public static final String KEY_CATEGORIES = "categories";

	public static float E6 = 1000000;

	public static GeoPoint geo(float latitude, float longitude) {
		return new GeoPoint((int) (latitude * E6), (int) (longitude * E6));
	}
}
