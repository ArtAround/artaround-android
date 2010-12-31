package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.AsyncInsertListener;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.AsyncQueryListener;
import us.artaround.android.commons.SharedPreferencesCompat;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.tasks.LoadArtTask;
import us.artaround.android.commons.tasks.LoadArtTask.LoadArtCallback;
import us.artaround.android.commons.tasks.LoadDataTask;
import us.artaround.android.commons.tasks.LoadDataTask.LoadDataCallback;
import us.artaround.android.database.ArtAroundDb;
import us.artaround.android.database.ArtAroundDb.Artists;
import us.artaround.android.database.ArtAroundDb.Arts;
import us.artaround.android.database.ArtAroundDb.Categories;
import us.artaround.android.database.ArtAroundDb.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.models.Art;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.models.Artist;
import us.artaround.services.ParseResult;
import us.artaround.services.TempCache;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements OverlayTapListener, ZoomListener, LocationUpdaterCallback,
		LoadArtCallback, AsyncQueryListener, AsyncInsertListener, LoadDataCallback {

	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int PER_PAGE = 100;
	public static final int DEFAULT_ZOOM_LEVEL = 11;

	private static final String[] FILTER_TYPES = { "category", "neighborhood", "artist" };
	private static final int[] MAX_PINS_PER_LEVEL = { 3, 5, 10, 20, 30, 40, 60 };
	private static final int MIN_LEVEL = 8;
	private static final int MAX_LEVEL = 15;

	//--- dialog ids
	private static final int DIALOG_ART_INFO = 0;
	private static final int DIALOG_LOCATION_SETTINGS = 1;
	private static final int DIALOG_WIFI_FAIL = 2;
	private static final int DIALOG_LOADING = 3;
	private static final int DIALOG_FILTER = 4;

	//--- query tokens for async query handler
	private static final int QUERY_ARTS = 0;
	private static final int INSERT_ARTS = 1;
	private static final int QUERY_CATEGORIES = 2;
	private static final int INSERT_CATEGORIES = 3;
	private static final int QUERY_NEIGHBORHOODS = 4;
	private static final int INSERT_NEIGHBORHOODS = 5;
	private static final int QUERY_ARTISTS = 6;
	private static final int INSERT_ARTISTS = 7;

	public static final GeoPoint DEFAULT_GEOPOINT = new GeoPoint(38895111, -77036365); // Washington 

	private ArtOverlay artOverlay;
	private HashMap<Art, OverlayItem> items;

	private ArtMapView mapView;
	private ProgressBar loading;
	private ImageButton btnLocation;
	private MyLocationOverlay myLocationOverlay;

	private NotifyingAsyncQueryHandler queryHandler;

	private List<Art> allArt;
	private List<Art> artFiltered;
	private int nrDisplayedArt;

	private List<String> categories;
	private List<String> neighborhoods;
	private List<String> artists;

	private CheckBoxifiedListAdapter filterAdapter;
	private Map<String, List<CheckBoxifiedText>> filters;

	private int newZoom;
	private boolean toBeRessurected;

	private ConnectivityManager connectivityManager;
	private Location currentLocation;
	private LocationUpdater locationUpdater;

	private SharedPreferences prefs;

	private Set<LoadArtTask> runningArtTasks;
	private AtomicInteger taskCount;
	private AtomicInteger howManyMoreTasks;

	private LoadDataTask loadDataTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		initVars();
		setupUi();
	}

	private void initVars() {
		toBeRessurected = false;

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		locationUpdater = new LocationUpdater(this, this);
		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);

		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder == null) {
			newZoom = DEFAULT_ZOOM_LEVEL;

			allArt = new ArrayList<Art>();
			artFiltered = new ArrayList<Art>();

			categories = new ArrayList<String>();
			neighborhoods = new ArrayList<String>();
			artists = new ArrayList<String>();

			filters = new HashMap<String, List<CheckBoxifiedText>>();
			filterAdapter = new CheckBoxifiedListAdapter(this);

			howManyMoreTasks = new AtomicInteger(0);
			taskCount = new AtomicInteger(0);
			runningArtTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());

			currentLocation = null;
		}
		else {
			Log.d(Utils.TAG, "--Restoring from holder after flipping the screen--");
			newZoom = holder.zoom;

			allArt = holder.allArt;
			artFiltered = holder.artFiltered;

			categories = holder.categories;
			neighborhoods = holder.neighborhoods;
			artists = holder.artists;

			filters = holder.filters;
			filterAdapter = holder.filterAdapter;

			howManyMoreTasks = holder.howManyMoreArtTasks;
			taskCount = holder.artTaskCount;
			runningArtTasks = holder.runningArtTasks;

			loadDataTask = holder.loadDataTask;

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
		myLocationOverlay = new MyLocationOverlay(this, R.drawable.ic_pin_current);

		List<Overlay> overlays = mapView.getOverlays();
		overlays.add(artOverlay);
		overlays.add(myLocationOverlay);

		centerMapOnCurrentLocation();
	}

	private void setupActionBarUi() {
		btnLocation = (ImageButton) findViewById(R.id.btn_1);
		btnLocation.setImageResource(R.drawable.ic_btn_location);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		final ImageButton btnFilter = (ImageButton) findViewById(R.id.btn_2);
		btnFilter.setImageResource(R.drawable.ic_btn_filter);
		btnFilter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_FILTER);
			}
		});

		ImageButton btnSearch = (ImageButton) findViewById(R.id.btn_3);
		btnSearch.setImageResource(R.drawable.ic_btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});
	}

	private void showLoading() {
		if (loading == null) {
			loading = (ProgressBar) findViewById(R.id.progress);
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

		boolean clearedCache = prefs.getBoolean(Utils.KEY_CLEARED_CACHE, false);
		if (clearedCache) {
			allArt.clear();
			clearPins();
		}

		//--- load art ---
		if (allArt.isEmpty()) {
			if (!isLoadingArt()) { // first time in the app
				loadArt();
			}
			else {
				showLoading();
				attachTasksCallback(); // screen-flip while loading first page
			}
		}
		else {
			if (!isLoadingArt()) {
				displayLastArt(); // screen-flip after loading all art
			}
			else {
				showLoading();
				attachTasksCallback(); // screen-flip while loading
			}
		}

		// Google Maps needs WIFI enabled!!
		checkWifiStatus();

		//FIXME un-comment this for production
		//if (currentLocation == null) {
		//updateLocation();
		//}
	}

	private void clearPins() {
		artOverlay.doClear();
		artOverlay.doPopulate();
	}

	private void attachTasksCallback() {
		for (LoadArtTask task : runningArtTasks) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.attach(this); // pass the new context
			}
		}

		if (loadDataTask != null) {
			loadDataTask.attach(this);
		}
	}

	private void detachTasksCallback() {
		for (LoadArtTask task : runningArtTasks) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.detach();
			}
		}

		if (loadDataTask != null) {
			loadDataTask.detach();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.d(Utils.TAG, "--Saving holder before flipping the screen--");
		toBeRessurected = true;

		Holder holder = new Holder();
		holder.runningArtTasks = runningArtTasks;
		holder.howManyMoreArtTasks = howManyMoreTasks;
		holder.artTaskCount = taskCount;
		holder.allArt = allArt;
		holder.artFiltered = artFiltered;
		holder.zoom = newZoom;
		holder.loadDataTask = loadDataTask;
		holder.categories = categories;
		holder.neighborhoods = neighborhoods;
		holder.artists = artists;
		holder.filters = filters;
		holder.filterAdapter = filterAdapter;

		detachTasksCallback();

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
	}

	private void finalCleanup() {
		Log.d(Utils.TAG, "Final cleanup before onDestroy(): stopping tasks...");

		Iterator<LoadArtTask> it = runningArtTasks.iterator();
		while (it.hasNext()) {
			LoadArtTask task = it.next();
			task.cancel(true);
			it.remove();
		}
		howManyMoreTasks.set(0);
		taskCount.set(0);
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
		case DIALOG_FILTER:
			Dialog filterDialog = new Dialog(this, R.style.CustomDialog);
			filterDialog.setTitle(R.string.filter_art);
			filterDialog.setContentView(R.layout.filter_dialog);
			filterDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					filterAndDisplayAllArt();
				}
			});

			int length = FILTER_TYPES.length;
			String[] typeNames = new String[length];
			for (int i = 0; i < length; i++) {
				typeNames[i] = getString(R.string.by) + " " + FILTER_TYPES[i];
			}

			ListView list = (ListView) filterDialog.findViewById(android.R.id.list);
			list.setEmptyView(filterDialog.findViewById(android.R.id.empty));
			list.setAdapter(filterAdapter);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
					typeNames);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			Spinner spinner = (Spinner) filterDialog.findViewById(R.id.spinner);
			spinner.setAdapter(adapter);
			spinner.setSelection(0);
			spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					filterAdapter.setListItems(filters.get(FILTER_TYPES[position]));
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});

			return filterDialog;
		}
		return builder.create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.art_map_menu, menu);
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
		Log.d(Utils.TAG, "Tapped on " + item.getClass().getName());
		if (item instanceof ArtOverlayItem) {
			gotoArtDetails(((ArtOverlayItem) item).art);
		}
		else if (item instanceof Overlay) {
			//TODO
		}
	}

	private void gotoArtDetails(Art art) {
		Intent intent = new Intent(this, ArtDetails.class);
		intent.putExtra("art", art);
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
		hideLoading();
		showDialog(DIALOG_WIFI_FAIL);
	}

	private void onFinishLoadArt() {
		Log.d(Utils.TAG, "Finished loading all art from the server. Triggering onFinishLoadArt actions.");
		recalculateMaximumDispersion();

		doSaveArt();

		//--- save artists from cache ---
		doSaveArtists();

		setLastCacheUpdate();
		hideLoading();
	}

	private void doSaveArt() {
		int size = allArt.size();
		Log.d(Utils.TAG, "There are " + size + " arts to be saved");
		for (int i = 0; i < size; i++) {
			Art art = allArt.get(i);
			ContentValues vals = ArtAroundDb.artToValues(art);
			queryHandler.startInsert(INSERT_ARTS, Arts.CONTENT_URI, vals);
		}
	}

	private void doSaveArtists() {
		Collection<Artist> artists = TempCache.artists.values();
		if (artists != null && !artists.isEmpty()) {
			int size = artists.size();
			Log.d(Utils.TAG, "There are " + size + " artists to be saved");
			for (Artist artist : artists) {
				ContentValues vals = ArtAroundDb.artistToValues(artist);
				queryHandler.startInsert(INSERT_ARTISTS, Artists.CONTENT_URI, vals);
			}
		}
	}

	private void loadArt() {
		Date lastUpdate = getLastCacheUpdate();
		Log.d(Utils.TAG, "Last art update was " + lastUpdate);

		resetDisplayedArt();
		showLoading();

		if (isCacheOutdated(lastUpdate)) {
			loadArtFromServer();
		} else {
			loadArtFromDatabase();
		}
	}

	private void loadArtFromDatabase() {
		Log.d(Utils.TAG, "Starting querying for arts, categories and neighborhoods from db...");
		queryHandler.startQuery(QUERY_ARTS, Arts.CONTENT_URI, ArtAroundDb.ARTS_PROJECTION);

		// load categories and neighborhoods
		loadExtraDataFromDatabase();
	}

	private boolean isLoadingArt() {
		return !runningArtTasks.isEmpty();
	}

	private void loadArtFromServer() {
		Log.d(Utils.TAG, "Loading art, categories and neighborhoods from server...");

		if (isLoadingArt()) {
			return;
		}
		taskCount.set(1);
		startTask(1);

		// load categories and neighborhoods
		loadExtraDataFromServer();
	}

	private void loadMoreArtFromServer(ParseResult result, LoadArtTask task) {
		runningArtTasks.remove(task);

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

				if (runningArtTasks.size() < MAX_CONCURRENT_TASKS) {
					startTask(taskCount.incrementAndGet());
				}
			}
		}
	}

	private void startTask(int page) {
		LoadArtTask task = new LoadArtTask(this, page, PER_PAGE);
		task.execute();
		runningArtTasks.add(task);
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
		Editor editor = prefs.edit();
		editor.putLong(Utils.KEY_LAST_UPDATE, new Date().getTime());
		SharedPreferencesCompat.apply(editor);
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
	
	private OverlayItem ensureOverlay(Art a) {
		if (items.containsKey(a)) {
			return items.get(a);
		} else {
			ArtOverlayItem pin = new ArtOverlayItem(a);
			items.put(a, pin);
			return pin;
		}
	}

	private void resetDisplayedArt() {
		nrDisplayedArt = 0;
		artFiltered.clear();
	}

	private void filterAndDisplayAllArt() {
		resetDisplayedArt();
		artOverlay.doClear();
		filterAndDisplayArt(allArt);
	}

	private void displayLastArt() {
		nrDisplayedArt = 0;
		displayArt(artFiltered);
	}

	private void filterAndDisplayArt(List<Art> art) {
		if (art == null || art.size() == 0) {
			return;
		}

		// find out max number of pins to display based on zoom
		int allNrPins = art.size();
		int newNrPins = 0;
		if (newZoom <= MIN_LEVEL)
			newNrPins = 1;
		else if (newZoom > MAX_LEVEL)
			newNrPins = allNrPins;
		else
			newNrPins = MAX_PINS_PER_LEVEL[newZoom - MIN_LEVEL - 1];

		Map<String, List<String>> onFilters = filterByAll();
		Log.d(Utils.TAG, "===== FILTERS: " + onFilters + " ===== ");

		//filter
		for (int i = 0; i < allNrPins && nrDisplayedArt < newNrPins; ++i) {
			Art a = art.get(i);

			if (artMatchesFilter(onFilters, a)) {
				artOverlay.addOverlay(ensureOverlay(a));
				artFiltered.add(a);
				++nrDisplayedArt;
			}
		}

		Log.d(Utils.TAG, "~~~ Art filtered: " + artFiltered + " ~~~");

		artOverlay.doPopulate();
		mapView.invalidate();
	}

	boolean artMatchesFilter(Map<String, List<String>> onFilters, Art a) {
		int match = 0;

		for (String filter : FILTER_TYPES) {
			List<String> byType = onFilters.get(filter);

			if (byType.isEmpty() || ("category".equals(filter) && byType.contains(a.category))) {
				++match;
			}
			else if (byType.isEmpty() || ("neighborhood".equals(filter) && byType.contains(a.neighborhood))) {
				++match;
			}
			else if (byType.isEmpty() || ("artist".equals(filter) && byType.contains(a.artist.name))) {
				++match;
			}
		}

		return match == FILTER_TYPES.length;
	}

	private Map<String, List<String>> filterByAll() {
		Map<String, List<String>> onFilters = new HashMap<String, List<String>>();

		for (String filter : FILTER_TYPES) {
			onFilters.put(filter, new ArrayList<String>());
		}

		for (String filter : FILTER_TYPES) {
			List<CheckBoxifiedText> cf = filters.get(filter);
			if (cf == null || cf.isEmpty()) {
				continue;
			}
			int size = cf.size();
			for (int i = 0; i < size; i++) {
				CheckBoxifiedText ct = cf.get(i);
				if (ct.isChecked()) {
					onFilters.get(filter).add(ct.getText());
				}
			}
		}

		return onFilters;
	}

	private void displayArt(List<Art> art) {
		artOverlay.doClear();
		int nrPins = art.size();
		for (int i = 0; i < nrPins; ++i) {
			Art a = art.get(i);
			artOverlay.addOverlay(ensureOverlay(a));
		}
		artOverlay.doPopulate();
		mapView.invalidate();
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
		myLocationOverlay.setGeoPoint(geo);

		Log.d(Utils.TAG, "Centered map on " + geo);
	}

	private void showToast(int msgId) {
		Toast.makeText(getApplicationContext(), msgId, Toast.LENGTH_LONG).show();
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

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		//Log.d(Utils.TAG, "Query " + QUERY_ARTS + " completed.");

		if (cursor == null) {
			Log.w(Utils.TAG, "Returned cursor is null!");
			return;
		}

		switch (token) {
		case QUERY_ARTS:
			
			List<Art> results = ArtAroundDb.artsFromCursor(cursor);
			processLoadedArt(results);
			hideLoading();
			
			Log.d(Utils.TAG, "Retrieved " + results.size() + " arts from the db.");
			break;

		case QUERY_CATEGORIES:
			categories = ArtAroundDb.categoriesFromCursor(cursor);
			
			List<CheckBoxifiedText> cf = new ArrayList<CheckBoxifiedText>();
			int size = categories.size();
			for(int i = 0; i < size; i++) {
				cf.add(new CheckBoxifiedText(categories.get(i)));
			}
			filters.put(FILTER_TYPES[0], cf);
			filterAdapter.setListItems(cf);

			//Log.d(Utils.TAG, "Retrieved categories " + categories + " from the db.");
			break;
		case QUERY_NEIGHBORHOODS:
			neighborhoods = ArtAroundDb.neighborhoodsFromCursor(cursor);
			
			cf = new ArrayList<CheckBoxifiedText>();
			size = neighborhoods.size();
			for (int i = 0; i < size; i++) {
				cf.add(new CheckBoxifiedText(neighborhoods.get(i)));
			}
			filters.put(FILTER_TYPES[1], cf);
			filterAdapter.setListItems(cf);

			//Log.d(Utils.TAG, "Retrieved neighborhoods " + neighborhoods + " from the db.");
			break;
		case QUERY_ARTISTS:
			artists = ArtAroundDb.artistsFromCursor(cursor);
			
			cf = new ArrayList<CheckBoxifiedText>();
			size = artists.size();
			for (int i = 0; i < size; i++) {
				cf.add(new CheckBoxifiedText(artists.get(i)));
			}
			filters.put(FILTER_TYPES[2], cf);
			filterAdapter.setListItems(cf);

			//Log.d(Utils.TAG, "Retrieved artists " + artists + " from the db.");
			break;
		}

		cursor.close();
	}

	@Override
	public void onInsertComplete(int token, Object cookie, Uri uri) {
		// this is for debug only
		switch (token) {
		case INSERT_ARTS:
		case INSERT_ARTISTS:
		case INSERT_CATEGORIES:
		case INSERT_NEIGHBORHOODS:
			//Log.d(Utils.TAG, "Inserted " + uri + " into db");
			break;
		}
	}

	private void loadExtraDataFromServer() {
		loadDataTask = (LoadDataTask) new LoadDataTask(this).execute();
	}

	private void loadExtraDataFromDatabase() {
		queryHandler.startQuery(QUERY_CATEGORIES, Categories.CONTENT_URI, ArtAroundDb.CATEGORIES_PROJECTION);
		queryHandler.startQuery(QUERY_NEIGHBORHOODS, Neighborhoods.CONTENT_URI, ArtAroundDb.NEIGHBORHOODS_PROJECTION);
		queryHandler.startQuery(QUERY_ARTISTS, Artists.CONTENT_URI, ArtAroundDb.ARTISTS_PROJECTION);
	}


	@Override
	public void onLoadData(Map<Integer, List<String>> data) {
		loadDataTask = null;

		if(data == null) {
			Toast.makeText(this, R.string.load_data_failure, Toast.LENGTH_LONG).show();
			return;
		}
		
		categories = data.get(LoadDataTask.TYPE_CATEGORIES);
		neighborhoods = data.get(LoadDataTask.TYPE_NEIGHBORHOODS);
		
		//Log.d(Utils.TAG, "Retrieved categories: " + categories + ", neighborhoods: " + neighborhoods + " from server");

		saveExtraData(LoadDataTask.TYPE_CATEGORIES, categories);
		saveExtraData(LoadDataTask.TYPE_NEIGHBORHOODS, neighborhoods);
	}

	private void saveExtraData(int type, List<String> data) {
		if (data == null || data.isEmpty()) {
			return;
		}

		switch (type) {
		case LoadDataTask.TYPE_CATEGORIES:
			int size = data.size();
			for (int i = size - 1; i >= 0; i--) {
				queryHandler.startInsert(INSERT_CATEGORIES, Categories.CONTENT_URI,
						ArtAroundDb.categoryToValues(data.get(i)));
			}
			break;
		case LoadDataTask.TYPE_NEIGHBORHOODS:
			size = data.size();
			for (int i = size - 1; i >= 0; i--) {
				queryHandler.startInsert(INSERT_NEIGHBORHOODS, Neighborhoods.CONTENT_URI,
						ArtAroundDb.neighborhoodToValues(data.get(i)));
			}
			break;
		}
	}

	private static class Holder {
		int zoom;
		Set<LoadArtTask> runningArtTasks;
		LoadDataTask loadDataTask;

		AtomicInteger artTaskCount, howManyMoreArtTasks;
		List<Art> allArt, artFiltered;

		List<String> categories, neighborhoods, artists;
		Map<String, List<CheckBoxifiedText>> filters;
		CheckBoxifiedListAdapter filterAdapter;

		Location currentLocation;
	}
}