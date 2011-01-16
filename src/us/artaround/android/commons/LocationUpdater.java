package us.artaround.android.commons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import us.artaround.android.commons.TimeoutTimer.Timeout.TimeoutCallback;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

public class LocationUpdater implements TimeoutCallback
{
	public final static String TAG = "LOCATION";
	
	private static final int MIN_TIME = 5000;
	private static final int MIN_DISTANCE = 100;

	// cache the addresses obtained for different locations
	private static final int MAX_CACHE_SIZE = 10;
	private static final LinkedHashMap<String, String> cache = new LinkedHashMap<String, String>();

	private Context context;
	private final LocationManager manager;

	private LocationUpdaterCallback locationCallback;
	private AddressUpdaterCallback addressCallback;

	private final LocationListener fineListener, coarseListener;
	private List<String> fineProviders, coarseProviders;
	private int fineCounter, coarseCounter;
	private boolean foundLocation, isCancelled;

	private final List<TimeoutTimer> timers;
	private final Handler handler;

	private AddressUpdater addressUpdater;
	
	private Object tag; // this can be used to attach any other info

	public LocationUpdater(Context context)
	{
		attach(context);

		timers = Collections.synchronizedList(new ArrayList<TimeoutTimer>());
		handler = new Handler();

		manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		fineListener = getNewListener();
		coarseListener = getNewListener();
		
		Utils.d(TAG, "Instantiated a LocationUpdater for context " + context);
	}

	public Object getTag()
	{
		return tag;
	}
	
	public void setTag(Object tag)
	{
		this.tag = tag;
		Utils.d(TAG, "Attached tag " + tag);
	}

	public void updateLocation()
	{
		updateEnabledProviders();
		
		if (providersNotEnabled())
		{
			locationCallback.onSuggestLocationSettings();
			return;
		}
		
		// if (!updateFromStoredLocation())
		// {
		updateFromLocationListener();
		// }
	}
	
	public boolean isUpdatingAddress()
	{
		return addressUpdater != null && addressUpdater.getStatus() == AsyncTask.Status.RUNNING;
	}

	public void removeUpdates()
	{
		if (timers == null || timers.isEmpty())
			return;

		isCancelled = true;

		Iterator<TimeoutTimer> it = timers.iterator(); // because the list is being modified
		while (it.hasNext())
		{
			TimeoutTimer timer = it.next();
			endTimer(timer);
			it.remove();
		}

		Utils.d(TAG, "Removed all location updates.");
	}

	public void attach(Context context)
	{
		this.context = context;
		
		if (!(context instanceof LocationUpdaterCallback))
		{
			throw new IllegalArgumentException("Your Context needs to implement the LocationUpdaterCallback!");
		}
		
		this.locationCallback = (LocationUpdaterCallback) context;
		
		if (context instanceof AddressUpdaterCallback) // not mandatory if no address update is required
		{
			addressCallback = (AddressUpdaterCallback) context;
		}
		Utils.d(TAG, "Attached a new context " + context);
	}
	
	public void detach()
	{
		removeUpdates();

		this.context = null;
		this.locationCallback = null;
		this.addressCallback = null;

		Utils.d(TAG, "Detached the old context " + context);
	}
	
	public void updateAddress(Location location)
	{
		if (addressCallback == null)
		{
			throw new IllegalArgumentException("Your Context needs to implement the AddressUpdaterCallback!");
		}
		
		if (location == null)
		{
			addressCallback.onAddressUpdateError();
			Utils.d(TAG, "Address update error - location is null.");
		}
		
		String address = getFromCache(location);
		if (!TextUtils.isEmpty(address))
		{
			addressCallback.onAddressUpdate(address);
			Utils.d(TAG, "Address for location " + location + " already in cache: " + address);
		}
		else
		{
			requestReverseGeocoding(location);
		}
	}

