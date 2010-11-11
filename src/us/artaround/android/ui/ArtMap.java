package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.commons.Database;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.tasks.LoadArtTask;
import us.artaround.android.commons.tasks.SaveArtTask;
import us.artaround.android.commons.tasks.LoadArtTask.LoadArtCallback;
import us.artaround.android.ui.ArtItemOverlay.OverlayTapListener;
import us.artaround.models.Art;
import us.artaround.services.ParseResult;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements LoadArtCallback, OverlayTapListener,
		LocationListener {
	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 3;
	public static final int PER_PAGE = 20;

	public static final int DIALOG_ART_INFO = 0;
	public static final int DIALOG_SUGGEST_LOCATION_SETTINGS = 1;
	public static final int DIALOG_LOCATION_UPDATE_FAILURE = 2;

	private MapView mapView;
	private ArtItemOverlay artOverlay;

	private SharedPreferences prefs;
	private Location currentLocation;
	private LocationManager manager;
	private Database database;

	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicInteger howManyMoreTasks;
	private List<Art> arts;

	private SaveArtTask saveArtTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		Intent intent = getIntent();

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMySearch(query);
		}

		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			arts = holder.arts;
			howManyMoreTasks = holder.howManyMoreTasks;
			taskCount = holder.taskCount;
			runningTasks = holder.runningTasks;
		} else {
			arts = new ArrayList<Art>();
			howManyMoreTasks = new AtomicInteger(0);
			taskCount = new AtomicInteger(0);
			runningTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		database = Database.getInstance(this);

		setupUi();
		updateLocation();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		manager.removeUpdates(this);
		database.close();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder holder = new Holder();
		holder.saveArtTask = saveArtTask;
		holder.runningTasks = runningTasks;
		holder.howManyMoreTasks = howManyMoreTasks;
		holder.taskCount = taskCount;
		holder.arts = arts;
		return holder;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_ART_INFO:
			break;
		case DIALOG_SUGGEST_LOCATION_SETTINGS:
			builder.setTitle(getString(R.string.location_suggest_settings_title));
			builder.setMessage(getString(R.string.location_suggest_settings_msg));
			builder.setCancelable(true);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					doUpdatePrefs();
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				}
			});
			builder.setNegativeButton(getString(R.string.skip), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					doUpdatePrefs();
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					doUpdatePrefs();
				}
			});
			break;
		}
		return builder.create();
	}

	@Override
	public void onTap(OverlayItem item) {
		Log.d(Utils.TAG, "Tapped!");
	}

	@Override
	public void onLocationChanged(Location location) {
		currentLocation = location;
		manager.removeUpdates(this);
		// center map on current location
		mapView.getController().setCenter(Utils.geo(currentLocation.getLatitude(), currentLocation.getLongitude()));
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	@Override
	public void onProviderEnabled(String provider) {}
	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onLoadArt(ParseResult result, LoadArtTask task) {
		Log.d(Utils.TAG, "Result of task is: " + result);

		if (result == null) {
			// TODO show dialog with failure message
			return;
		}

		processLoadedArt(result.art);

		if (result.totalCount == arts.size()) {
			// finished to load all arts; clear database cache
			new SaveArtTask().execute(database, arts);

			setLastUpdate();
			
		} else {
			Log.d(Utils.TAG, "Loading more art from server...");
			loadMoreArtFromServer(result, task);
		}
	}

	private void setupUi() {
		mapView = (MapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		artOverlay = new ArtItemOverlay(getResources().getDrawable(R.drawable.ic_pin), this);
		mapView.getOverlays().add(artOverlay);

		View btnSearch = findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});

		loadArt();
	}

	private void loadArt() {
		Date lastUpdate = getLastUpdate();
		Log.d(Utils.TAG, "Last art update was " + lastUpdate);

		if (isOutdated(lastUpdate)) {
			loadArtFromServer();
		} else {
			loadArtFromDatabase();
		}
	}
	
	private void loadArtFromDatabase() {
		processLoadedArt(database.getArts());
	}

	private boolean isLoadingArt() {
		return !runningTasks.isEmpty();
	}

	private void loadArtFromServer() {
		Log.d(Utils.TAG, "Loading art from server...");
		if (isLoadingArt()) {
			return;
		}

		taskCount.set(1);
		startTask(1);
	}

	private void loadMoreArtFromServer(ParseResult result, LoadArtTask task) {
		runningTasks.remove(task);

		if (result.page == 1) {
			howManyMoreTasks.set((int) (Math
					.ceil((double) (result.totalCount - result.count) / (double) result.perPage)));

			int min = howManyMoreTasks.get() < MAX_CONCURRENT_TASKS ? howManyMoreTasks.get() : MAX_CONCURRENT_TASKS;
			for (int i = 0; i < min; i++) {
				startTask(taskCount.incrementAndGet());
			}
		} else {
			howManyMoreTasks.decrementAndGet();

			if (result.count == result.perPage && howManyMoreTasks.get() > 0) {
				Log.d(Utils.TAG, "There are " + howManyMoreTasks.get() + " more tasks to start.");

				if (runningTasks.size() < MAX_CONCURRENT_TASKS) {
					startTask(taskCount.incrementAndGet());
				}
			}
		}
	}

	private void startTask(int page) {
		new LoadArtTask(this).execute(PER_PAGE, page);
	}

	private boolean isOutdated(Date lastUpdate) {
		if(lastUpdate == null) {
			return true;
		}
		return (new Date().getTime() - lastUpdate.getTime()) > prefs.getLong(Utils.KEY_UPDATE_INTERVAL,
				DEFAULT_UPDATE_INTERVAL);
	}

	private Date getLastUpdate() {
		long time = prefs.getLong(Utils.KEY_LAST_UPDATE, 0);
		if (time == 0) {
			return null;
		}
		return new Date(time);
	}

	private void setLastUpdate() {
		prefs.edit().putLong(Utils.KEY_LAST_UPDATE, new Date().getTime()).commit();
	}

	private void processLoadedArt(List<Art> art) {
		if (art == null || art.isEmpty()) return;
		arts.addAll(art);
		showArt(filterArt(art));
	}

	private List<Art> filterArt(List<Art> art) {
		/* String categoriesStr = prefs.getString(Utils.KEY_CATEGORIES, null);

		if (!TextUtils.isEmpty(categoriesStr)) {
			ArrayList<Art> result = new ArrayList<Art>();
			String[] categories = TextUtils.split(categoriesStr, Utils.STR_SEP);

			for (int i = 0; i < art.size(); i++) {
				Art a = art.get(i);
				for (int j = 0; j < categories.length; j++) {
					if (a.category.equals(categories[j])) {
						result.add(a);
						break;
					}
				}
			}
			return result;
		}
		return null; */
		return art;
	}

	private void showArt(List<Art> art) {
		if (art == null || art.isEmpty()) return;

		for (int i = 0; i < art.size(); i++) {
			Art a = art.get(i);
			OverlayItem pin = new OverlayItem(Utils.geo(a.latitude, a.longitude), a.title, a.locationDesc);
			artOverlay.addOverlay(pin);
		}
		// re-draw map view
		artOverlay.doPopulate();
		mapView.invalidate();
	}

	private void updateLocation() {
		// step 1: suggest the user to enable gps and network providers
		suggestLocationSettings();

		// step 2: try the last known location
		currentLocation = fromStoredLocation();

		if (currentLocation != null) {
			return;
		}

		// step 3: request an update from enabled providers
		if (!fromLocationListener()) {
			showDialog(DIALOG_LOCATION_UPDATE_FAILURE);
		}
	}

	private void suggestLocationSettings() {
		// don't bother the user if he already has chosen not to
		if (!prefs.getBoolean(Utils.KEY_SUGGEST_LOCATION_SETTINGS, true)) {
			return;
		}

		int count = 0;
		for (int i = 0; i < Utils.PROVIDERS.length; i++) {
			if (manager.isProviderEnabled(Utils.PROVIDERS[i])) {
				count++;
			}
		}
		if (count < Utils.PROVIDERS.length) {
			// safeguard against non-existing activity
			PackageManager manager = getPackageManager();
			ResolveInfo info = manager.resolveActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
					PackageManager.GET_INTENT_FILTERS);
			IntentFilter filter = info.filter;
			if (filter != null && filter.hasAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) {
				showDialog(DIALOG_SUGGEST_LOCATION_SETTINGS);
			}
		}
	}

	private Location fromStoredLocation() {
		Location location = null;
		for (int i = 0; i < Utils.PROVIDERS.length; i++) {
			location = manager.getLastKnownLocation(Utils.PROVIDERS[i]);
			if (location != null) {
				break;
			}
		}
		return location;
	}

	private boolean fromLocationListener() {
		boolean enabled = false;
		for (int i = 0; i < Utils.PROVIDERS.length; i++) {
			if (manager.isProviderEnabled(Utils.PROVIDERS[i])) {
				manager.requestLocationUpdates(Utils.PROVIDERS[i], 0, 0, this);
				enabled = true;
				break;
			}
		}
		return enabled;
	}

	private void doUpdatePrefs() {
		prefs.edit().putBoolean(Utils.KEY_SUGGEST_LOCATION_SETTINGS, false).commit();
	}

	private void doMySearch(String query) {
		// TODO Auto-generated method stub

	}

	private static class Holder {
		SaveArtTask saveArtTask;
		Set<LoadArtTask> runningTasks;
		AtomicInteger taskCount;
		AtomicInteger howManyMoreTasks;
		List<Art> arts;
	}
}