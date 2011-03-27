package us.artaround.android.ui;

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
import us.artaround.android.commons.BackgroundCommand;
import us.artaround.android.commons.LoadArtCommand;
import us.artaround.android.commons.LoadingTask;
import us.artaround.android.commons.LoadingTask.LoadingTaskCallback;
import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.NotifyingAsyncUpdateListener;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.models.City;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements OverlayTapListener, ZoomListener, LocationUpdaterCallback,
		LoadingTaskCallback, NotifyingAsyncQueryListener, NotifyingAsyncUpdateListener /*, OnTimeSetListener, NavigationListener*/ {

	//--- loading tasks constants ---
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int ARTS_PER_PAGE = 100;

	//--- zoom constants ---
	public static final int ZOOM_DEFAULT_LEVEL = 11;
	private static final int[] MAX_PINS_PER_ZOOM = { 3, 5, 10, 30, 40, 60 };
	private static final int ZOOM_MIN_LEVEL = 8;
	private static final int ZOOM_MAX_LEVEL = ZOOM_MIN_LEVEL + MAX_PINS_PER_ZOOM.length;

	//private static final int AVG_SPEED = 3000; // 3000m/h
	//private static final long AVG_VISIT_TIME = 600000; // 10 min
	//private static final int HOUR = 3600000;

	//--- activity requests ids ---
	public static final int REQUEST_FILTER = 0;

	//--- dialog ids ---
	private static final int DIALOG_LOCATION_SETTINGS = 1;
	private static final int DIALOG_WIFI_FAIL = 2;
	//private static final int DIALOG_TIME_PICKER = 3;

	private Animation rotateAnim;
	//private Animation inAnim, outAnim;
	private ArtMapView mapView;
	private ArtBalloonsOverlay artOverlay;
	//private ArtBalloonsOverlay routeOverlay;
	private HashMap<Art, OverlayItem> items;

	private ImageButton btnLocation, btnAdd, btnFilter, btnRevert;
	private ImageView imgRefresh;
	private Button btnNearby, btnFavorites;
	//private Button btnRoute;
	//private TextView tvRoute;

	private ArrayList<Art> allArt, filteredArt;
	//private ArrayList<Art> roadArt;
	private int nrPinsToDisplayAtThisZoomLevel;
	private HashMap<Integer, HashSet<String>> filters;

	private int newZoom;
	private boolean toBeRessurected;
	private AtomicBoolean isLoadingArt;

	private NotifyingAsyncQueryHandler queryHandler;
	private LocationUpdater locationUpdater;
	private Location currentLocation;
	private City currentCity;

	private Map<String, LoadingTask> runningTasks;
	private AtomicInteger tasksCount;
	private AtomicInteger tasksLeft;

	//private long routeTotalTime;
	//private List<GeoPoint> routePoints;
	//private int routePosition;
	//private Navigation navigation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		initVars();
		restoreState();
		setupUi();
		changeCity();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupState();
	}

	private void changeCity() {
		currentCity = ServiceFactory.getCurrentCity();
		Utils.d(Utils.TAG, "Change city to " + currentCity.name);
		centerMapOnCurrentLocation();
	}

	private void setupState() {
		Utils.d(Utils.TAG, "Setup state...");

		City newCurrent = ServiceFactory.getCurrentCity();
		if (newCurrent.code != currentCity.code) {
			taskCleanup();
			showLoading(false);
			allArt.clear();
			clearPins();
			changeCity();
		}

		if (isLoadingFromServer()) {
			showLoading(true);
			attachTasksCallback();
		}
		else {
			if (Utils.isCacheOutdated(this)) {
				Utils.d(Utils.TAG, "Cache is outdated!");
				allArt.clear();
				clearPins();
				clearCache();
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
			finish();
		}
	}

	private void clearCache() {
		ArtAroundProvider.contentResolver.delete(Arts.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Artists.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Categories.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Neighborhoods.CONTENT_URI, null, null);
	}

	private void taskCleanup() {
		Utils.d(Utils.TAG, "Stopping all current tasks.");

		Iterator<LoadingTask> it = runningTasks.values().iterator();
		while (it.hasNext()) {
			LoadingTask task = it.next();
			task.cancel(true);
			it.remove();
		}
		tasksLeft.set(0);
		tasksCount.set(0);
	}

	private void initVars() {
		toBeRessurected = false;
		ServiceFactory.init(getApplicationContext());
		locationUpdater = new LocationUpdater(this);
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
		//routePoints = new ArrayList<GeoPoint>();
		//navigation = new Navigation(true);
	}

	@SuppressWarnings("unchecked")
	private void restoreState() {
		Holder holder = (Holder) getLastNonConfigurationInstance();

		if (holder == null) {
			Intent i = getIntent();
			newZoom = i.getIntExtra("newZoom", ZOOM_DEFAULT_LEVEL);
			currentLocation = i.getParcelableExtra("location");
			filters = (HashMap<Integer, HashSet<String>>) i.getSerializableExtra("filters");
			if (filters == null) {
				filters = new HashMap<Integer, HashSet<String>>();
			}

			allArt = new ArrayList<Art>();
			filteredArt = new ArrayList<Art>();
			tasksLeft = new AtomicInteger(0);
			tasksCount = new AtomicInteger(0);
			runningTasks = Collections.synchronizedMap(new HashMap<String, LoadingTask>());
			isLoadingArt = new AtomicBoolean(false);
		}
		else {
			newZoom = holder.newZoom;
			allArt = holder.allArt;
			filteredArt = holder.artFiltered;
			filters = holder.filters;
			tasksLeft = holder.tasksLeft;
			tasksCount = holder.tasksCount;
			runningTasks = holder.runningArtTasks;
			currentLocation = holder.location;
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
		imgRefresh = (ImageView) findViewById(R.id.img_refresh);

		btnLocation = (ImageButton) findViewById(R.id.btn_location);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		btnRevert = (ImageButton) findViewById(R.id.btn_revert);
		btnRevert.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// cleanup and restore default state
				revertDisplay();
				setupState();
				btnRevert.setVisibility(View.INVISIBLE);
			}
		});

		btnFilter = (ImageButton) findViewById(R.id.btn_filter);
		btnFilter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(ArtMap.this, ArtFilters.class).putExtra("filters", filters),
						REQUEST_FILTER);
			}
		});

		btnAdd = (ImageButton) findViewById(R.id.btn_add);
		btnAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoNewArtPage();
			}
		});

		rotateAnim = Utils.getRoateAnim(this);
	}

	private void setupFooterBarUi() {
		btnNearby = (Button) findViewById(R.id.btn_nearby);
		btnNearby.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(ArtMap.this, ArtNearby.class).putExtra("arts", filteredArt));
			}
		});

		btnFavorites = (Button) findViewById(R.id.btn_favorites);
		btnFavorites.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(ArtMap.this, ArtFavs.class));
			}
		});

		//		btnRoute = (Button) findViewById(R.id.btn_directions);
		//		btnRoute.setOnClickListener(new View.OnClickListener() {
		//			@Override
		//			public void onClick(View v) {
		//				showDialog(DIALOG_TIME_PICKER);
		//			}
		//		});
		//		btnRoute.setOnLongClickListener(new View.OnLongClickListener() {
		//			@Override
		//			public boolean onLongClick(View v) {
		//				if (tvRoute.getVisibility() == View.INVISIBLE) {
		//					showDirectionsPopup();
		//				}
		//				return true;
		//			}
		//		});
		//
		//		tvRoute = (TextView) findViewById(R.id.popup_directions);
		//		tvRoute.setMovementMethod(new ScrollingMovementMethod());
		//		tvRoute.setOnLongClickListener(new View.OnLongClickListener() {
		//			@Override
		//			public boolean onLongClick(View v) {
		//				if (tvRoute.getVisibility() == View.VISIBLE) {
		//					hideDirectionsPopup();
		//				}
		//				return true;
		//			}
		//		});

		//		outAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.glide_out);
		//		outAnim.setAnimationListener(new AnimationListener() {
		//			@Override
		//			public void onAnimationStart(Animation animation) {}
		//
		//			@Override
		//			public void onAnimationRepeat(Animation animation) {}
		//
		//			@Override
		//			public void onAnimationEnd(Animation animation) {
		//				tvRoute.setVisibility(View.VISIBLE);
		//			}
		//		});
		//
		//		inAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.glide_in);
		//		inAnim.setAnimationListener(new AnimationListener() {
		//			@Override
		//			public void onAnimationStart(Animation animation) {}
		//
		//			@Override
		//			public void onAnimationRepeat(Animation animation) {}
		//
		//			@Override
		//			public void onAnimationEnd(Animation animation) {
		//				tvRoute.setVisibility(View.INVISIBLE);
		//			}
		//		});
	}

	//	private void showDirectionsPopup() {
	//		if (tvRoute.getVisibility() == View.INVISIBLE) {
	//			tvRoute.startAnimation(outAnim);
	//		}
	//	}
	//
	//	private void hideDirectionsPopup() {
	//		if (tvRoute.getVisibility() == View.VISIBLE) {
	//			tvRoute.startAnimation(inAnim);
	//		}
	//	}

	private void setupMapUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		mapView.setZoomLevel(newZoom);
		mapView.setZoomListener(this);

		items = new HashMap<Art, OverlayItem>();
		artOverlay = new ArtBalloonsOverlay(getResources().getDrawable(R.drawable.ic_pin), this, mapView);
		mapView.getOverlays().add(artOverlay);
	}

	private void showLoading(boolean loading) {
		if (loading) {
			imgRefresh.setVisibility(View.VISIBLE);
			imgRefresh.startAnimation(rotateAnim);
		}
		else {
			imgRefresh.clearAnimation();
			imgRefresh.setVisibility(View.INVISIBLE);
		}
	}

	private void clearPins() {
		artOverlay.doClear();
		artOverlay.doPopulate();
	}

	private void revertDisplay() {
		clearPins();
		allArt.clear();
		filteredArt.clear();
		filters.clear();
		mapView.getOverlays().clear();
		mapView.getOverlays().add(artOverlay);
		mapView.setZoomLevel(ZOOM_DEFAULT_LEVEL);
		mapView.invalidate();
		//hideDirectionsPopup();
	}

	private void attachTasksCallback() {
		for (LoadingTask task : runningTasks.values()) {
			if (task.getStatus() == AsyncTask.Status.RUNNING) {
				task.attachCallback(this); // pass the new context
			}
		}
	}

	private void detachTasksCallback() {
		for (LoadingTask task : runningTasks.values()) {
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
		holder.newZoom = newZoom;
		holder.filters = filters;
		holder.location = currentLocation;
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
		//		case DIALOG_TIME_PICKER:
		//			Time t = new Time();
		//			t.setToNow();
		//			return new TimePickerDialog(this, this, t.hour, t.minute, true);
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

			btnRevert.setVisibility(View.VISIBLE);

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
		Intent iArt = new Intent(this, ArtInfo.class);
		iArt.putExtra("art", art).putExtra("location", currentLocation);
		iArt.putExtra("newZoom", newZoom);
		iArt.putExtra("filters", filters);
		startActivity(iArt);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void gotoNewArtPage() {
		// save the current state of the map
		Intent iArt = new Intent(this, NewArtInfo.class);
		iArt.putExtra("location", currentLocation);
		iArt.putExtra("newZoom", newZoom);
		iArt.putExtra("filters", filters);
		startActivity(iArt);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void onFinishLoadArt() {
		Utils.d(Utils.TAG, "Finished loading all art from the server.");

		saveAllArt();
		Utils.setLastCacheUpdate(this);

		showLoading(false);
		isLoadingArt.set(false);
	}

	private void saveAllArt() {
		int size = allArt.size();
		Utils.d(Utils.TAG, "Saving " + size + " arts in the db.");

		new Thread() {
			@Override
			public void run() {
				ArtAroundProvider.contentResolver.bulkInsert(Arts.CONTENT_URI, ArtAroundDatabase.artsToValues(allArt));
			}

		}.start();
	}

	private void loadArt() {
		showLoading(true);
		isLoadingArt.set(true);

		loadArtFromDatabase();
	}

	private void loadArtFromDatabase() {
		Utils.d(Utils.TAG, "Starting querying for arts from db...");
		queryHandler.startQuery(-1, null, Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION, Arts.CITY + "=?",
				new String[] { currentCity.name }, null);
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

	private void loadMoreArtFromServer(ParseResult result, String id) {
		runningTasks.remove(id);

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
		LoadingTask task = new LoadingTask(this, comm);
		task.execute();
		runningTasks.put(comm.getId(), task);
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
	}

	private float distance(Art a, Art b) {
		float[] results = new float[1];
		Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results);
		return results[0];
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
			if (artMatchesFilter(onFilters, a, true)) {
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
		//btnRoute.setEnabled(false);
		btnLocation.setEnabled(false);
		btnFilter.setEnabled(false);
		locationUpdater.updateLocation();
	}

	private void endLocationUpdate() {
		setupState();
		showLoading(false);
		btnNearby.setEnabled(true);
		//btnRoute.setEnabled(true);
		btnLocation.setEnabled(true);
		btnFilter.setEnabled(true);
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
		final GeoPoint geo = currentLocation != null ? Utils.geo(currentLocation) : currentCity.center;
		mapView.getController().animateTo(geo);
		Utils.d(Utils.TAG, "Centered map on " + geo);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.currentLocation = location;
		endLocationUpdate();
		centerMapOnCurrentLocation();
	}

	@Override
	public void onLocationUpdateError() {
		Utils.w(Utils.TAG, "Timeout!");

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
			Utils.w(Utils.TAG, "Something is wrong, returned art cursor is null!");
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
		int newZoom;
		Map<String, LoadingTask> runningArtTasks;
		AtomicInteger tasksCount, tasksLeft;
		ArrayList<Art> allArt, artFiltered;
		HashMap<Integer, HashSet<String>> filters;
	}

	@Override
	public void beforeLoadingTask(BackgroundCommand command) {}

	@Override
	public void afterLoadingTask(BackgroundCommand command, Object object) {
		ParseResult result = (ParseResult) object;
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

		loadMoreArtFromServer(result, command.getId());
	}

	@Override
	public void onLoadingTaskError(BackgroundCommand command, ArtAroundException exception) {
		showLoading(false);
		isLoadingArt.set(false);
		taskCleanup();
		Utils.showToast(this, R.string.load_data_failure);
	}

	@Override
	public void onUpdateComplete(int token, Object cookie, int result) {
		Utils.d(Utils.TAG, "Update result = " + result);
	}

	//	@Override
	//	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	//		Time t = new Time();
	//		t.setToNow();
	//		t.hour = hourOfDay;
	//		t.minute = minute;
	//		routeTotalTime = t.toMillis(false);
	//		computeRoute();
	//	}

	//	private void computeRoute() {
	//		if (currentLocation == null) {
	//			Utils.showToast(this, R.string.update_location);
	//			return;
	//		}
	//		roadArt = new ArrayList<Art>(allArt);
	//
	//		GeoPoint current = Utils.geo(currentLocation);
	//		routePoints.add(Utils.geo(currentLocation));
	//
	//		List<Overlay> overlays = mapView.getOverlays();
	//		overlays.remove(artOverlay);
	//		routeOverlay = new ArtBalloonsOverlay(getResources().getDrawable(R.drawable.ic_pin), mapView);
	//		overlays.add(routeOverlay);
	//		overlays.add(new CurrentOverlay(this, getResources().getDrawable(R.drawable.ic_pin_current), current, false));
	//
	//		Time now = new Time();
	//		now.setToNow();
	//		long timeLeft = routeTotalTime - now.toMillis(true);
	//
	//		addNextGeoPoint(current, timeLeft);
	//
	//		Utils.d(Utils.TAG, "route=" + routePoints);
	//
	//		if (routePoints.size() > 2) {
	//			// because the routePosition must be set before any other thread finishes
	//			synchronized (this) {
	//				navigation.navigateTo(routePoints.get(0), routePoints.get(1), Navigation.TYPE_WALKING, this);
	//				routePosition = 1;
	//			}
	//		}
	//	}

	//	private void addNextGeoPoint(GeoPoint currentGeo, long timeLeft) {
	//		if (timeLeft <= 0) return;
	//
	//		double currentLat = currentGeo.getLatitudeE6() / 1E6;
	//		double currentLong = currentGeo.getLongitudeE6() / 1E6;
	//
	//		float[] buf = new float[1];
	//		double minDist = Float.MAX_VALUE;
	//		double minLat = 0;
	//		double minLong = 0;
	//		Art nearestArt = null;
	//
	//		int n = roadArt.size();
	//		for (int i = 0; i < n; ++i) {
	//			Art art = roadArt.get(i);
	//
	//			Location.distanceBetween(currentLat, currentLong, art.latitude, art.longitude, buf);
	//			art._distanceFromCurrentPosition = buf[0];
	//
	//			if (art._distanceFromCurrentPosition < minDist) {
	//				minDist = art._distanceFromCurrentPosition;
	//				nearestArt = art;
	//			}
	//		}
	//
	//		if (minDist == Float.MAX_VALUE) { // did not find any points
	//			return;
	//		}
	//
	//		minLat = nearestArt.latitude;
	//		minLong = nearestArt.longitude;
	//		roadArt.remove(nearestArt);
	//
	//		routeOverlay.addOverlay(new ArtOverlayItem(nearestArt));
	//		routeOverlay.doPopulate();
	//		mapView.invalidate();
	//
	//		long duration = (long) ((minDist / AVG_SPEED) * HOUR) + AVG_VISIT_TIME;
	//		if (duration > timeLeft) return;
	//
	//		Utils.d(Utils.TAG, "dist=" + minDist + ",time=" + ((float) duration / HOUR));
	//
	//		GeoPoint p = Utils.geo(minLat, minLong);
	//		routePoints.add(p);
	//
	//		addNextGeoPoint(Utils.geo(minLat, minLong), timeLeft - duration);
	//	}
	//
	//	@Override
	//	public void onNavigationAvailable(int type, Route route) {
	//		Utils.d(Navigation.TAG, "route=" + route);
	//
	//		List<GeoPoint> points = route.getGeoPoints();
	//		List<Placemark> placemarks = route.getPlacemarks();
	//
	//		if (points != null) {
	//			int size = points.size();
	//			for (int i = 0; i < size; i++) {
	//				Placemark pl = null;
	//				if (placemarks.size() > i) {
	//					pl = placemarks.get(i);
	//				}
	//				if (pl != null) {
	//					tvRoute.setText(tvRoute.getText() + buildDirections(pl));
	//				}
	//			}
	//		}
	//
	//		synchronized (this) {
	//			if (routePosition == 1) {
	//				showDirectionsPopup();
	//				mapView.setZoomLevel(mapView.getMaxZoomLevel() - 2); // FIXME magic number
	//				btnRevert.setVisibility(View.VISIBLE);
	//			}
	//			if (routePosition < routePoints.size() - 1) {
	//				navigation.navigateTo(routePoints.get(routePosition), routePoints.get(routePosition + 1),
	//						Navigation.TYPE_WALKING, this);
	//				routePosition += 1;
	//
	//				tvRoute.setText(tvRoute.getText() + " --- " + route.getTotalDistance() + " ---" + Utils.DNL);
	//			}
	//		}
	//
	//		RouteLineOverlay rlo = new RouteLineOverlay(this, mapView, points);
	//		mapView.getOverlays().add(rlo);
	//		mapView.invalidate();
	//	}
	//
	//	@Override
	//	public void onNavigationUnavailable(GeoPoint startPoint, GeoPoint endPoint) {
	//		Utils.d(Navigation.TAG, "Can't get directions from " + startPoint + " to " + endPoint);
	//	}
	//
	//	private String buildDirections(Placemark placemark) {
	//		if (placemark == null) return "";
	//		String instructions = placemark.getInstructions();
	//		String distance = placemark.getDistance();
	//
	//		StringBuilder bld = new StringBuilder();
	//		if (!TextUtils.isEmpty(instructions)) {
	//			bld.append(instructions);
	//			if (!TextUtils.isEmpty(distance)) {
	//				bld.append(" -> ");
	//			}
	//		}
	//		if (!TextUtils.isEmpty(distance)) {
	//			bld.append(distance);
	//		}
	//		if (bld.length() > 0) {
	//			bld.append(Utils.NL);
	//		}
	//		return bld.toString();
	//	}

}