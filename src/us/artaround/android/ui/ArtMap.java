package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.LoaderPayload;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.SharedPreferencesCompat;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.City;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

public class ArtMap extends FragmentActivity implements OverlayTapListener, ZoomListener, LocationUpdaterCallback {

	private static final String TAG = "ArtMap";

	//--- server call constants ---
	private static final int ARTS_PER_PAGE = 100;
	private static final String ARG_PAGE = "page";

	//--- zoom constants ---
	public static final int ZOOM_DEFAULT_LEVEL = 13;
	private static final int[] MAX_PINS_PER_ZOOM = { 3, 5, 10, 30, 40, 60 };
	private static final int ZOOM_MIN_LEVEL = 8;
	private static final int ZOOM_MAX_LEVEL = ZOOM_MIN_LEVEL + MAX_PINS_PER_ZOOM.length;

	//--- activity requests ids ---
	public static final int REQUEST_FILTER = 0;
	public static final String EXTRA_FILTERS = "filters";

	//--- dialog ids ---
	private static final int DIALOG_LOCATION_SETTINGS = 1;
	private static final int DIALOG_WIFI_FAIL = 2;
	private static final int DIALOG_ABOUT = 3;
	private static final int DIALOG_CHANGELOG = 4;
	private static final int DIALOG_WELCOME = 5;

	//--- loader ids ---
	private static final int LOADER_CURSOR_ARTS = 0;
	private static final int LOADER_ASYNC_ARTS = 1;
	private static final int LOADER_CURSOR_FAVORITES = 2;

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

	private ImageButton btnLocation;
	private ImageButton btnFavorites;
	private ImageButton btnSearch;
	private ImageView imgRefresh;
	private Button btnAddArt;

	private View filtersHeading;
	private TextView filtersTitle;
	private ImageButton btnFiltersExit;

	private ArrayList<Art> allArt;
	private ArrayList<Art> filteredArt;
	private int nrPinsForZoomLevel;
	private HashMap<Integer, HashSet<String>> filters;

	private SharedPreferences sharedPrefs;
	private LocationUpdater locationUpdater;
	private Location currentLocation;
	private GeoPoint currentMapCenter;
	private City currentCity;

