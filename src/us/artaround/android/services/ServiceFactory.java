package us.artaround.android.services;

import us.artaround.R;
import us.artaround.android.common.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.models.City;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.google.android.maps.GeoPoint;

public class ServiceFactory {
	private static SharedPreferences sharedPrefs;
	private static City[] cities;
	private static ArtService artService;

	private ServiceFactory() {} // singleton

	/**
	 * The application context needs to be set the first time we enter the
	 * application, for example in the main activity.
	 * 
	 * @param context
	 */
	public static void init(Context context) {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		Resources resources = context.getResources();
		int[] cityCodes = resources.getIntArray(R.array.city_codes);
		String[] cityNames = resources.getStringArray(R.array.city_names);
		String[] cityCenters = resources.getStringArray(R.array.city_centers);
		String[] cityServers = resources.getStringArray(R.array.city_servers);

		cities = new City[cityCodes.length];
		for (int i = 0; i < cityCodes.length; i++) {
			String[] tmp = cityCenters[i].split(Utils.STR_SEP);
			cities[i] = new City(cityNames[i], cityCodes[i], new GeoPoint(Integer.parseInt(tmp[0]),
					Integer.parseInt(tmp[1])), cityServers[i]);
		}

		artService = new ArtService(cities[Utils.DEFAULT_CITY_CODE]);

		FlickrService.init(context);
	}

	/**
	 * Get an instance of the {@link ArtService} for the current country and/or
	 * city, as set by the user in the preferences.
	 * 
	 * @return
	 * @throws ArtAroundException
	 */
	public static ArtService getArtService() {
		artService.setCity(getCurrentCity());
		return artService;
	}


	public static City getCurrentCity() {
		return cities[sharedPrefs.getInt(Utils.KEY_CITY_CODE, Utils.DEFAULT_CITY_CODE)];
	}

	public static City[] getCities() {
		return cities;
	}
}
