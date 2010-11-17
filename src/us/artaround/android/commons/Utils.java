package us.artaround.android.commons;

import android.location.Criteria;
import android.location.Location;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ARTAROUND";
	public static final String STR_SEP = "|";

	public static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_UPDATE_INTERVAL = "updateInterval";
	public static final String KEY_CATEGORIES = "categories";
	public static final String KEY_ART_ID = "artId";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";
	public static final int TIMEOUT = 30000; // 30 seconds

	public static float E6 = 1000000;

	public static GeoPoint geo(Location location) {
		return geo(location.getLatitude(), location.getLongitude());
	}

	public static GeoPoint geo(float latitude, float longitude) {
		return new GeoPoint((int) (latitude * E6), (int) (longitude * E6));
	}

	public static GeoPoint geo(double latitude, double longitude) {
		return geo((float) latitude, (float) longitude);
	}

	/** this criteria will settle for less accuracy, high power, and cost */
	public static Criteria createCoarseCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.NO_REQUIREMENT);
		return c;
	}

	/** this criteria needs high accuracy, high power, and cost */
	public static Criteria createFineCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.NO_REQUIREMENT);
		return c;
	}
}
