package us.artaround.android.commons;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import us.artaround.R;
import us.artaround.android.ui.ArtMap;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ArtAround";
	public static final String STR_SEP = ",";
	public static final String NL = "\n";
	public static final String USER_AGENT = "us.artaround";

	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int DEFAULT_CITY_CODE = 0; // Washington, DC

	public static final String KEY_CITY_CODE = "city_code";
	public static final String KEY_LAST_UPDATE = "last_update";
	public static final String KEY_UPDATE_INTERVAL = "update_interval";
	public static final String KEY_CLEARED_CACHE = "cleared_cache";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";
	public static final SimpleDateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT);

	public static final int TIMEOUT = 30000; // 30 seconds
	public static float E6 = 1000000;

	public static final boolean DEBUG_MODE = true;

	private static Method setView = null;

	public static int floatE6(float f) {
		return (int) ((int) f * E6);
	}

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
		if (date == null) {
			return null;
		}
		return df.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		if (TextUtils.isEmpty(date)) {
			return new Date();
		}
		return df.parse(date);
	}

	/*
	 * Using reflection to support custom tabs for 1.6 and up, and default to
	 * regular tabs for 1.5.
	 */
	static {
		checkCustomTabs();
	}

	// check for existence of TabHost.TabSpec#setIndicator(View)
	private static void checkCustomTabs() {
		try {
			setView = TabHost.TabSpec.class.getMethod("setIndicator", new Class[] { View.class });
		}
		catch (NoSuchMethodException nsme) {}
	}

	public static void addTab(Activity activity, TabHost tabHost, String tag, Intent intent, String name,
			Drawable backup) {
		TabHost.TabSpec tab = tabHost.newTabSpec(tag).setContent(intent);

		if (setView != null) {
			try {
				setView.invoke(tab, tabView(activity, name));
			}
			catch (IllegalAccessException ie) {
				throw new RuntimeException(ie);
			}
			catch (InvocationTargetException ite) {
				Throwable cause = ite.getCause();

				if (cause instanceof RuntimeException)
					throw (RuntimeException) cause;
				else if (cause instanceof Error)
					throw (Error) cause;
				else
					throw new RuntimeException(ite);
			}
		}
		else
			// default 1.5 tabs
			tab.setIndicator(name, backup);

		tabHost.addTab(tab);
	}

	public static View tabView(Context context, String name) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View tab = inflater.inflate(R.layout.tab, null);
		((TextView) tab.findViewById(R.id.tab_name)).setText(name);
		return tab;
	}

	public static Animation getLoadingAni(Context context) {
		Animation rotate = new RotateAnimation(0, 360, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(800);
		rotate.setInterpolator(new LinearInterpolator());
		rotate.setRepeatCount(Animation.INFINITE);
		return rotate;
	}

	public static void showToast(Context context, int msgId) {
		Toast.makeText(context, msgId, Toast.LENGTH_LONG).show();
	}

	public static void d(String tag, String message) {
		if (DEBUG_MODE) {
			Log.d(tag, message);
		}
	}

	public static void w(String tag, String message) {
		if (DEBUG_MODE) {
			Log.w(tag, message);
		}
	}

	public static void w(String tag, String message, Throwable e) {
		if (DEBUG_MODE) {
			Log.w(tag, message, e);
		}
	}

	public static void i(String tag, String message) {
		if (DEBUG_MODE) {
			Log.i(tag, message);
		}
	}

	public static Intent getHomeIntent(Context context) {
		return new Intent(context, ArtMap.class);
	}

	public static InputStream getConnection(String url) {
		InputStream is = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			is = conn.getInputStream();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}

	public static boolean isCacheOutdated(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		Date lastUpdate = getLastCacheUpdate(prefs);
		if (lastUpdate == null) {
			return true;
		}
		return (new Date().getTime() - lastUpdate.getTime()) > prefs.getLong(Utils.KEY_UPDATE_INTERVAL,
				Utils.DEFAULT_UPDATE_INTERVAL);
	}

	public static Date getLastCacheUpdate(SharedPreferences prefs) {
		long time = prefs.getLong(Utils.KEY_LAST_UPDATE, 0);
		if (time == 0) {
			return null;
		}
		return new Date(time);
	}

	public static void setLastCacheUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		Editor editor = prefs.edit();
		editor.putLong(Utils.KEY_LAST_UPDATE, new Date().getTime());
		SharedPreferencesCompat.apply(editor);
	}
}
