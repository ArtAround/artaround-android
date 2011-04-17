package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.NotifyingAsyncQueryHandler;
import us.artaround.android.common.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.common.Utils;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.ClearCacheCommand;
import us.artaround.android.common.task.LoadArtCommand;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.models.City;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.maps.GeoPoint;

public class ArtMap extends ArtAroundMapActivity implements OverlayTapListener, ZoomListener, LocationUpdaterCallback,
		NotifyingAsyncQueryListener {

	private static final String TAG = "ArtAround.ArtMap";

	//--- server call constants ---
	public static final int ARTS_PER_PAGE = 100;

	//--- zoom constants ---
	public static final int ZOOM_DEFAULT_LEVEL = 15;
	private static final int[] MAX_PINS_PER_ZOOM = { 3, 5, 10, 30, 40, 60 };
	private static final int ZOOM_MIN_LEVEL = 8;
	private static final int ZOOM_MAX_LEVEL = ZOOM_MIN_LEVEL + MAX_PINS_PER_ZOOM.length;

	//--- activity requests ids ---
	public static final int REQUEST_FILTER = 0;

	//--- dialog ids ---
	private static final int DIALOG_LOCATION_SETTINGS = 1;
	private static final int DIALOG_WIFI_FAIL = 2;

	//--- commands ids ---
	private static final int LOAD_ARTS = 0;
	private static final int CLEAR_CACHE = 100;

	//--- queries ids ---
	private static final int QUERY_ARTS = 0;

	//--- handler msgs ---
	private static final int MSG_LOAD_ARTS_FINISHED = 0;
	private static final int MSG_PROCESS_ARTS = 1;

	//--- save state --- 
	private static final String SAVE_ARTS = "save_arts";
	private static final String SAVE_FILTERED_ARTS = "save_filtered_arts";
	private static final String SAVE_LOCATION = "save_location";
	private static final String SAVE_ZOOM = "save_zoom";
	private static final String SAVE_MAP_LATITUDE = "save_map_lati";
	private static final String SAVE_MAP_LONGITUDE = "save_map_longi";
	private static final String SAVE_FILTERS = "save_filters";
	private static final String SAVE_CRT_PAGE = "save_crt_page";
	private static final String SAVE_TOTAL_COUNT = "save_total_count";

	private static final String ARTS_SELECTION = Arts.CITY + "=?";

	private Animation rotateAnim;
	private ArtMapView mapView;

	private int newZoom = ZOOM_DEFAULT_LEVEL;
	private ArtBubblesOverlay artsOverlay;
	private HashMap<Art, ArtOverlayItem> items;

	private ImageView btnLocation;
	private ImageView btnFavorite;
	private ImageView btnSearch;
	private ImageView imgRefresh;
	private Button btnAddArt;

	private ArrayList<Art> allArt;
	private ArrayList<Art> filteredArt;
	private int nrPinsForZoomLevel;
	private HashMap<Integer, HashSet<String>> filters;

	private NotifyingAsyncQueryHandler queryHandler;
	private LocationUpdater locationUpdater;
	private Location currentLocation;
	private GeoPoint currentMapCenter;
	private City currentCity;

	private final AtomicInteger totalCount = new AtomicInteger(0);
	private final AtomicInteger crtPage = new AtomicInteger(1);

	@Override
	protected void onChildCreate(Bundle savedInstanceState) {
		setContentView(R.layout.art_map);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		setupVars(savedInstanceState);
		setupUi();
	}

	@Override
	protected void onChildEndCreate(Bundle savedInstanceState) {
		Utils.d(TAG, "onChildEndCreate()");
		setupState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationUpdater.removeUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (currentLocation == null) {
			startLocationUpdate();
		}
	}

	@Override
	protected void initActionbarUi() {
		super.initActionbarUi();
		Utils.d(TAG, "initActionbarUi()");

		imgRefresh = (ImageView) actionbar.findViewById(R.id.img_loading);
		rotateAnim = Utils.getRoateAnim(this);

		btnLocation = (ImageView) actionbar.findViewById(R.id.btn_locate);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		btnSearch = (ImageView) actionbar.findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoFiltersPage();
			}
		});

		btnFavorite = (ImageView) actionbar.findViewById(R.id.btn_favorite);
		btnFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoFavoritesPage();
			}
		});
	}

	private void setupVars(Bundle savedInstanceState) {
		ServiceFactory.init(getApplicationContext());
		ArtAroundProvider.contentResolver.registerContentObserver(Arts.CONTENT_URI, false, new ArtsContentObserver(
				handler));

		locationUpdater = new LocationUpdater(this);
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
		items = new HashMap<Art, ArtOverlayItem>();

		restoreState(savedInstanceState);

		if (filteredArt == null) {
			filteredArt = new ArrayList<Art>();
		}
		if (filters == null) {
			filters = new HashMap<Integer, HashSet<String>>();
		}
	}

	private void setupState() {
		Utils.d(TAG, "setupState()");

		checkWifiStatus();
		changeCity();
		startLocationUpdate();

		if (hasRunningTasks(LOAD_ARTS)) {
			showLoading(true);
		}
		else {
			showLoading(false);

			if (Utils.isCacheOutdated(this)) {
				Utils.d(TAG, "setupState(): cache outdated!");
				Utils.showToast(this, R.string.update_cache_progress);
				clearCache();
			}
			else {
				loadArt();
			}
		}
	}

	private void changeCity() {
		currentCity = ServiceFactory.getCurrentCity();
		if (currentMapCenter == null) {
			currentMapCenter = currentCity.center;
		}
		Utils.d(TAG, "changeCity() " + currentCity.name);
		//centerMapOnLocation();
	}

	private void clearCache() {
		showLoading(true);
		startTask(new ClearCacheCommand(CLEAR_CACHE, String.valueOf(CLEAR_CACHE)));
	}

	private void setupUi() {
		setupFooterBarUi();
		setupMapUi();
	}

	private void setupFooterBarUi() {
		btnAddArt = (Button) findViewById(R.id.btn_add_art);
		btnAddArt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoNewArtPage();
			}
		});
	}

	private void setupMapUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		View zoomControls = mapView.getZoomButtonsController().getZoomControls();
		FrameLayout parent = (FrameLayout) zoomControls.getParent();
		parent.removeView(zoomControls);

		FrameLayout zoomLayout = (FrameLayout) findViewById(R.id.layout_zoom);
		zoomLayout.addView(zoomControls);
		//FIXME make the zoom controllers auto-hide
		mapView.getZoomButtonsController().setAutoDismissed(true);

		mapView.setZoomLevel(newZoom);
		mapView.setZoomListener(this);

		artsOverlay = new ArtBubblesOverlay(getResources().getDrawable(R.drawable.ic_pin), this, mapView);
		mapView.getOverlays().add(artsOverlay);
	}

	@SuppressWarnings("unchecked")
	private void restoreState(Bundle savedInstanceState) {
		if (savedInstanceState == null) return;

		crtPage.set(savedInstanceState.getInt(SAVE_CRT_PAGE, 1));
		totalCount.set(savedInstanceState.getInt(SAVE_TOTAL_COUNT, 0));

		int lati = savedInstanceState.getInt(SAVE_MAP_LATITUDE, 0);
		int longi = savedInstanceState.getInt(SAVE_MAP_LONGITUDE, 0);

		if (lati == 0 || longi == 0) {
			if (currentCity != null) {
				lati = currentCity.center.getLatitudeE6();
				longi = currentCity.center.getLongitudeE6();
			}
		}

		if (lati != 0 && longi != 0) {
			currentMapCenter = new GeoPoint(lati, longi);
		}

		currentLocation = savedInstanceState.getParcelable(SAVE_LOCATION);
		newZoom = savedInstanceState.getInt(SAVE_ZOOM, ZOOM_DEFAULT_LEVEL);

		allArt = (ArrayList<Art>) savedInstanceState.getSerializable(SAVE_ARTS);
		filteredArt = (ArrayList<Art>) savedInstanceState.getSerializable(SAVE_FILTERED_ARTS);
		if (filteredArt == null) {
			filteredArt = new ArrayList<Art>();
		}

		filters = (HashMap<Integer, HashSet<String>>) savedInstanceState.getSerializable(SAVE_FILTERS);
		if (filters == null) {
			filters = new HashMap<Integer, HashSet<String>>();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAVE_CRT_PAGE, crtPage.get());
		outState.putInt(SAVE_TOTAL_COUNT, totalCount.get());
		GeoPoint center = mapView.getMapCenter();
		outState.putInt(SAVE_MAP_LATITUDE, center.getLatitudeE6());
		outState.putInt(SAVE_MAP_LONGITUDE, center.getLongitudeE6());
		outState.putInt(SAVE_ZOOM, newZoom);
		outState.putParcelable(SAVE_LOCATION, currentLocation);
		outState.putSerializable(SAVE_ARTS, allArt);
		outState.putSerializable(SAVE_FILTERED_ARTS, filteredArt);
		outState.putSerializable(SAVE_FILTERS, filters);
		super.onSaveInstanceState(outState);
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

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void loadArt() {
		Utils.d(TAG, "loadArtFromDb(): start query");

		showLoading(true);
		queryHandler.startQuery(QUERY_ARTS, Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION, ARTS_SELECTION,
				new String[] { currentCity.name }, null);
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (cursor == null) return;

		startManagingCursor(cursor);

		switch (token) {
		case QUERY_ARTS:
			allArt = ArtAroundDatabase.artsFromCursor(cursor);
			Utils.d(TAG, "onQueryComplete() -QUERY_ARTS- " + allArt.size());

			if (allArt.isEmpty()) {
				loadArtFromServer();
			}
			else {
				showLoading(false);
				processArts();
			}
			break;
		}
	}

	private void loadArtFromServer() {
		Utils.d(TAG, "loadArtFromServer(): start task " + crtPage.get());
		startTask(new LoadArtCommand(LOAD_ARTS, String.valueOf(LOAD_ARTS), crtPage.getAndIncrement(), ARTS_PER_PAGE));
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {
		super.onPreExecute(command);
	}

	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		super.onPostExecute(command, result, exception);

		switch (command.token) {
		case LOAD_ARTS:

			if (exception != null) {
				showLoading(false);
				Utils.showToast(this, R.string.load_arts_error);
				return;
			}

			ParseResult pr = (ParseResult) result;
			if (pr == null) {
				showLoading(false);
				showDialog(DIALOG_WIFI_FAIL);
				return;
			}

			Utils.d(TAG, "onPostExecute(): LOAD_ARTS parseResult=" + pr);

			if (pr.page == 1 && !hasRunningTasks(LOAD_ARTS)) {
				totalCount.set(pr.totalCount);

				int tasksLeft = (int) (Math.ceil((double) (pr.totalCount - pr.count) / (double) pr.perPage));
				for (int i = 0; i < tasksLeft; i++) {
					Utils.d(TAG, "onPostExecute(): start task " + crtPage.get());
					startTask(new LoadArtCommand(LOAD_ARTS, String.valueOf(LOAD_ARTS), crtPage.getAndIncrement(),
							ARTS_PER_PAGE));
				}
			}
			break;

		case CLEAR_CACHE:
			showLoading(true);
			loadArt();
			break;
		}
	}

	@Override
	public void onPublishProgress(ArtAroundAsyncCommand command, Object progress) {
		super.onPublishProgress(command, progress);
	}

	private void processArts() {
		calculateMaximumDispersion(allArt);
		filterArt(allArt);
		displayArt(filteredArt);
	}

	private void calculateMaximumDispersion(List<Art> art) {
		Utils.d(TAG, "calculateMaximumDispersion() for " + art.size() + " arts");

		final int size = art.size();
		for (int i = 0; i < size; ++i) {
			art.get(i).mediumDistance = 0;
		}
		for (int i = 0; i < size; ++i) {
			Art a = art.get(i);
			for (int j = i + 1; j < size; ++j) {
				Art b = art.get(j);
				float dist = getDistance(a, b);
				a.mediumDistance += dist;
				b.mediumDistance += dist;
			}
		}
		for (int i = 0; i < size; ++i) {
			art.get(i).mediumDistance /= size;
		}
		Collections.sort(art, new ArtDispersionComparator());
	}

	private float getDistance(Art a, Art b) {
		float[] results = new float[1];
		Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results);
		return results[0];
	}

	private void filterArt(List<Art> art) {
		if (art == null || art.size() == 0) return;

		int allNrPins = art.size();
		HashMap<Integer, List<String>> onFilters = updateFilters();
		Utils.d(TAG, "filterArt(): filters=" + onFilters);

		filteredArt.clear();
		for (int i = 0; i < allNrPins; ++i) {
			Art a = art.get(i);
			if (matchesAllFilters(onFilters, a, true)) {
				filteredArt.add(a);
			}
		}
		Utils.d(TAG, "filterArt(): found " + filteredArt.size() + " matches");
	}

	private boolean matchesFilter(List<String> byType, String prop) {
		if ((TextUtils.isEmpty(prop) && byType.contains(getString(R.string.value_unset)))) return true;
		return byType.contains(prop);
	}

	private boolean matchesAllFilters(HashMap<Integer, List<String>> onFilters, Art a, boolean matchAll) {
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

	private HashMap<Integer, List<String>> updateFilters() {
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

	private void displayArt(List<Art> art) {
		getNrPinsToDisplay(art);
		int nrPins = Math.min(art.size(), this.nrPinsForZoomLevel);
		Utils.d(TAG, "displayArt(): " + nrPins + " pins");

		artsOverlay.doClear();
		for (int i = 0; i < nrPins; ++i) {
			Art a = art.get(i);
			artsOverlay.addOverlay(ensureOverlay(a));
		}
		artsOverlay.doPopulate();
		mapView.invalidate();
	}

	private void getNrPinsToDisplay(List<Art> art) {
		if (art == null || art.size() == 0) {
			return;
		}
		// find out max number of pins to display based on zoom
		int allNrPins = art.size();

		if (newZoom <= ZOOM_MIN_LEVEL)
			nrPinsForZoomLevel = 1;
		else if (newZoom > ZOOM_MAX_LEVEL)
			nrPinsForZoomLevel = allNrPins;
		else
			nrPinsForZoomLevel = MAX_PINS_PER_ZOOM[newZoom - ZOOM_MIN_LEVEL - 1];
	}

	private ArtOverlayItem ensureOverlay(Art a) {
		if (items.containsKey(a)) {
			return items.get(a);
		}
		else {
			ArtOverlayItem pin = new ArtOverlayItem(a);
			items.put(a, pin);
			return pin;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOCATION_SETTINGS:
			return Utils.locationSettingsDialog(this).create();
		case DIALOG_WIFI_FAIL:
			return Utils.wifiSettingsDialog(this).create();
		default:
			return super.onCreateDialog(id);
		}
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
	}

	private void gotoArtPage(Art art) {
		// save the current state of the map
		Intent iArtPage = new Intent(this, ArtEdit.class).putExtra("art", art);
		startActivity(iArtPage);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void gotoNewArtPage() {
		// save the current state of the map
		Intent iArtEdit = new Intent(this, ArtEdit.class);
		startActivity(iArtEdit);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void gotoFavoritesPage() {
		startActivity(new Intent(ArtMap.this, ArtFavs.class));
	}

	private void gotoFiltersPage() {
		startActivityForResult(new Intent(ArtMap.this, ArtFilters.class).putExtra("filters", filters), REQUEST_FILTER);
	}

	@Override
	public void onZoom(int oldZoom, int newZoom) {
		this.newZoom = newZoom;
		//Utils.d(TAG, "Zoom changed from " + oldZoom + " to " + newZoom);
		displayArt(filteredArt);
	}

	private void startLocationUpdate() {
		Utils.showToast(this, R.string.waiting_location);
		showLoading(true);
		btnLocation.setEnabled(false);
		locationUpdater.updateLocation();
	}

	private void endLocationUpdate() {
		showLoading(false);
		btnLocation.setEnabled(true);
		locationUpdater.removeUpdates();
	}

	private boolean checkWifiStatus() {
		ConnectivityManager mngr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mngr.getActiveNetworkInfo();

		if (info == null || (info != null && info.getState() != NetworkInfo.State.CONNECTED)) {
			showDialog(DIALOG_WIFI_FAIL);
			return false;
		}
		return true;
	}

	private void centerMapOnLocation() {
		if (currentMapCenter == null) return;

		mapView.getController().animateTo(currentMapCenter);
		Utils.d(TAG, "centerMapOnLocation() " + currentMapCenter);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.currentLocation = location;
		this.currentMapCenter = Utils.geo(currentLocation);
		endLocationUpdate();
		centerMapOnLocation();
	}

	@Override
	public void onLocationUpdateError() {
		endLocationUpdate();
		Utils.showToast(this, R.string.location_update_failure);
	}

	@Override
	public void onSuggestLocationSettings() {
		showDialog(DIALOG_LOCATION_SETTINGS);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			doOnBackKey();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		doOnBackKey();
	}

	private void doOnBackKey() {
		if (!artsOverlay.hideBubble()) {
			finish();
		}
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LOAD_ARTS_FINISHED:
				showLoading(false);
				Utils.setLastCacheUpdate(ArtMap.this);
				break;

			case MSG_PROCESS_ARTS:
				processArts();
				break;
			}
		}
	};

	private class ArtsContentObserver extends ContentObserver {
		private final Handler handler;

		public ArtsContentObserver(Handler handler) {
			super(handler);
			this.handler = handler;
		}

		@Override
		public boolean deliverSelfNotifications() {
			return false;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			Cursor cursor = ArtAroundProvider.contentResolver.query(Arts.CONTENT_URI,
					ArtAroundDatabase.ARTS_PROJECTION, ARTS_SELECTION, new String[] { currentCity.name }, null);
			startManagingCursor(cursor);

			allArt = ArtAroundDatabase.artsFromCursor(cursor);
			Utils.d(TAG, "onChange(): updated new arts from db " + allArt.size());

			if (allArt.size() > 0) {
				handler.sendEmptyMessage(MSG_PROCESS_ARTS);

				//FIXME fix the case where not all arts could be parsed
				if (totalCount.get() == allArt.size()) {
					Utils.d(TAG, "onChange(): finished loading arts!");
					handler.sendEmptyMessage(MSG_LOAD_ARTS_FINISHED);
				}
			}
		}
	}
}