	private final AtomicInteger totalCount = new AtomicInteger(0);
	private final AtomicInteger crtPage = new AtomicInteger(1);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);

		setContentView(R.layout.art_map);
		Utils.d(TAG, "onCreate()");

		ServiceFactory.init(getApplicationContext());
		ArtAroundProvider.contentResolver.registerContentObserver(Arts.CONTENT_URI, false, new ArtsContentObserver(
				handler));

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		locationUpdater = new LocationUpdater(this);
		items = new HashMap<Art, ArtOverlayItem>();

		restoreState(savedInstanceState);
		setupUi();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Utils.d(TAG, "onPause()");
		locationUpdater.removeUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Utils.d(TAG, "onResume()");

		if (currentLocation == null && !getIntent().hasExtra(SAVE_MAP_LATITUDE)) {
			startLocationUpdate();
		}

		// WiFi status has been already checked
		if (sharedPrefs.getBoolean(Utils.KEY_CHECK_WIFI, true)) {
			checkWifiStatus();

			Editor edit = sharedPrefs.edit();
			edit.putBoolean(Utils.KEY_CHECK_WIFI, false);
			SharedPreferencesCompat.apply(edit);
		}
		setupState();
	}

	private void setupActionbarUi() {
		Utils.d(TAG, "initActionbarUi()");

		imgRefresh = (ImageView) findViewById(R.id.img_loading);
		rotateAnim = Utils.getRoateAnim(this);

		btnLocation = (ImageButton) findViewById(R.id.btn_locate);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		btnSearch = (ImageButton) findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoFiltersPage();
			}
		});

		btnFavorites = (ImageButton) findViewById(R.id.btn_favorite);
		btnFavorites.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadFavorites();
			}
		});
	}

	private void setupState() {
		Utils.d(TAG, "setupState()");

		changeCity();
		if (currentLocation == null && !getIntent().hasExtra(SAVE_MAP_LATITUDE)) {
			startLocationUpdate();
		}

		if (Utils.isCacheOutdated(this)) {
			Utils.d(TAG, "setupState(): cache outdated, loading...");
			Toast.makeText(this, R.string.update_cache_progress, Toast.LENGTH_SHORT).show();
			clearCache();
			loadArt();
		}
		else if (allArt.isEmpty()) {
			Utils.d(TAG, "setupState(): allArt is empty, loading...");
			loadArt();
		}
		else {
			Utils.d(TAG, "setupState(): filter & display existing arts.");
			filterArt(allArt);
			displayArt(filteredArt);
		}
	}

	private void changeCity() {
		currentCity = ServiceFactory.getCurrentCity();
		if (currentMapCenter == null) {
			currentMapCenter = currentCity.center;
		}
	}

	private void clearCache() {
		artsOverlay.doClear();
		totalCount.set(0);
		crtPage.set(1);
	}

	private void setupUi() {
		setupActionbarUi();
		setupFilterBarUi();
		setupFooterBarUi();
		setupMapUi();

		if (sharedPrefs.getBoolean(Utils.KEY_SHOW_WELCOME, true)) {
			showDialog(DIALOG_WELCOME);
			SharedPreferencesCompat.apply(sharedPrefs.edit().putBoolean(Utils.KEY_SHOW_WELCOME, false));
		}
		if (isNewVersion()) {
			showDialog(DIALOG_CHANGELOG);
			SharedPreferencesCompat.apply(sharedPrefs.edit().putString(Utils.KEY_VERSION,
					getString(R.string.app_version)));
		}
	}

	private boolean isNewVersion() {
		return !sharedPrefs.getString(Utils.KEY_VERSION, getString(R.string.app_version)).equalsIgnoreCase(
				getString(R.string.app_version));
	}

	private void setupFilterBarUi() {
		filtersHeading = findViewById(R.id.filters);
		filtersTitle = (TextView) findViewById(R.id.filters_title);
		btnFiltersExit = (ImageButton) findViewById(R.id.filters_exit);
		btnFiltersExit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				filtersTitle.setText("");
				filtersHeading.setVisibility(View.GONE);
				artsOverlay.hideBubble();

				// clear all filters
				filters.clear();
				filterArt(allArt);
				displayArt(filteredArt);
			}
		});
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
		Utils.d(TAG, "restoreState(): savedInstance=", savedInstanceState);

		Intent intent = getIntent();
		if (intent.hasExtra(SAVE_ZOOM)) {
			newZoom = intent.getIntExtra(SAVE_ZOOM, ZOOM_DEFAULT_LEVEL);

			if (intent.hasExtra(SAVE_MAP_LATITUDE) && intent.hasExtra(SAVE_MAP_LONGITUDE)) {
				int lati = intent.getIntExtra(SAVE_MAP_LATITUDE, 0);
				int longi = intent.getIntExtra(SAVE_MAP_LONGITUDE, 0);

				if (lati != 0 && longi != 0) {
					currentMapCenter = new GeoPoint(lati, longi);
				}
			}
		}
		else if (savedInstanceState != null) {
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
			filters = (HashMap<Integer, HashSet<String>>) savedInstanceState.getSerializable(SAVE_FILTERS);
		}

		if (filters == null) {
			filters = new HashMap<Integer, HashSet<String>>();
		}

		if (filteredArt == null) {
			filteredArt = new ArrayList<Art>();
		}

		if (allArt == null) {
			allArt = new ArrayList<Art>();
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

	private void toggleLoading(boolean loading) {
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
		Utils.d(TAG, "loadArt(): start query");

		toggleLoading(true);
		getSupportLoaderManager().restartLoader(LOADER_CURSOR_ARTS, null, cursorCallback);
	}

	private void loadArtFromServer() {
		Utils.d(TAG, "loadArtFromServer(): start loading page=", crtPage.get());

		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, crtPage.getAndIncrement());

		getSupportLoaderManager().restartLoader(LOADER_ASYNC_ARTS, args, asyncCallback);
	}

	private void filterArt(List<Art> art) {
		if (art == null || art.size() == 0) return;

		int allNrPins = art.size();
		HashMap<Integer, List<String>> onFilters = updateFilters();
		Utils.d(TAG, "filterArt(): filters=", onFilters);

		filteredArt.clear();
		for (int i = 0; i < allNrPins; ++i) {
			Art a = art.get(i);
			if (matchesAllFilters(onFilters, a, true)) {
				filteredArt.add(a);
			}
		}
		Utils.d(TAG, "filterArt(): found", filteredArt.size(), "matches");
	}

	private boolean matchesFilter(List<String> byType, String prop) {
		return byType.contains(prop);
	}

	private boolean matchesAllFilters(HashMap<Integer, List<String>> onFilters, Art a, boolean matchAll) {
		int match = 0;
		int length = ArtFilter.FILTER_TYPE_NAMES.length;

		for (int i = 0; i < length; i++) {
			List<String> byType = onFilters.get(i);

			if (byType.isEmpty()) {
				++match;
				continue;
			}

			if (i == ArtFilter.TYPE_CATEGORY && matchesFilter(byType, a.category)) {
				++match;
			}
			else if (i == ArtFilter.TYPE_NEIGHBORHOOD && matchesFilter(byType, a.neighborhood)) {
				++match;
			}
			else if (i == ArtFilter.TYPE_ARTIST && a.artist != null && matchesFilter(byType, a.artist)) {
				++match;
			}
			else if (i == ArtFilter.TYPE_TITLE && matchesFilter(byType, a.title)) {
				++match;
			}
		}

		return matchAll ? match == ArtFilter.FILTER_TYPE_NAMES.length : match > 0;
	}

	private HashMap<Integer, List<String>> updateFilters() {
		HashMap<Integer, List<String>> onFilters = new HashMap<Integer, List<String>>();
		int length = ArtFilter.FILTER_TYPE_NAMES.length;

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
		Utils.d(TAG, "displayArt(): pins=", nrPins);

		artsOverlay.doClear();
		artsOverlay.hideBubble();
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
		case DIALOG_ABOUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.about_title);
			TextView tvAbout = (TextView) LayoutInflater.from(ArtMap.this).inflate(R.layout.about, null);
			tvAbout.setText(Html.fromHtml(getString(R.string.about_msg)));
			builder.setView(tvAbout);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			return builder.create();
		case DIALOG_WELCOME:
			builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.welcome_title);
			builder.setMessage(Html.fromHtml(getString(R.string.welcome_msg)));
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_FILTER:
			filters = (HashMap<Integer, HashSet<String>>) data.getSerializableExtra(EXTRA_FILTERS);

			boolean isEmpty = true;
			for (int type : filters.keySet()) {
				if (!filters.get(type).isEmpty()) {
					isEmpty = false;
					break;
				}
			}

			if (!isEmpty) {
				filterArt(allArt);
				displayArt(filteredArt);

				filtersTitle.setText(R.string.filtered);
				filtersHeading.setVisibility(View.VISIBLE);
			}

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
			break;
		case R.id.about:
			showDialog(DIALOG_ABOUT);
			break;
		case R.id.feedback:
			sendFeedback();
			break;
		}
		return true;
	}

	private void sendFeedback() {
		Intent iEmail = new Intent(Intent.ACTION_SEND);
		iEmail.setType("plain/text");
		iEmail.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.feedback_email) });
		iEmail.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
		startActivity(iEmail);
	}

	@Override
	public void onTap(Object item) {
		if (item instanceof ArtOverlayItem) {
			gotoArtDetail(((ArtOverlayItem) item).art);
		}
	}

	private void gotoArtDetail(Art art) {
		// save the current state of the map
		Intent iArtDetail = new Intent(this, ArtDetail.class).putExtra(ArtDetail.EXTRA_ART, art);
		iArtDetail.putExtra(SAVE_ZOOM, newZoom);
		GeoPoint center = mapView.getMapCenter();
		iArtDetail.putExtra(SAVE_MAP_LATITUDE, center.getLatitudeE6());
		iArtDetail.putExtra(SAVE_MAP_LONGITUDE, center.getLongitudeE6());
		startActivity(iArtDetail);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void gotoNewArtPage() {
		// save the current state of the map
		Intent iArtEdit = new Intent(this, ArtEdit.class);
		iArtEdit.putExtra(SAVE_ZOOM, newZoom);
		GeoPoint center = mapView.getMapCenter();
		iArtEdit.putExtra(SAVE_MAP_LATITUDE, center.getLatitudeE6());
		iArtEdit.putExtra(SAVE_MAP_LONGITUDE, center.getLongitudeE6());
		startActivity(iArtEdit);
		finish(); // call finish to save memory because of the 2 map views
	}

	private void loadFavorites() {
		toggleLoading(true);
		getSupportLoaderManager().restartLoader(LOADER_CURSOR_FAVORITES, null, cursorCallback);
	}

	private void gotoFiltersPage() {
		Intent iArtFilter = new Intent(this, ArtFilter.class);
		iArtFilter.putExtra(SAVE_ZOOM, newZoom);
		GeoPoint center = mapView.getMapCenter();
		iArtFilter.putExtra(SAVE_MAP_LATITUDE, center.getLatitudeE6());
		iArtFilter.putExtra(SAVE_MAP_LONGITUDE, center.getLongitudeE6());
		startActivityForResult(iArtFilter.putExtra(EXTRA_FILTERS, filters), REQUEST_FILTER);
	}

	@Override
	public void onZoom(int oldZoom, int newZoom) {
		this.newZoom = newZoom;
		displayArt(filteredArt);
	}

	private void startLocationUpdate() {
		Toast.makeText(this, R.string.waiting_location, Toast.LENGTH_SHORT).show();
		toggleLoading(true);
		btnLocation.setEnabled(false);
		locationUpdater.updateLocation();
	}

	private void endLocationUpdate() {
		toggleLoading(false);
		btnLocation.setEnabled(true);
		locationUpdater.removeUpdates();
	}

	private void checkWifiStatus() {
		ConnectivityManager mngr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mngr.getActiveNetworkInfo();

		if (info == null || (info != null && info.getState() != NetworkInfo.State.CONNECTED)) {
			showDialog(DIALOG_WIFI_FAIL);
		}
	}

	private void centerMapOnLocation() {
		if (currentMapCenter == null) return;

		mapView.getController().animateTo(currentMapCenter);
		Utils.d(TAG, "centerMapOnLocation()", currentMapCenter);
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
		Toast.makeText(this, R.string.location_update_failure, Toast.LENGTH_LONG).show();
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
				toggleLoading(false);
				Utils.setLastCacheUpdate(ArtMap.this);
				break;

			case MSG_PROCESS_ARTS:
				filterArt(allArt);
				displayArt(filteredArt);
				break;
			}
		}
	};

	private final LoaderCallbacks<Cursor> cursorCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case LOADER_CURSOR_ARTS:
				return new CursorLoader(ArtMap.this, Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION,
						ARTS_SELECTION, new String[] { currentCity.name }, null);

			case LOADER_CURSOR_FAVORITES:
				return new CursorLoader(ArtMap.this, Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION, Arts.FAVORITE
						+ "=1", null, null);
			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			switch (loader.getId()) {
			case LOADER_CURSOR_ARTS:
				allArt = ArtAroundDatabase.artsFromCursor(cursor);
				Utils.d(TAG, "onLoadFinished(): LOADER_ARTS", allArt.size());

				if (allArt.isEmpty()) {
					loadArtFromServer();
				}
				else {
					toggleLoading(false);
					//processArts();
					filterArt(allArt);
					displayArt(filteredArt);
				}
				break;

			case LOADER_CURSOR_FAVORITES:
				toggleLoading(false);

				if (cursor != null && cursor.moveToFirst()) {
					Utils.d(TAG, "Favorite arts=", cursor.getCount());

					ArrayList<Art> favs = ArtAroundDatabase.artsFromCursor(cursor);

					filtersHeading.setVisibility(View.VISIBLE);
					filtersTitle.setText(R.string.favorites);
					filterArt(favs);
					displayArt(filteredArt);
				}
				else {
					Toast.makeText(ArtMap.this, R.string.empty_favorites, Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {}
	};

	private final LoaderCallbacks<LoaderPayload> asyncCallback = new LoaderCallbacks<LoaderPayload>() {

		@Override
		public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
			switch (id) {
			case LOADER_ASYNC_ARTS:
				return new AsyncLoader<LoaderPayload>(ArtMap.this) {

					@Override
					public LoaderPayload loadInBackground() {
						LoaderPayload payload;
						try {
							ParseResult result = ServiceFactory.getArtService().getArts(args.getInt(ARG_PAGE),
									ARTS_PER_PAGE);
							payload = new LoaderPayload(LoaderPayload.STATUS_OK, result);
						}
						catch (ArtAroundException e) {
							payload = new LoaderPayload(LoaderPayload.STATUS_ERROR, e);
						}
						return payload;
					}
				};

			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
			switch (loader.getId()) {
			case LOADER_ASYNC_ARTS:
				if (payload.getStatus() == LoaderPayload.STATUS_OK) {

					ParseResult result = (ParseResult) payload.getResult();
					Utils.d(TAG, "onLoadFinished(): LOADER_ASYNC_ARTS parseResult=", result);

					if (result == null) {
						toggleLoading(false);
						showDialog(DIALOG_WIFI_FAIL);
						return;
					}

					if (result.page == 1) {
						totalCount.set(result.totalCount);

						int tasksLeft = (int) (Math.ceil((double) (result.totalCount - result.count) / result.perPage));
						if (tasksLeft > 0) {
							Bundle args = new Bundle();
							args.putInt(ARG_PAGE, crtPage.getAndIncrement());
							getSupportLoaderManager().restartLoader(LOADER_ASYNC_ARTS, args, asyncCallback);

							Utils.d(TAG, "onLoadFinished(): start loading page=", crtPage.get());
						}
					}
				}
				else {
					toggleLoading(false);
					Toast.makeText(ArtMap.this, R.string.load_arts_error, Toast.LENGTH_SHORT).show();
				}
				break;
			}

			loader.stopLoading();
		}

		@Override
		public void onLoaderReset(Loader<LoaderPayload> loader) {}
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
			Utils.d(TAG, "onChange(): updated new arts from db", allArt.size());

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