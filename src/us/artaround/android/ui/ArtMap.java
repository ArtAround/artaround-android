package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.commons.Database;
import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
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
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements LoadArtCallback, OverlayTapListener, ZoomListener,
		LocationUpdaterCallback {
	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int PER_PAGE = 20;
	public static final int DEFAULT_ZOOM_LEVEL = 11;

	private static final int[] MAX_PINS_PER_LEVEL = { 3, 5, 10, 20, 30, 40, 60 };
	private static final int MIN_LEVEL = 8;
	private static final int MAX_LEVEL = 15;

	public static final int DIALOG_ART_INFO = 0;
	public static final int DIALOG_LOCATION_SETTINGS = 1;
	public static final int DIALOG_WIFI_FAIL = 2;

	public static final GeoPoint DEFAULT_GEOPOINT = new GeoPoint(38895111, -77036365); // Washington 

	private ArtOverlay artOverlay;
	private HashMap<Art, OverlayItem> items;

	private ArtMapView mapView;
	private ProgressBar loading;
	private ImageButton btnLocation;

	private List<Art> allArt;
	private List<Art> artFiltered;

	private int newZoom;
	private boolean toBeRessurected;

	private ConnectivityManager connectivityManager;
	private Location currentLocation;
	private LocationUpdater locationUpdater;

	private SharedPreferences prefs;
	private Database database;

	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicInteger howManyMoreTasks;
	private SaveArtTask saveArtTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		initVars((Holder) getLastNonConfigurationInstance());
		setupUi();

		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMySearch(query);
		}
	}

	private void initVars(Holder holder) {
		toBeRessurected = false;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		locationUpdater = new LocationUpdater(this, this);
		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		database = Database.getInstance(this);

		if (holder == null) {
			newZoom = DEFAULT_ZOOM_LEVEL;

			allArt = new ArrayList<Art>();
			artFiltered = new ArrayList<Art>();

			howManyMoreTasks = new AtomicInteger(0);
			taskCount = new AtomicInteger(0);
			runningTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());

			currentLocation = null;
		}
		else {
			Log.d(Utils.TAG, "--Restoring from holder after flipping the screen--");
			newZoom = holder.zoom;

			allArt = holder.allArt;
			artFiltered = holder.artFiltered;

			howManyMoreTasks = holder.howManyMoreTasks;
			taskCount = holder.taskCount;
			runningTasks = holder.runningTasks;
			saveArtTask = holder.saveArtTask;

			currentLocation = holder.currentLocation;
		}
	}

	private void setupUi() {
		setupActionBarUi();
		setupMapUi();
	}

	private void setupMapUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		mapView.setZoomLevel(newZoom);
		mapView.setZoomListener(this);

		items = new HashMap<Art, OverlayItem>();
		artOverlay = new ArtOverlay(getResources().getDrawable(R.drawable.ic_pin), this);
		mapView.getOverlays().add(artOverlay);

		centerMapOnCurrentLocation();
	}

	private void setupActionBarUi() {
		btnLocation = (ImageButton) findViewById(R.id.btn_location);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		ImageButton btnSearch = (ImageButton) findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});
	}

	private void showLoading() {
		if (loading == null) {
			loading = (ProgressBar) findViewById(R.id.spinner);
		}
		loading.setVisibility(View.VISIBLE);
	}

	private void hideLoading() {
		if (loading == null) return;
		loading.setVisibility(View.GONE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupState();
	}

	private void setupState() {
		Log.d(Utils.TAG, "Setup state...");

		if (allArt.isEmpty()) {
			if (!isLoadingArt()) { // first time in the app
				loadArt();
			}
			else {
				showLoading();
				setTasksNewCallback(); // screen-flip while loading first page
			}
		}
		else {
			if (!isLoadingArt()) {
				displayLastArt(); // screen-flip after loading all art
			}
			else {
				showLoading();
				setTasksNewCallback(); // screen-flip while loading
			}
		}

		if (isSavingArt()) {
			saveArtTask.setDatabase(database);
		}

		// Google Maps needs WIFI enabled!!
		checkWifiStatus();

		//FIXME un-comment this for production
		//if (currentLocation == null) {
		//updateLocation();
		//}
	}

	private void setTasksNewCallback() {
		for (LoadArtTask task : runningTasks) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.setCallback(this); // pass the new context
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.d(Utils.TAG, "--Saving holder before flipping the screen--");
		toBeRessurected = true;

		Holder holder = new Holder();
		holder.runningTasks = runningTasks;
		holder.howManyMoreTasks = howManyMoreTasks;
		holder.taskCount = taskCount;
		holder.allArt = allArt;
		holder.artFiltered = artFiltered;
		holder.zoom = newZoom;
		holder.saveArtTask = saveArtTask;
		return holder;
	}

	@Override
	protected void onPause() {
		super.onPause();
		cleanup();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (!toBeRessurected) {
			finalCleanup();
		}
	}

	private void cleanup() {
		endLocationUpdate();
		database.close();
	}

	private void finalCleanup() {
		Log.d(Utils.TAG, "Final cleanup before onDestroy(): stopping tasks...");

		Iterator<LoadArtTask> it = runningTasks.iterator();
		while (it.hasNext()) {
			LoadArtTask task = it.next();
			task.cancel(true);
			it.remove();
		}
		howManyMoreTasks.set(0);
		taskCount.set(0);

		if (isSavingArt()) {
			saveArtTask.cancel(true);
		}
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
		case DIALOG_LOCATION_SETTINGS:
			builder.setTitle(getString(R.string.location_suggest_settings_title));
			builder.setMessage(getString(R.string.location_suggest_settings_msg));
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					gotoLocationSettings();
				}
			});
			builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			break;
		case DIALOG_WIFI_FAIL:
			builder.setTitle(R.string.connection_failure_title);
			builder.setMessage(R.string.connection_failure_msg);
			builder.setPositiveButton(R.string.connection_failure_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					gotoWifiSettings();
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
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
	public void onLoadArt(ParseResult result, LoadArtTask task) {
		Log.d(Utils.TAG, "Result of task is: " + result);

		if (result == null) {
			showDialog(DIALOG_WIFI_FAIL);
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
		hideLoading();
		showDialog(DIALOG_WIFI_FAIL);
	}

	private void onFinishLoadArt() {
		Log.d(Utils.TAG, "Finished loading all art from the server. Triggering onFinishLoadArt actions.");
		recalculateMaximumDispersion();
		// update database cache
		saveArtTask = (SaveArtTask) new SaveArtTask(allArt, database).execute();
		setLastCacheUpdate();
		hideLoading();
	}

	private void loadArt() {
		Date lastUpdate = getLastCacheUpdate();
		Log.d(Utils.TAG, "Last art update was " + lastUpdate);

		showLoading();

		if (isCacheOutdated(lastUpdate)) {
			loadArtFromServer();
		} else {
			loadArtFromDatabase();
		}
	}

	private void loadArtFromDatabase() {
		processLoadedArt(database.getArts());
		hideLoading();
	}

	private boolean isLoadingArt() {
		return !runningTasks.isEmpty();
	}

	private boolean isSavingArt() {
		return saveArtTask != null;
	}

	private void loadArtFromServer() {
		Log.d(Utils.TAG, "Loading art from server...");

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
		LoadArtTask task = new LoadArtTask(this, page, PER_PAGE);
		task.execute();
		runningTasks.add(task);
	}

	private boolean isCacheOutdated(Date lastUpdate) {
		if (lastUpdate == null) {
			return true;
		}
		return (new Date().getTime() - lastUpdate.getTime()) > prefs.getLong(Utils.KEY_UPDATE_INTERVAL,
				DEFAULT_UPDATE_INTERVAL);
	}

	private Date getLastCacheUpdate() {
		long time = prefs.getLong(Utils.KEY_LAST_UPDATE, 0);
		if (time == 0) {
			return null;
		}
		return new Date(time);
	}

	private void setLastCacheUpdate() {
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

	private void startLocationUpdate() {
		Log.d(Utils.TAG, "Start location update...");
		showLoading();
		btnLocation.setEnabled(false);
		locationUpdater.updateLocation();
	}

	private void endLocationUpdate() {
		Log.d(Utils.TAG, "End location update...");

		hideLoading();
		btnLocation.setEnabled(true);
		locationUpdater.removeUpdates();
	}

	private void checkWifiStatus() {
		NetworkInfo info = connectivityManager.getActiveNetworkInfo();

		if (info == null || (info != null && info.getState() != NetworkInfo.State.CONNECTED)) {
			Log.w(Utils.TAG, "No network connection!");
			showDialog(DIALOG_WIFI_FAIL);
		}
	}

	private void centerMapOnCurrentLocation() {
		final GeoPoint geo = currentLocation == null ? DEFAULT_GEOPOINT : Utils.geo(currentLocation);
		mapView.getController().animateTo(geo);
		Log.d(Utils.TAG, "Centered map on " + geo);
	}

	private void doMySearch(String query) {
		// TODO Auto-generated method stub
	}

	private void showToast(int msgId) {
		Toast.makeText(getApplicationContext(), msgId, Toast.LENGTH_LONG).show();
	}

	private static class RenderingContext {
		public RenderingContext(List<Art> art) {
			this.art.addAll(art);
		}
		public final List<Art> art = new ArrayList<Art>();
		public final List<Art> artToRemove = new ArrayList<Art>();
		public final List<Art> artToAdd = new ArrayList<Art>();
	}

	private static class Holder {
		int zoom;
		SaveArtTask saveArtTask;
		Set<LoadArtTask> runningTasks;
		AtomicInteger taskCount;
		AtomicInteger howManyMoreTasks;
		List<Art> allArt;
		List<Art> artFiltered;
		Location currentLocation;
	}

	@Override
	public void onLocationUpdate(Location location) {
		Log.d(Utils.TAG, "New location available from listener " + location);

		currentLocation = location;
		endLocationUpdate();
		centerMapOnCurrentLocation();
	}

	@Override
	public void onLocationUpdateError() {
		Log.w(Utils.TAG, "Timeout!");

		endLocationUpdate();
		showToast(R.string.location_update_failure);
	}

	@Override
	public void onSuggestLocationSettings() {
		Log.d(Utils.TAG, "Suggest location settings...");
		showDialog(DIALOG_LOCATION_SETTINGS);
	}
}