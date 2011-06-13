package us.artaround.android.ui;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.Utils;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;

public class LocatorFragment extends Fragment implements LoaderCallbacks<String> {
	private static final String TAG = "ArtAround.LocatorFragment";

	public static final String ARG_ADDRESS_UPDATE = "address_update";
	private static final String ARG_LOCATION = "location";

	public static final int ERROR_NO_PROVIDER = 0;
	public static final int ERROR_TIMEOUT = 1;
	public static final int ERROR_REVERSE_GEOCODING = 2;

	private static final int TIMEOUT = 30000; // 30 seconds

	private Map<String, CountDownTimer> timers;
	private LocationManager locationManager;
	private LocatorCallback callback;
	private boolean addressUpdate;

	public LocatorFragment() {}

	public LocatorFragment(LocatorCallback callback) {
		this.callback = callback;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		timers = Collections.synchronizedMap(new HashMap<String, CountDownTimer>());
		locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		addressUpdate = getArguments().getBoolean(ARG_ADDRESS_UPDATE);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		locationManager.removeUpdates(listener);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String fineProvider = locationManager.getBestProvider(fineCriteria, true);
		if (onUpdateFromProvider(fineProvider)) return;

		String coarseProvider = locationManager.getBestProvider(coarseCriteria, true);
		if (onUpdateFromProvider(coarseProvider)) return;

		if (callback != null) {
			callback.onLocationUpdateError(ERROR_NO_PROVIDER);
		}
	}

	private boolean onUpdateFromProvider(final String provider) {
		if (!TextUtils.isEmpty(provider)) {
			CountDownTimer timer = new CountDownTimer(TIMEOUT, 1) {
				@Override
				public void onTick(long millisUntilFinished) {}

				@Override
				public void onFinish() {
					onTimeoutProvider(provider, false);
				}
			};
			timers.put(provider, timer);
			timer.start();
			locationManager.requestLocationUpdates(provider, 0, 0, listener);
			Utils.d(TAG, "updateFromProvider(): provider=" + provider);
			return true;
		}
		return false;
	}

	protected void onTimeoutProvider(String provider, boolean success) {
		Utils.d(TAG, "timeoutProvider(): provider=" + provider + ", success=" + success);
		Iterator<Entry<String, CountDownTimer>> it = timers.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, CountDownTimer> entry = it.next();
			if (entry.getKey().equalsIgnoreCase(provider)) {
				entry.getValue().cancel();
				locationManager.removeUpdates(listener);
				it.remove();
			}
		}
		if (!success) {
			if (callback != null) {
				callback.onLocationUpdateError(ERROR_TIMEOUT);
			}
		}
	}

	protected void onStartAddressUpdate(Location location) {
		String address = null;
		if ((callback instanceof LocatorAddressCallback) && !TextUtils.isEmpty(address = getFromCache(location))) {
			((LocatorAddressCallback) callback).onAddressUpdate(address);
			return;
		}

		Bundle args = new Bundle();
		args.putParcelable(ARG_LOCATION, location);

		getLoaderManager().restartLoader(0, args, this);
	}

	public static interface LocatorCallback {
		void onLocationUpdate(Location location);

		void onLocationUpdateError(int errorCode);
	}

	public static interface LocatorAddressCallback extends LocatorCallback {
		void onAddressUpdate(String address);

		void onAddressUpdateError(int errorCode);
	}

	private final LocationListener listener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onLocationChanged(Location location) {
			if (callback != null) {
				callback.onLocationUpdate(location);
				onTimeoutProvider(location.getProvider(), true);

				if (addressUpdate) {
					onStartAddressUpdate(location);
				}
			}
		}
	};

	@Override
	public Loader<String> onCreateLoader(int id, final Bundle args) {
		return new AsyncLoader<String>(getActivity()) {
			@Override
			public String loadInBackground() {
				Location location = args.getParcelable(ARG_LOCATION);
				try {
					List<Address> addresses = new Geocoder(getActivity()).getFromLocation(location.getLatitude(),
							location.getLongitude(), 1);
					String address = getFullAddress(addresses);

					if (!TextUtils.isEmpty(address)) {
						addToCache(location, address);
						return address;
					}
				}
				catch (IOException e) {
					Utils.w(TAG, "loadInBackground(): exception=" + e);
				}
				return null;
			}
		};
	}

	private String getFullAddress(List<Address> addresses) {
		StringBuilder fullAddress = new StringBuilder();

		if (addresses != null && !addresses.isEmpty()) {
			Address address = addresses.get(0);

			for (int i = 0; i < address.getMaxAddressLineIndex(); ++i) {
				fullAddress.append(address.getAddressLine(i));
				fullAddress.append(", ");
			}
			fullAddress.append(address.getAddressLine(address.getMaxAddressLineIndex()));
		}

		return fullAddress.toString();
	}

	@Override
	public void onLoadFinished(Loader<String> loader, String address) {
		if (callback instanceof LocatorAddressCallback) {
			if (!TextUtils.isEmpty(address)) {
				((LocatorAddressCallback) callback).onAddressUpdate(address);
			}
			else {
				((LocatorAddressCallback) callback).onAddressUpdateError(ERROR_REVERSE_GEOCODING);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<String> loader) {}

	private static final Criteria fineCriteria = createCriteria(Criteria.ACCURACY_FINE);
	private static final Criteria coarseCriteria = createCriteria(Criteria.ACCURACY_COARSE);

	private static Criteria createCriteria(int accuracy) {
		Criteria c = new Criteria();
		c.setAccuracy(accuracy);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.NO_REQUIREMENT);
		return c;
	}

	private static final int MAX_CACHE_SIZE = 10;
	private static final LinkedHashMap<String, String> cache = new LinkedHashMap<String, String>();

	// Location doesn't override equals() or hashCode(), so we use latitude and longitude as key
	public static void addToCache(Location location, String address) {
		// make some room by deleting old addresses
		if (cache.size() > 0 && cache.size() == MAX_CACHE_SIZE) cache.remove(cache.keySet().iterator().next());

		cache.put(location.getLatitude() + "-" + location.getLongitude(), address);
		Utils.d(TAG, "addToCache(): address " + address + "  for location " + location);
	}

	public static String getFromCache(Location location) {
		return cache.get(location.getLatitude() + "-" + location.getLongitude());
	}
}
