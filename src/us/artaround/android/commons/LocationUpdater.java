package us.artaround.android.commons;

import java.util.ArrayList;
import java.util.List;

import us.artaround.android.commons.TimeoutTimer.Timeout.TimeoutCallback;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class LocationUpdater implements TimeoutCallback {
	private final static String TAG = "LOCATION";

	private final LocationManager manager;
	private final Activity context;
	private final LocationUpdaterCallback callback;
	private final LocationListener listener;

	private List<String> providers, fineProviders;
	private List<TimeoutTimer> timers;

	private boolean firstTime = true;
	private int counter = 0;

	public LocationUpdater(Activity context, LocationUpdaterCallback callback) {
		Log.d(TAG, "Instantiating a LocationUpdater for context " + context);
		this.callback = callback;
		this.context = context;

		timers = new ArrayList<TimeoutTimer>();
		manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		listener = getNewListener();
	}

	private LocationListener getNewListener() {
		return new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				Log.d(TAG, "onStatusChanged(" + provider + ") with status " + status);
				switch (status) {
				case LocationProvider.OUT_OF_SERVICE:
					endTimerForProvider(provider, this);
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					endTimerForProvider(provider, this);
					break;
				}
			}

			@Override
			public void onProviderEnabled(String provider) {
				Log.d(TAG, "onProviderEnabled(" + provider + ")");
			}

			@Override
			public void onProviderDisabled(String provider) {
				Log.d(TAG, "onProviderDisabled(" + provider + ")");
				endTimerForProvider(provider, this);
			}

			@Override
			public void onLocationChanged(Location location) {
				Log.d(TAG, "onLocationChanged(" + location + ")");
				callback.onLocationUpdate(location);
				endTimerForProvider(location.getProvider(), this);
			}
		};
	}

	private TimeoutTimer getTimer(String id) {
		int size = timers.size();
		for (int i = 0; i < size; i++) {
			TimeoutTimer timer = timers.get(i);
			if (timer.getId().equals(id)) {
				return timer;
			}
		}
		return null;
	}

	private void startTimerForProvider(String provider, LocationListener listener) {
		TimeoutTimer timer = getTimer(provider);
		if (timer != null) {
			Log.d(TAG, "There is another timed update from provider " + provider + " taking place.");
			return;
		}

		Log.d(TAG, "Getting a timed update from provider " + provider);
		timer = new TimeoutTimer(this, provider);
		timers.add(timer);
		manager.requestLocationUpdates(provider, 0, 0, listener);
		timer.start();
	}

	private void endTimerForProvider(String provider, LocationListener listener) {
		if (timers != null) {
			TimeoutTimer timer = getTimer(provider);
			if (timer != null) {
				timer.cancel();
				timers.remove(provider);
				Log.d(TAG, "Ending a timed update from provider " + provider);
			}
		}
		if (listener != null) {
			manager.removeUpdates(listener);
			Log.d(TAG, "Removing the listener for provider " + provider);
		}
	}

	public void removeUpdates() {
		int size = providers.size();
		for (int i = 0; i < size; i++) {
			String provider = providers.get(i);
			endTimerForProvider(provider, listener);
		}
	}

	private void updateEnabledProviders() {
		providers = manager.getProviders(true);
		fineProviders = manager.getProviders(fineCriteria, true);
		counter = providers.size() - 1;
		Log.d(TAG, "Currently enabled providers " + providers);
	}

	public void updateLocation() {
		updateEnabledProviders();

		if (suggestBetterSettings()) {
			callback.onSuggestLocationSettings();
			return;
		}
		if (!updateFromStoredLocation()) {
			updateFromLocationListener();
		}
	}

	private boolean suggestBetterSettings() {
		if (firstTime) {
			firstTime = false;
			if (fineProviders.isEmpty()) {
				return true;
			}
		}
		if (providers.isEmpty()) {
			return true;
		}
		return false;
	}

	private void updateFromLocationListener() {
		// enabled providers size may change
		if (counter >= 0 && providers.size() > counter) {
			String provider = providers.get(counter);
			startTimerForProvider(provider, listener);
			counter--;
		}
		else {
			callback.onLocationUpdateError();
		}
	}

	private boolean updateFromStoredLocation() {
		Location location = null;
		int size = providers.size();
		for (int i = 0; i < size; i++) {
			location = manager.getLastKnownLocation(providers.get(i));
			if (location != null) {
				callback.onLocationUpdate(location);
				return true;
			}
		}
		return false;
	}

	private void updateFromNextProvider() {
		if (counter >= 0) {
			updateFromLocationListener();
		}
		else {
			callback.onLocationUpdateError();
		}
	}

	@Override
	public void onTimeout(final TimeoutTimer timer) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String provider = timer.getId();
				endTimerForProvider(provider, listener);
				updateFromNextProvider();
			}
		});
	}

	private static final Criteria fineCriteria = createCriteria(Criteria.ACCURACY_FINE);

	public static Criteria createCriteria(int accuracy) {
		Criteria c = new Criteria();
		c.setAccuracy(accuracy);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.NO_REQUIREMENT);
		return c;
	}

	public interface LocationUpdaterCallback {
		void onSuggestLocationSettings();

		void onLocationUpdate(Location location);

		void onLocationUpdateError();
	}

}