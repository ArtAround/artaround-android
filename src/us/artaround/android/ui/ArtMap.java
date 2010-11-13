package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import us.artaround.android.ui.ArtOverlay.OverlayTapListener;
import us.artaround.models.Art;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.services.ParseResult;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements LoadArtCallback, OverlayTapListener, LocationListener, ZoomListener {
	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int PER_PAGE = 20;
	public static final int DEFAULT_ZOOM_LEVEL = 11;

	private static final int[] MAX_PINS_PER_LEVEL = { 3, 5, 10, 20, 30, 40, 60 };
	private static final int MIN_LEVEL = 8;
	private static final int MAX_LEVEL = 15;

	public static final int DIALOG_ART_INFO = 0;
	public static final int DIALOG_SUGGEST_LOCATION_SETTINGS = 1;
	public static final int DIALOG_LOCATION_UPDATE_FAILURE = 2;
	public static final int DIALOG_LOADING_ART_FAILURE = 3;

	public static final GeoPoint DEFAULT_LOCATION = new GeoPoint(38895111, -77036365); // Washington 

	private boolean initialized = false;
	private ArtMapView mapView;

	private List<Art> allArt;
	private List<Art> artFiltered;

	private int newZoom;

	private ArtOverlay artOverlay;
	private HashMap<Art, OverlayItem> items = new HashMap<Art, OverlayItem>();

	private SharedPreferences prefs;
	private GeoPoint currentLocation = DEFAULT_LOCATION;
	private LocationManager manager;
	private Database database;

	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicInteger howManyMoreTasks;
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

		doCreate();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!initialized) {
			doCreate();
		}
	}

	private void doCreate() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		database = Database.getInstance(this);
		
		Holder holder = (Holder) getLastNonConfigurationInstance();
		LoadArtTask lastTask = null;

		if (holder == null) {
			//if holder is null it means we got here from a fresh application start
			init();

		} else {
			//if we have a holder it means we got here from a screen flip
			restoreSavedConfig(holder);

			int maxPage = 1;
			for (LoadArtTask task : runningTasks) {
				task.setCallback(this); // pass the new context

				if (task.getPage() > maxPage) {
					lastTask = task;
					maxPage = task.getPage();
				}
			}

			if (saveArtTask != null && saveArtTask.getStatus() == AsyncTask.Status.RUNNING) {
				saveArtTask.setDatabase(database); // pass the new database connection
			}
		}

		setupUi();

		if (holder == null) {
			// first time; load fresh art
			loadArt();
		} else if (isLoadingArt()) {
			// continue with loading art
			try {
				loadMoreArtFromServer(lastTask.get(), lastTask);
			} catch (Exception e) {
				Log.e(Utils.TAG, "Could not continue loading arts from server!", e);
				showDialog(DIALOG_LOADING_ART_FAILURE);
			}
		} else {
			//just use the art we already have loaded
			displayLastArt();
		}
		initialized = true;
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
		holder.runningTasks = runningTasks;
		holder.howManyMoreTasks = howManyMoreTasks;
		holder.taskCount = taskCount;
		holder.allArt = allArt;
		holder.artFiltered = artFiltered;
		holder.initialized = initialized;
		holder.zoom = newZoom;
		holder.saveArtTask = saveArtTask;
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
					ArtMap.this.gotoLocationSettings();
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
		case DIALOG_LOADING_ART_FAILURE:
			builder.setCancelable(true);
			builder.setMessage(R.string.connection_fail_message);
			builder.setPositiveButton(R.string.connection_fail_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					ArtMap.this.gotoWifiSettings();
				}
			});
			builder.setNegativeButton(R.string.connection_fail_cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			break;
		}
		return builder.create();
	}

	@Override
	public void onTap(OverlayItem item) {
		Log.d(Utils.TAG, "Tapped!");
		if (item instanceof ArtOverlayItem) {
			gotoArtDetails(((ArtOverlayItem) item).art);
		}
	}

	private void gotoArtDetails(Art art) {
		Intent intent = new Intent(this, ArtDetails.class);
		intent.putExtra(Utils.KEY_ART_ID, art.slug);
		startActivity(intent);
	}

	@Override
	public void onLocationChanged(Location location) {
		currentLocation = Utils.geo(location.getLatitude(), location.getLongitude());
		manager.removeUpdates(this);
		// center map on current location
		mapView.getController().setCenter(currentLocation);
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
			showDialog(DIALOG_LOADING_ART_FAILURE);
			return;
		}

		processLoadedArt(result.art);

		if (result.totalCount == allArt.size()) {
			// finished to load all arts 
			onFinishLoadArt();
		}

		loadMoreArtFromServer(result, task);
	}
	
	@Override
	public void onLoadArtError(Throwable e) {
		Log.d(Utils.TAG, "Failed to load art!", e);
		showDialog(DIALOG_LOADING_ART_FAILURE);
	}

	private void init() {
		allArt = new ArrayList<Art>();
		artFiltered = new ArrayList<Art>();
		howManyMoreTasks = new AtomicInteger(0);
		taskCount = new AtomicInteger(0);
		runningTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());
		newZoom = DEFAULT_ZOOM_LEVEL;
	}

	private void restoreSavedConfig(Holder holder) {
		allArt = holder.allArt;
		artFiltered = holder.artFiltered;
		howManyMoreTasks = holder.howManyMoreTasks;
		taskCount = holder.taskCount;
		runningTasks = holder.runningTasks;
		newZoom = holder.zoom;
		initialized = holder.initialized;
		saveArtTask = holder.saveArtTask;
	}

	private void onFinishLoadArt() {
		Log.d(Utils.TAG, "Finished loading all art from the server. Triggering onFinishLoadArt actions.");
		recalculateMaximumDispersion();
		// clear database cache
		saveArtTask = (SaveArtTask) new SaveArtTask(allArt, database).execute();
		setLastUpdate();
	}

	private void setupUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		mapView.setZoomLevel(newZoom);
		mapView.setZoomListener(this);
		mapView.getController().setCenter(currentLocation);

		artOverlay = new ArtOverlay(getResources().getDrawable(R.drawable.ic_pin), this);
		mapView.getOverlays().add(artOverlay);

		View btnSearch = findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});
	}

	private void loadArt() {
		Date lastUpdate = getLastUpdate();
		this.allArt.clear();
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
			howManyMoreTasks.set((int)(Math.ceil((double) (result.totalCount - result.count) / (double) result.perPage)));

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
		new LoadArtTask(this, page, PER_PAGE).execute();
	}

	private boolean isOutdated(Date lastUpdate) {
		if (lastUpdate == null) {
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
		if (art != null && !art.isEmpty()) {
			allArt.addAll(art);
			calculateMaximumDispersion(art);
			filterAndDisplayArt(art);
		}
	}

	private void gotoWifiSettings() {
		//prepare for refresh when we come back
		initialized = false;
		//go to wireless settings
		startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
	}

	private void gotoLocationSettings() {
		startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	}

	private void calculateMaximumDispersion(List<Art> art) {
		//Log.d(Utils.TAG, "Computing maxium dispersion for " + art.size() + " art objects.");
		final int allS = art.size();
		for (int i = 0; i < allS; ++i) {
			art.get(i).mediumDistance = 0;
		}
		for (int i = 0; i < allS; ++i) {
			Art a = art.get(i);
			for (int j = i + 1; j < allS; ++j) {
				Art b = art.get(j);
				float dist = distance(a, b);
				a.mediumDistance += dist;
				b.mediumDistance += dist;
			}
		}
		for (int i = 0; i < allS; ++i) {
			art.get(i).mediumDistance /= allS;
		}
		Collections.sort(art, new ArtDispersionComparator());
		Collections.reverse(art);
	}

	private void recalculateMaximumDispersion() {
		if (allArt.size() > PER_PAGE) { //only need to do this if we have more than one page
			//Log.d(Utils.TAG, "Computing maxium dispersion for all art (" + allArt.size() + ").");
			calculateMaximumDispersion(allArt);
		}
	}

	private float distance(Art a, Art b) {
		return (float) Math.sqrt(Math.pow(a.latitude - b.latitude, 2) + Math.pow(a.longitude - b.longitude, 2));
	}

	private void filterAndDisplayArt(List<Art> art) {
		if (art != null && art.size() > 0) {
			displayArt(filterArt(art));
		}
	}

	private void filterAndDisplayAllArt() {
		filterAndDisplayArt(allArt);
	}

	private void displayLastArt() {
		RenderingContext context = new RenderingContext(artFiltered);
		context.artToAdd.addAll(artFiltered);
		displayArt(context);
	}

	private void displayArt(RenderingContext art) {
		//remove art
		//Log.d(Utils.TAG, "Removing " + art.artToRemove.size() + " pins.");
		for (Art a : art.artToRemove) {
			artOverlay.removeOverlay(items.get(a));
		}
		//add new art
		//Log.d(Utils.TAG, "Adding " + art.artToAdd.size() + " pins.");
		for (Art a : art.artToAdd) {
			artOverlay.addOverlay(newOverlay(a));
		}
		//redraw
		artOverlay.doPopulate();
		mapView.invalidate();
	}

	private OverlayItem newOverlay(Art a) {
		if (items.containsKey(a)) {
			return items.get(a);
		} else {
			ArtOverlayItem pin = new ArtOverlayItem(a);
			items.put(a, pin);
			return pin;
		}
	}

	private RenderingContext filterArt(List<Art> art) {
		return filterByZoom(filterByCategories(new RenderingContext(art)));
	}

	private RenderingContext filterByZoom(RenderingContext context) {
		int newNrPins = 0;
		List<Art> art = context.art;
		List<Art> artToAdd = context.artToAdd;
		List<Art> artToRemove = context.artToRemove;

		if (newZoom <= MIN_LEVEL) {
			newNrPins = 1;
		} else if (newZoom > MAX_LEVEL) {
			newNrPins = art.size();
		} else {
			newNrPins = MAX_PINS_PER_LEVEL[newZoom - MIN_LEVEL - 1];
		}
		//Log.d(Utils.TAG, "There are " + newNrPins + " new pins.");

		int oldNrPins = artFiltered.size();

		if (newNrPins > oldNrPins) { //must add pins
			for (int i = oldNrPins; i < newNrPins; ++i) {
				artToAdd.add(art.get(i));
			}
			artFiltered.addAll(artToAdd);
		} else { //must remove pins
			for (int i = oldNrPins - 1; i >= newNrPins; --i) {
				artToRemove.add(artFiltered.get(i));
				artFiltered.remove(i);
			}
		}
		return context;
	}

	private RenderingContext filterByCategories(RenderingContext context) {
		return context;
	}

	@Override
	public void onZoom(int oldZoom, int newZoom) {
		//Log.d(Utils.TAG, "Zoom changed from " + oldZoom + " to " + newZoom);
		//this.oldZoom = oldZoom;
		this.newZoom = newZoom;
		filterAndDisplayAllArt();
	}

	private void updateLocation() {
		// step 1: suggest the user to enable gps and network providers
		suggestLocationSettings();

		// step 2: try the last known location
		Location l = fromStoredLocation();
		currentLocation = Utils.geo(l.getLatitude(), l.getLongitude());

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

	public static class RenderingContext {
		public RenderingContext(List<Art> art) {
			this.art.addAll(art);
		}
		public final List<Art> art = new ArrayList<Art>();
		public final List<Art> artToRemove = new ArrayList<Art>();
		public final List<Art> artToAdd = new ArrayList<Art>();
	}

	private static class Holder {
		SaveArtTask saveArtTask;
		Set<LoadArtTask> runningTasks;
		AtomicInteger taskCount;
		AtomicInteger howManyMoreTasks;
		List<Art> allArt;
		List<Art> artFiltered;
		int zoom;
		boolean initialized;
	}
}