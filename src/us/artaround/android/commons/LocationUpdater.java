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
import android.os.Bundle;
import android.util.Log;

public class LocationUpdater implements TimeoutCallback {
	private final static String TAG = "LOCATION";

	private final LocationManager manager;
	private final Activity context;
	private final LocationUpdaterCallback callback;
	private final LocationListener fineListener, coarseListener;

	private List<String> fineProviders, coarseProviders;
	private final List<TimeoutTimer> timers;

	private int fineCounter = 0, coarseCounter = 0;

	public LocationUpdater(Activity context, LocationUpdaterCallback callback) {
		Log.d(TAG, "Instantiating a LocationUpdater for context " + context);
		this.callback = callback;
		this.context = context;

		timers = new ArrayList<TimeoutTimer>();
		manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		fineListener = getNewListener();
		coarseListener = getNewListener();
	}

	private LocationListener getNewListener() {
		return new LocationListener() {
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				Log.d(TAG, "onStatusChanged(" + provider + ") with status " + status);
			}

			@Override
			public void onProviderEnabled(String provider) {
				Log.d(TAG, "onProviderEnabled(" + provider + ")");
			}

			@Override
			public void onProviderDisabled(String provider) {
				Log.d(TAG, "onProviderDisabled(" + provider + ")");
			}

			@Override
			public void onLocationChanged(Location location) {
				Log.d(TAG, "onLocationChanged(" + location + ")");
				endTimerForProvider(location.getProvider(), this);
				callback.onLocationUpdate(location);
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
		TimeoutTimer timer = new TimeoutTimer(this, provider);
		timer.setTag(listener);
		timers.add(timer);
		manager.requestLocationUpdates(provider, 0, 0, listener);
		timer.start();

		Log.d(TAG, "Getting a timed update from provider " + provider);
	}

	private void endTimerForProvider(String provider, LocationListener listener) {
		TimeoutTimer timer = getTimer(provider);
		timer.cancel();
		timers.remove(provider);
		manager.removeUpdates(listener);
		Log.d(TAG, "Ending a timed update from provider " + provider);
	}

	public void removeUpdates() {
		if (timers == null || timers.isEmpty()) return;
		int size = timers.size();
		for (int i = 0; i < size; i++) {
			TimeoutTimer timer = timers.get(i);
			endTimerForProvider(timer.getId(), (LocationListener) timer.getTag());
		}
	}

	private void updateEnabledProviders() {
		fineProviders = manager.getProviders(fineCriteria, true);
		coarseProviders = manager.getProviders(coarseCriteria, true);
		fineCounter = fineProviders.size() - 1;
		coarseCounter = coarseProviders.size() - 1;

		Log.d(TAG, "Currently enabled providers: fine=" + fineProviders + ", coarse=" + coarseProviders);
	}

	public void updateLocation() {
		updateEnabledProviders();

		if (providersNotEnabled()) {
			callback.onSuggestLocationSettings();
			return;
		}

		if (!updateFromStoredLocation()) {
			updateFromLocationListener();
		}
	}

	private boolean providersNotEnabled() {
		if ((fineProviders == null || fineProviders.isEmpty())
				&& (coarseProviders == null || coarseProviders.isEmpty())) {
			return true;
		}
		return false;
	}

	private void updateFromLocationListener() {
		String provider;
		if (fineCounter >= 0) {
			provider = fineProviders.get(fineCounter);
			fineCounter--;
			startTimerForProvider(provider, fineListener);
		}
		else if (coarseCounter >= 0) {
			provider = coarseProviders.get(coarseCounter);
			coarseCounter--;
			startTimerForProvider(provider, coarseListener);
		}
		else {
			Log.d(TAG, "Location update error!");
			callback.onLocationUpdateError();
		}
	}

	private boolean updateFromStoredLocation() {
		Location location = null;
		int i;
		String provider;

		int size = fineProviders.size();
		for (i = 0; i < size; i++) {
			provider = fineProviders.get(i);
			location = manager.getLastKnownLocation(provider);
			if (location != null) {
				Log.d(TAG, "Last known location is " + location.getLatitude() + "-"
						+ location.getLongitude() + " from provider " + provider);
				callback.onLocationUpdate(location);
				return true;
			}
		}

		size = coarseProviders.size();
		for (i = 0; i < size; i++) {
			provider = coarseProviders.get(i);
			location = manager.getLastKnownLocation(provider);
			if (location != null) {
				Log.d(TAG, "Last known location is " + location.getLatitude() + "-"
						+ location.getLongitude() + " from provider " + provider);
				callback.onLocationUpdate(location);
				return true;
			}
		}
		return false;
	}


	@Override
	public void onTimeout(final TimeoutTimer timer) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				endTimerForProvider(timer.getId(), (LocationListener) timer.getTag());
				updateFromLocationListener();

				Log.d(TAG, "Timeout for provider " + timer.getId());
			}
		});
	}

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

	public static final Criteria fineCriteria = createCriteria(Criteria.ACCURACY_FINE);
	public static final Criteria coarseCriteria = createCriteria(Criteria.ACCURACY_COARSE);

	public interface LocationUpdaterCallback {
		void onSuggestLocationSettings();

		void onLocationUpdate(Location location);

		void onLocationUpdateError();
	}
}