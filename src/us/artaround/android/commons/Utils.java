package us.artaround.android.commons;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;
import android.text.TextUtils;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ARTAROUND";
	public static final String STR_SEP = "|";

	public static final String USER_AGENT = "us.artaround";

	public static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_UPDATE_INTERVAL = "updateInterval";
	public static final String KEY_CATEGORIES = "categories";
	public static final String KEY_CLEARED_CACHE = "cleared_cache";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";
	public static final SimpleDateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT);

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

	public static String formatDate(Date date) {
		return df.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		if (TextUtils.isEmpty(date)) {
			return new Date();
		}
		return df.parse(date);
	}
}
