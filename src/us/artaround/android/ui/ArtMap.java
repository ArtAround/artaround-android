package us.artaround.android.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.commons.LoadArtCommand;
import us.artaround.android.commons.LoadingTask;
import us.artaround.android.commons.LoadingTask.LoadingTaskCallback;
import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.navigation.Road;
import us.artaround.android.commons.navigation.RoadProvider;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.models.City;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements OverlayTapListener, ZoomListener, LocationUpdaterCallback,
		LoadingTaskCallback<ParseResult>, NotifyingAsyncQueryListener {

	//--- loading tasks constants ---
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int ARTS_PER_PAGE = 100;

	//--- zoom constants ---
	public static final int ZOOM_DEFAULT_LEVEL = 11;
	private static final int[] MAX_PINS_PER_ZOOM = { 3, 5, 10, 30, 40, 60 };
	private static final int ZOOM_MIN_LEVEL = 8;
	private static final int ZOOM_MAX_LEVEL = ZOOM_MIN_LEVEL + MAX_PINS_PER_ZOOM.length;

	//--- activity requests ids ---
	public static final int REQUEST_FILTER = 0;

	//--- dialog ids ---
	private static final int DIALOG_ART_INFO = 0;
	private static final int DIALOG_LOCATION_SETTINGS = 1;
	private static final int DIALOG_WIFI_FAIL = 2;
	private static final int DIALOG_LOADING = 3;

	private ArtMapView mapView;
	private CurrentOverlay currentOverlay;
	private ArtBalloonsOverlay artOverlay;
	private HashMap<Art, OverlayItem> items;

	private LoadingButton btnAdd, btnLoading;
	private Button btnNearby, btnFilter;

	private ArrayList<Art> allArt;
	private ArrayList<Art> filteredArt;
	private int nrPinsToDisplayAtThisZoomLevel;
	private HashMap<Integer, HashSet<String>> filters;

	private int newZoom;
	private boolean toBeRessurected;
	private AtomicBoolean isLoadingArt;

	private City city;

	private NotifyingAsyncQueryHandler queryHandler;
	private LocationUpdater locationUpdater;
	private Location location;

	private Map<Integer, LoadingTask<ParseResult>> runningTasks;
	private AtomicInteger tasksCount;
	private AtomicInteger tasksLeft;

	private Road road;

	//FIXME make sure the give pin is not removed by the zooming filter
	//TODO compute the correct zoom to display all the road
	private Runnable updateRoadRunner = new Runnable() {
		@Override
		public void run() {
			RoadOverlay roadOverlay = new RoadOverlay(road, mapView);
			GeoPoint middle = roadOverlay.getMoveTo();

			List<Overlay> ov = mapView.getOverlays();
			ov.clear();
			ov.add(roadOverlay);
			ov.add(artOverlay);
			ov.add(currentOverlay);

			mapView.getController().animateTo(middle);
			mapView.getController().setZoom(mapView.getMaxZoomLevel() - 2); //FIXME magic number
			mapView.invalidate();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		initVars();
		restoreState();
		setupUi();
		handleIntent(getIntent());

		// Google Maps needs WIFI enabled!!
		//checkWifiStatus();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupState();
		handleIntent(getIntent());
	}

	private void changeCity() {
		City current = ServiceFactory.getCurrentCity();

		if (current != city) {
			taskCleanup();
			showLoading(false);

			allArt.clear();
			clearPins();

			city = ServiceFactory.getCurrentCity();
			centerMapOnCurrentLocation();
		}
	}

	private void setupState() {
		Utils.d(Utils.TAG, "Setup state...");

		changeCity();


		if (isLoadingFromServer()) {
			showLoading(true);
			attachTasksCallback();
		}
		else {
			if (Utils.isCacheOutdated(this)) {
				allArt.clear();
				clearPins();
			}

			if (allArt.isEmpty()) {
				loadArt();
			}
			else {
				showLoading(false);
				displayArt(filteredArt);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		endLocationUpdate();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (!toBeRessurected) {
			taskCleanup();
		}
	}

	private void taskCleanup() {
		Utils.d(Utils.TAG, "Stopping all current tasks.");

		Iterator<LoadingTask<ParseResult>> it = runningTasks.values().iterator();
		while (it.hasNext()) {
			LoadingTask<ParseResult> task = it.next();
			task.cancel(true);
			it.remove();
		}
		tasksLeft.set(0);
		tasksCount.set(0);
	}

	private void handleIntent(Intent intent) {
		if (intent.hasExtra("toLat") && intent.hasExtra("toLong")) {

			final float toLat = intent.getFloatExtra("toLat", 0);
			final float toLong = intent.getFloatExtra("toLong", 0);
			final float fromLat = (float) (location != null ? location.getLatitude() : city.center.getLatitudeE6()
					/ Utils.E6);
			final float fromLong = (float) (location != null ? location.getLongitude() : city.center.getLongitudeE6()
					/ Utils.E6);

			new Thread() {
				@Override
				public void run() {
					String url = RoadProvider.getUrl(fromLat, fromLong, toLat, toLong);
					InputStream is = Utils.getConnection(url);
					road = RoadProvider.getRoad(is);
					try {
						is.close();
					}
					catch (IOException e) {
						// ignore
					}
					runOnUiThread(updateRoadRunner);
				}
			}.start();
		}
	}

	private void initVars() {
		toBeRessurected = false;

		ServiceFactory.init(getApplicationContext());
		city = ServiceFactory.getCurrentCity();

		locationUpdater = new LocationUpdater(this);
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
	}

	private void restoreState() {
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder == null) {
			newZoom = ZOOM_DEFAULT_LEVEL;
			allArt = new ArrayList<Art>();
			filteredArt = new ArrayList<Art>();
			filters = new HashMap<Integer, HashSet<String>>();
			tasksLeft = new AtomicInteger(0);
			tasksCount = new AtomicInteger(0);
			runningTasks = Collections.synchronizedMap(new HashMap<Integer, LoadingTask<ParseResult>>());
			isLoadingArt = new AtomicBoolean(false);
		}
		else {
			newZoom = holder.zoom;
			allArt = holder.allArt;
			filteredArt = holder.artFiltered;
			filters = holder.filters;
			tasksLeft = holder.tasksLeft;
			tasksCount = holder.tasksCount;
			runningTasks = holder.runningArtTasks;
			location = holder.location;
			isLoadingArt = holder.isLoadingArt;
		}

		if (holder == null) {
			checkWifiStatus(); // only once
		}
	}

	private void setupUi() {
		setupActionBarUi();
		setupFooterBarUi();
		setupMapUi();
	}

	private void setupActionBarUi() {
		btnLoading = (LoadingButton) findViewById(R.id.btn_1);
		btnLoading.setImageResource(R.drawable.ic_btn_refresh);
		btnLoading.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setupState();
				startLocationUpdate();
			}
		});

		btnAdd = (LoadingButton) findViewById(R.id.btn_2);
		btnAdd.setImageResource(R.drawable.ic_btn_add);
	}

	private void setupFooterBarUi() {
		btnNearby = (Button) findViewById(R.id.btn_nearby);
		btnNearby.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(ArtMap.this, ArtNearby.class).putExtra("arts", filteredArt));
			}
		});

		btnFilter = (Button) findViewById(R.id.btn_filter);
		btnFilter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(ArtMap.this, ArtFilters.class).putExtra("filters", filters),
						REQUEST_FILTER);
			}
		});
	}

	private void setupMapUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		mapView.setZoomLevel(newZoom);
		mapView.setZoomListener(this);

		items = new HashMap<Art, OverlayItem>();
		artOverlay = new ArtBalloonsOverlay(getResources().getDrawable(R.drawable.ic_pin), this, mapView);
		currentOverlay = new CurrentOverlay(this, R.drawable.ic_pin_current);

		List<Overlay> overlays = mapView.getOverlays();
		overlays.add(artOverlay);
		overlays.add(currentOverlay);

		centerMapOnCurrentLocation();
	}

	private void showLoading(boolean loading) {
		btnLoading.showLoading(loading);
		btnNearby.setEnabled(!loading);
		btnFilter.setEnabled(!loading);
	}

	private void clearPins() {
		artOverlay.doClear();
		artOverlay.doPopulate();
	}

	private void attachTasksCallback() {
		for (LoadingTask<ParseResult> task : runningTasks.values()) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.attachCallback(this); // pass the new context
			}
		}
	}

	private void detachTasksCallback() {
		for (LoadingTask<ParseResult> task : runningTasks.values()) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.detachCallback();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		toBeRessurected = true;

		Holder holder = new Holder();
		holder.runningArtTasks = runningTasks;
		holder.tasksLeft = tasksLeft;
		holder.tasksCount = tasksCount;
		holder.allArt = allArt;
		holder.artFiltered = filteredArt;
		holder.zoom = newZoom;
		holder.filters = filters;
		holder.location = location;
		holder.isLoadingArt = isLoadingArt;

		detachTasksCallback();

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
		case DIALOG_LOADING:
			ProgressDialog progress = new ProgressDialog(this);
			progress.setCancelable(false);
			progress.setMessage(getString(R.string.loading));
			return progress;
		}
		return builder.create();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_FILTER:
			filters = (HashMap<Integer, HashSet<String>>) data.getSerializableExtra("filters");

			filterArt(allArt);
			displayArt(filteredArt);

			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.default_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			startActivity(new Intent(this, Preferences.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onTap(Object item) {
		if (item instanceof ArtOverlayItem) {
			gotoArtPage(((ArtOverlayItem) item).art);
		}
		else if (item instanceof Overlay) {
			//TODO
		}
	}

	private void gotoArtPage(Art art) {
		// save the current state of the map
		startActivity(new Intent(this, ArtPage.class).putExtra("slug", art.slug));
		finish(); // call finish to save memory because of the 2 map views
	}

	private void onFinishLoadArt() {
		Utils.d(Utils.TAG, "Finished loading all art from the server.");

		doSaveArt();
		Utils.setLastCacheUpdate(this);

		showLoading(false);
		isLoadingArt.set(false);
	}

	private void doSaveArt() {
		int size = allArt.size();
		Utils.d(Utils.TAG, "Updating dispersion for " + size + " arts in the db.");

		for (int i = 0; i < size; i++) {
			Art art = allArt.get(i);
			queryHandler.startUpdate(ContentUris.withAppendedId(Arts.CONTENT_URI, art.uuid),
					ArtAroundDatabase.artToValues(art), Arts.SLUG + "=?", new String[] { art.slug });
		}
	}

	private void loadArt() {
		showLoading(true);
		isLoadingArt.set(true);

		loadArtFromDatabase();
	}

	private void loadArtFromDatabase() {
		Utils.d(Utils.TAG, "Starting querying for arts from db...");
		queryHandler.startQuery(-1, null, Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION,
				ArtAroundDatabase.ARTS_WHERE, new String[] { city.name }, null);
	}

	private boolean isLoadingFromServer() {
		return !runningTasks.isEmpty();
	}

	private void loadArtFromServer() {
		Utils.d(Utils.TAG, "Loading art from server...");

		if (isLoadingFromServer()) {
			return;
		}
		tasksCount.set(1);
		startTask(1);
	}

	private void loadMoreArtFromServer(ParseResult result, Integer token) {
		runningTasks.remove(token);

		if (result.page == 1) {
			tasksLeft.set((int) (Math.ceil((double) (result.totalCount - result.count) / (double) result.perPage)));

			int min = tasksLeft.get() < MAX_CONCURRENT_TASKS ? tasksLeft.get() : MAX_CONCURRENT_TASKS;
			for (int i = 0; i < min; i++) {
				startTask(tasksCount.incrementAndGet());
			}
		}
		else {
			tasksLeft.decrementAndGet();

			if (result.count == result.perPage && tasksLeft.get() > 0) {
				Utils.d(Utils.TAG, "There are " + tasksLeft.get() + " more tasks to start.");

				if (runningTasks.size() < MAX_CONCURRENT_TASKS) {
					startTask(tasksCount.incrementAndGet());
				}
			}
		}
	}

	private void startTask(int page) {
		LoadArtCommand comm = new LoadArtCommand(page, ARTS_PER_PAGE);
		LoadingTask<ParseResult> task = new LoadingTask<ParseResult>(this, comm);
		task.execute();
		runningTasks.put(comm.getToken(), task);
	}

	private void processLoadedArt(List<Art> art) {
		if (art != null && !art.isEmpty()) {
			allArt.addAll(art);
			calculateMaximumDispersion(allArt);
			filterArt(allArt);
			displayArt(filteredArt);
		}
	}

	private void gotoWifiSettings() {
		startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
	}

	private void gotoLocationSettings() {
		startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	}

	private void calculateMaximumDispersion(List<Art> art) {
		Utils.d(Utils.TAG, "Computing maxium dispersion for " + art.size() + " art objects.");
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
		//Collections.reverse(art);
	}

	private float distance(Art a, Art b) {
		return (float) Math.sqrt(Math.pow(a.latitude - b.latitude, 2) + Math.pow(a.longitude - b.longitude, 2));
	}

	private OverlayItem ensureOverlay(Art a) {
		if (items.containsKey(a)) {
			return items.get(a);
		}
		else {
			ArtOverlayItem pin = new ArtOverlayItem(a);
			items.put(a, pin);
			return pin;
		}
	}

	private void filterArt(List<Art> art) {
		if (art == null || art.size() == 0) {
			return;
		}

		int allNrPins = art.size();

		HashMap<Integer, List<String>> onFilters = filterByAll();
		Utils.d(Utils.TAG, "===== FILTERS: " + onFilters + " ===== ");

		filteredArt.clear();

		//filter
		for (int i = 0; i < allNrPins; ++i) {
			Art a = art.get(i);
			// FIXME create an ui property to change matching type to "all" or "at least one"
			if (artMatchesFilter(onFilters, a, false)) {
				filteredArt.add(a);
			}
		}
		Utils.d(Utils.TAG, "~~~ " + filteredArt.size() + " arts match the filters ~~~");
	}

	private void displayArt(List<Art> art) {
		calculateHowManyPinsToDisplay(art);
		artOverlay.doClear();
		int nrPins = Math.min(art.size(), this.nrPinsToDisplayAtThisZoomLevel);
		Utils.d(Utils.TAG, "Displaying " + nrPins + " arts");
		for (int i = 0; i < nrPins; ++i) {
			Art a = art.get(i);
			artOverlay.addOverlay(ensureOverlay(a));
		}
		artOverlay.doPopulate();
		mapView.invalidate();
	}

	private void calculateHowManyPinsToDisplay(List<Art> art) {
		if (art == null || art.size() == 0) {
			return;
		}
		// find out max number of pins to display based on zoom
		int allNrPins = art.size();

		if (newZoom <= ZOOM_MIN_LEVEL)
			nrPinsToDisplayAtThisZoomLevel = 1;
		else if (newZoom > ZOOM_MAX_LEVEL)
			nrPinsToDisplayAtThisZoomLevel = allNrPins;
		else
			nrPinsToDisplayAtThisZoomLevel = MAX_PINS_PER_ZOOM[newZoom - ZOOM_MIN_LEVEL - 1];
	}

	private boolean matchesFilter(List<String> byType, String prop) {
		if ((TextUtils.isEmpty(prop) && byType.contains(getString(R.string.value_unset)))) return true;
		return byType.contains(prop);
	}

	boolean artMatchesFilter(HashMap<Integer, List<String>> onFilters, Art a, boolean matchAll) {
		int match = 0;
		int length = ArtFilters.FILTER_NAMES.length;

		for (int i = 0; i < length; i++) {
			List<String> byType = onFilters.get(i);

			if (byType.isEmpty() || (i == ArtFilters.FILTER_CATEGORY && matchesFilter(byType, a.category))) {
				++match;
			}
			else if (byType.isEmpty() || (i == ArtFilters.FILTER_NEIGHBORHOOD && matchesFilter(byType, a.neighborhood))) {
				++match;
			}
			else if (byType.isEmpty()
					|| (i == ArtFilters.FILTER_ARTIST && a.artist != null && matchesFilter(byType, a.artist.name))) {
				++match;
			}
		}

		return matchAll ? match == ArtFilters.FILTER_NAMES.length : match > 0;
	}

	private HashMap<Integer, List<String>> filterByAll() {
		HashMap<Integer, List<String>> onFilters = new HashMap<Integer, List<String>>();

		int length = ArtFilters.FILTER_NAMES.length;

		for (int i = 0; i < length; i++) {
			onFilters.put(i, new ArrayList<String>());
		}

		for (int i = 0; i < length; i++) {
			HashSet<String> f = filters.get(i);
			if (f == null || f.isEmpty()) {
				continue;
			}

			for (String name : f) {
				onFilters.get(i).add(name);
			}
		}

		return onFilters;
	}

	@Override
	public void onZoom(int oldZoom, int newZoom) {
		this.newZoom = newZoom;
		Utils.d(Utils.TAG, "Zoom changed from " + oldZoom + " to " + newZoom);
		displayArt(filteredArt);
	}

	private void startLocationUpdate() {
		Utils.showToast(this, R.string.waiting_location);
		showLoading(true);
		btnNearby.setEnabled(false);
		locationUpdater.updateLocation();
	}

	private void endLocationUpdate() {
		showLoading(false);
		btnNearby.setEnabled(true);
		locationUpdater.removeUpdates();
	}

	private void checkWifiStatus() {
		ConnectivityManager mngr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mngr.getActiveNetworkInfo();

		if (info == null || (info != null && info.getState() != NetworkInfo.State.CONNECTED)) {
			showDialog(DIALOG_WIFI_FAIL);
		}
	}

	private void centerMapOnCurrentLocation() {
		final GeoPoint geo = location != null ? Utils.geo(location) : city.center;
		mapView.getController().animateTo(geo);
		currentOverlay.setGeoPoint(geo);
		Utils.d(Utils.TAG, "Centered map on " + geo);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.location = location;
		endLocationUpdate();
		centerMapOnCurrentLocation();
	}

	@Override
	public void onLocationUpdateError() {
		Log.w(Utils.TAG, "Timeout!");

		endLocationUpdate();
		Utils.showToast(this, R.string.location_update_failure);
	}

	@Override
	public void onSuggestLocationSettings() {
		Utils.d(Utils.TAG, "Suggest location settings...");
		showDialog(DIALOG_LOCATION_SETTINGS);
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (cursor == null) {
			showLoading(false);
			isLoadingArt.set(false);
			Log.w(Utils.TAG, "Something is wrong, returned art cursor is null!");
			return;
		}

		List<Art> results = ArtAroundDatabase.artsFromCursor(cursor);
		Utils.d(Utils.TAG, "Retrieved " + results + " arts from the db.");
		cursor.close();

		if (results.isEmpty()) {
			loadArtFromServer();
		}
		else {
			processLoadedArt(results);
			showLoading(false);
			isLoadingArt.set(false);
		}
	}

	private static class Holder {
		public AtomicBoolean isLoadingArt;
		public Location location;
		int zoom;
		Map<Integer, LoadingTask<ParseResult>> runningArtTasks;
		AtomicInteger tasksCount, tasksLeft;
		ArrayList<Art> allArt;
		ArrayList<Art> artFiltered;
		HashMap<Integer, HashSet<String>> filters;
	}

	@Override
	public void beforeLoadingTask(int token) {}

	@Override
	public void afterLoadingTask(int token, ParseResult result) {
		Utils.d(Utils.TAG, "Result of task is: " + result);

		if (result == null) {
			showDialog(DIALOG_WIFI_FAIL);
			showLoading(false);
			return;
		}

		processLoadedArt(result.art);

		if (result.totalCount == allArt.size()) {
			// finished to load all arts 
			onFinishLoadArt();
		}

		loadMoreArtFromServer(result, token);
	}

	@Override
	public void onLoadingTaskError(int token, ArtAroundException exception) {
		showLoading(false);
		isLoadingArt.set(false);
		taskCleanup();
		Utils.showToast(this, R.string.load_data_failure);
	}
}