	private LocationListener getNewListener()
	{
		return new LocationListener()
		{
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras)
			{
				Utils.d(TAG, "onStatusChanged(" + provider + ") with status " + status);
				if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				{
					endTimerForProvider(provider, this);
				}
			}
			
			@Override
			public void onProviderEnabled(String provider)
			{
				Utils.d(TAG, "onProviderEnabled(" + provider + ")");
			}
			
			@Override
			public void onProviderDisabled(String provider)
			{
				Utils.d(TAG, "onProviderDisabled(" + provider + ")");
			}
			
			@Override
			public void onLocationChanged(Location location)
			{
				foundLocation = true;
				endTimerForProvider(location.getProvider(), this);
				locationCallback.onLocationUpdate(location);
				Utils.d(TAG, "Location update success: " + location);
			}
		};
	}
	
	private TimeoutTimer getTimer(String id)
	{
		int size = timers.size();
		for (int i = 0; i < size; i++)
		{
			TimeoutTimer timer = timers.get(i);
			if (timer.getId().equals(id))
			{
				return timer;
			}
		}
		return null;
	}

	private void startTimerForProvider(String provider, LocationListener listener)
	{
		TimeoutTimer timer = new TimeoutTimer(this, handler, provider);
		timer.setTag(listener);
		timer.start();

		timers.add(timer);
		manager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, listener);
		
		Utils.d(TAG, "Getting a timed update from " + provider);
	}

	private void endTimerForProvider(String provider, LocationListener listener)
	{
		TimeoutTimer timer = getTimer(provider);
		endTimer(timer);
		timers.remove(timer);
	}
	
	private void endTimer(TimeoutTimer timer)
	{
		Utils.d(TAG, "Ending a timed update from " + timer.getId());
		
		timer.stop(); // this will NOT stop the already running thread
		manager.removeUpdates((LocationListener) timer.getTag());
	}

	private void updateEnabledProviders()
	{
		fineProviders = manager.getProviders(fineCriteria, true);
		coarseProviders = manager.getProviders(coarseCriteria, true);
		
		// there's no point in having the same provider in both lists (like gps) ?
		coarseProviders.removeAll(fineProviders);
		
		fineCounter = coarseCounter = 0;
		foundLocation = false;
		isCancelled = false;

		Utils.d(TAG, "Currently enabled providers: fine=" + fineProviders + ", coarse=" + coarseProviders);
	}

	private boolean providersNotEnabled()
	{
		if ((fineProviders == null || fineProviders.isEmpty())
				&& (coarseProviders == null || coarseProviders.isEmpty()))
		{
			return true;
		}
		return false;
	}
	
	// private boolean updateFromStoredLocation()
	// {
	// Location location = null;
	// int i;
	// String provider;
	//
	// int size = fineProviders.size();
	// for (i = 0; i < size; i++)
	// {
	// provider = fineProviders.get(i);
	// location = manager.getLastKnownLocation(provider);
	// if (location != null)
	// {
	// Utils.d(TAG, "Last location from " + provider + " is " + location.getLatitude() + "-"
	// + location.getLongitude());
	// locationCallback.onLocationUpdate(location);
	// return true;
	// }
	// }
	//
	// size = coarseProviders.size();
	// for (i = 0; i < size; i++)
	// {
	// provider = coarseProviders.get(i);
	// location = manager.getLastKnownLocation(provider);
	// if (location != null)
	// {
	// Utils.d(TAG,
	// "Last known location is " + location.getLatitude() + "-" + location.getLongitude()
	// + " from provider " + provider);
	// locationCallback.onLocationUpdate(location);
	// return true;
	// }
	// }
	// return false;
	// }

	private void updateFromLocationListener()
	{
		if (isCancelled)
		{
			return;
		}

		if (fineCounter < fineProviders.size())
		{
			startTimerForProvider(fineProviders.get(fineCounter), fineListener);
			fineCounter++;
		}
		else if (coarseCounter < coarseProviders.size())
		{
			startTimerForProvider(coarseProviders.get(coarseCounter), coarseListener);
			coarseCounter++;
		}
		else
		{
			if (!foundLocation)
			{
				locationCallback.onLocationUpdateError();
			}
		}
	}

	@Override
	public void onTimeout(final TimeoutTimer timer)
	{
		if (locationCallback == null)
		{
			return;
		}
		endTimerForProvider(timer.getId(), (LocationListener) timer.getTag());
		updateFromLocationListener();
		
		Utils.d(TAG, "Location timeout for provider " + timer.getId() + "---" + Thread.currentThread());
	}
	
	private void requestReverseGeocoding(Location location)
	{
		addressUpdater = (AddressUpdater) new AddressUpdater().execute(location);
		Utils.d(TAG, "Started reverse geocoding location " + location);
	}
	
	// Location doesn't override equals() or hashCode(), so we use latitude and longitude as key
	public static void addToCache(Location location, String address)
	{
		// make some room by deleting old addresses
		if (cache.size() > 0 && cache.size() == MAX_CACHE_SIZE)
			cache.remove(cache.keySet().iterator().next());
		
		cache.put(location.getLatitude() + "-" + location.getLongitude(), address);
		Utils.d(TAG, "Cached address " + address + "  for location " + location);
	}
	
	public static String getFromCache(Location location)
	{
		return cache.get(location.getLatitude() + "-" + location.getLongitude());
	}

	public static Criteria createCriteria(int accuracy)
	{
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

	public static interface LocationUpdaterCallback
	{
		void onSuggestLocationSettings();
		
		void onLocationUpdate(Location location);
		
		void onLocationUpdateError();
	}
	
	public static interface AddressUpdaterCallback
	{
		void onAddressUpdate(String address);
		
		void onAddressUpdateError();
	}
	
	private class AddressUpdater extends AsyncTask<Location, Void, String>
	{
		private Location location;
		
		@Override
		protected String doInBackground(Location... params)
		{
			if (params == null || params.length == 0) {
				Utils.d(TAG, "You must pass the location parameter!");
				return null;
			}
			
			location = params[0];
			try
			{
				List<Address> addresses = new Geocoder(context).getFromLocation(location.getLatitude(),
						location.getLongitude(), 1);
				String address = getFullAddress(addresses);
				
				if(!TextUtils.isEmpty(address)) {
					addToCache(location, address);
				}
				return address;
				
			}
			catch (IOException e)
			{
				Utils.d(TAG, "Could not reverse geocoding to get the address from location " + location);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(String address)
		{
			Utils.d(TAG, "The reverse geocoding found address: " + address);
			if (address != null)
			{
				addressCallback.onAddressUpdate(address);
				Utils.d(TAG, "Address update success: " + address);
			}
			else
			{
				addressCallback.onAddressUpdateError();
				Utils.d(TAG, "Address update failure!");
			}
			
			addressUpdater = null;
		}
		
		private String getFullAddress(List<Address> addresses)
		{
			StringBuilder fullAddress = new StringBuilder();
			
			if (addresses != null && !addresses.isEmpty())
			{
				Address address = addresses.get(0);
				
				for (int i = 0; i < address.getMaxAddressLineIndex(); ++i)
				{
					fullAddress.append(address.getAddressLine(i));
					fullAddress.append(", ");
				}
				fullAddress.append(address.getAddressLine(address.getMaxAddressLineIndex()));
			}
			
			return fullAddress.toString();
		}
	}
}