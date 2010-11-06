package us.artaround.android.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public abstract class LocationUpdater {
	private static final String TAG = "LOCATION";

	private static final int TIMEOUT_TIME = 20000; // 20 seconds

	public static final int CODE_PROVIDER_DISABLED = 0;
	public static final int CODE_TIMEOUT = 1;

	public static final String ACTION_UPDATE_FAILURE = "us.artaround.android.ACTION_UPDATE_FAILURE";
	public static final String ACTION_UPDATE_SUCCESS = "us.artaround.android.ACTION_UPDATE_SUCCESS";

	public static final String INTENT_EXTRA_PROVIDER = "provider";
	public static final String INTENT_EXTRA_LOCATION = "location";
	public static final String INTENT_EXTRA_CODE = "code";

	private static HashMap<Context, Timer> timers = new HashMap<Context, Timer>();
	private static HashMap<Timer, LocationListener> listeners = new HashMap<Timer, LocationListener>();

	/**
	 * Returns the last known location fix, trying all enabled providers.
	 * 
	 * @see android.location.LocationManager#getLastKnownLocation(String)
	 * @param context
	 * @return the last known location, or null
	 */
	public static Location getStoredLocation(Context context) {
		LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		Location myLocation = null;
		List<String> enabledProviders = manager.getProviders(true);
		if (enabledProviders != null && !enabledProviders.isEmpty()) {
			for (String provider : enabledProviders) {
				myLocation = manager.getLastKnownLocation(provider);
				if (myLocation != null) {
					Log.d(TAG, "LocationUpdater: success fetching stored location from " + provider);
					break;
				} else {
					Log.d(TAG, "LocationUpdater: failure fetching stored location from " + provider);
				}
			}
		}
		return myLocation;
	}

	/**
	 * 
	 * @param context
	 * @param provider
	 */
	public static void requestSingleUpdate(final Context context, final String provider) {
		final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		if (!manager.isProviderEnabled(provider)) {
			context.sendBroadcast(getFailureIntent(provider, CODE_PROVIDER_DISABLED));
			return;
		}

		final Timer timer = new Timer() {
			@Override
			public void cancel() {
				super.cancel();
				manager.removeUpdates(listeners.get(this));
				listeners.remove(this);
				Log.d(TAG, "LocationUpdater: canceled timer and updates.");
			}
		};

		timers.put(context, timer);

		final LocationListener listener = new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onProviderDisabled(String provider) {}

			@Override
			public void onLocationChanged(Location location) {
				timer.cancel();
				timers.remove(context);

				context.sendBroadcast(getSuccessIntent(provider, location));
				Log.d(TAG, "LocationUpdater: update success " + location.toString());
			}
		};

		listeners.put(timer, listener);

		TimerTask timeoutTask = new TimerTask() {
			@Override
			public void run() {
				Timer timer = timers.get(context);
				timer.cancel();
				timers.remove(context);

				context.sendBroadcast(getFailureIntent(provider, CODE_TIMEOUT));
				Log.d(TAG, "LocationUpdater: timeout for " + provider + " after " + TIMEOUT_TIME + " millis");
			}
		};

		timer.schedule(timeoutTask, TIMEOUT_TIME);
		manager.requestLocationUpdates(provider, 0, 0, listener);
	}

	public static void cleanup(Context context) {
		if (timers.size() == 0) {
			return;
		}
		Timer timer = timers.get(context);
		if (timer != null) {
			timer.cancel();
			timers.remove(context);
		}
	}

	private static Intent getFailureIntent(String provider, int code) {
		Intent intent = new Intent(ACTION_UPDATE_FAILURE);
		intent.putExtra(INTENT_EXTRA_PROVIDER, provider);
		intent.putExtra(INTENT_EXTRA_CODE, code);
		return intent;
	}

	private static Intent getSuccessIntent(String provider, Location location) {
		Intent intent = new Intent(ACTION_UPDATE_SUCCESS);
		intent.putExtra(INTENT_EXTRA_PROVIDER, provider);
		intent.putExtra(INTENT_EXTRA_LOCATION, location);
		return intent;
	}
}
