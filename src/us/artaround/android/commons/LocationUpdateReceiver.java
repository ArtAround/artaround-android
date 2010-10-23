package us.artaround.android.commons;

import java.util.ArrayList;
import java.util.List;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class LocationUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "LOCATION";

	private static List<LocationUpdateable> listeners = new ArrayList<LocationUpdateable>();

	public static void addListener(LocationUpdateable listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			Log.v(TAG, "LocationReceiver: registered listener " + listener);
		}
	}

	public static void removeListener(LocationUpdateable listener) {
		if (listeners.contains(listener)) {
			listeners.remove(listener);
			Log.v(TAG, "LocationReceiver: unregistered listener " + listener);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "LocationReceiver: received intent " + intent);

		if (intent.getAction().equals(LocationUpdater.ACTION_UPDATE_SUCCESS)) {
			String provider = intent.getStringExtra(LocationUpdater.INTENT_EXTRA_PROVIDER);
			Location location = intent.getParcelableExtra(LocationUpdater.INTENT_EXTRA_LOCATION);

			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).onLocationUpdate(provider, location);
			}

		} else if (intent.getAction().equals(LocationUpdater.ACTION_UPDATE_FAILURE)) {
			String provider = intent.getStringExtra(LocationUpdater.INTENT_EXTRA_PROVIDER);
			int code = intent.getIntExtra(LocationUpdater.INTENT_EXTRA_CODE, -1);

			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).onLocationUpdateError(provider, code);
			}
		}
	}
}
