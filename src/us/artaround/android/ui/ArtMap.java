package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.android.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.tasks.LoadArtTask;
import us.artaround.android.tasks.LoadArtTask.LoadArtCallback;
import us.artaround.android.ui.ArtItemOverlay.OverlayTapListener;
import us.artaround.models.Art;
import us.artaround.models.ArtDispersionComparator;
import us.artaround.services.ParseResult;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements LoadArtCallback, OverlayTapListener, LocationListener, ZoomListener {

	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int PER_PAGE = 1000;
	public static final int DEFAULT_ZOOM_LEVEL = 11;

	private static final int[] MAX_PINS_PER_LEVEL = { 3, 5, 10, 20, 30, 40, 60 };
	private static final int MIN_LEVEL = 8;
	private static final int MAX_LEVEL = 15;

	public static final int DIALOG_ART_INFO = 0;
	public static final int DIALOG_SUGGEST_LOCATION_SETTINGS = 1;
	public static final int DIALOG_UPDATE_FAILURE = 2;
	public static final int DIALOG_DATABASE_ACCESS_ERROR = 3;
	
	public static final GeoPoint DEFAULT_LOCATION = new GeoPoint(38895111, -77036365);//Washington 

	public static final int doNothing = 1;

	private ArtMapView mapView;

	private List<Art> allArt;
	private List<Art> artFiltered = new ArrayList<Art>();

	private int newZoom;

	private ArtItemOverlay artOverlay;
	private HashMap<Art, OverlayItem> items = new HashMap<Art, OverlayItem>();

	private SharedPreferences prefs;
	private GeoPoint currentLocation = DEFAULT_LOCATION;
	private LocationManager manager;

	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicInteger howManyMoreTasks;
	private List<Art> arts;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);

		Intent intent = getIntent();

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMySearch(query);
		}

		arts = new ArrayList<Art>();
		howManyMoreTasks = new AtomicInteger(0);
		taskCount = new AtomicInteger(0);
		allArt = new ArrayList<Art>();
		runningTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		setupUi();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		manager.removeUpdates(this);
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
		case DIALOG_DATABASE_ACCESS_ERROR:
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string.database_access_error_title);
			builder.setMessage(R.string.database_access_error_msg);
			builder.setCancelable(true);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
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
		Log.d(Utils.TAG, "Result of task is" + result);
		processLoadedArt(result.art);

		if (result.totalCount == arts.size()) {
			// finished to load all arts; update database cache			
		} else {
			Log.d(Utils.TAG, "Loading more art from server...");
			loadMoreArtFromServer(result, task);
		}
	}

	@Override
	public void onLoadArtError(Throwable e) {
		Log.d(Utils.TAG, "Failed to load art!", e);
		OnClickListener onOk = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ArtMap.this.gotoWifiSettings();
			}
		};
		OnClickListener onCancel = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//TODO
			}
		};
		new AlertDialog.Builder(this)
.setMessage(R.string.connection_fail_message)
				.setPositiveButton(R.string.connection_fail_ok, onOk)
				.setNegativeButton(R.string.connection_fail_cancel, onCancel)
				.show();
	}

	protected void gotoWifiSettings() {
		//go to wireless settings
		startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
	}

	private void setupUi() {
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		mapView.setZoomLevel(newZoom = DEFAULT_ZOOM_LEVEL);
		mapView.setZoomListener(this);
		mapView.getController().setCenter(currentLocation);

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
		this.allArt.clear();
		Log.d(Utils.TAG, "Last art update was " + lastUpdate);

		if (isOutdated(lastUpdate)) {
			loadArtFromServer();
		} else {
			loadArtFromDatabase();
		}
	}

	private void loadArtFromDatabase() {

	}

	private boolean isLoadingArt() {
		return !runningTasks.isEmpty();
	}

	private void loadArtFromServer() {
		Log.d(Utils.TAG, "Loading art from server...");
		if (isLoadingArt()) {
			return;
		}

		howManyMoreTasks.set(1);
		taskCount.set(MAX_CONCURRENT_TASKS);
		for (int page = 1; page <= MAX_CONCURRENT_TASKS; page++) {
			startTask(page);
		}
	}

	private void loadMoreArtFromServer(ParseResult result, LoadArtTask task) {
		runningTasks.remove(task);
		howManyMoreTasks.decrementAndGet();

		if (result.page == 1) {
			howManyMoreTasks.set((int) (Math.floor(result.totalCount - result.count) / result.perPage));
		}

		Log.d(Utils.TAG, "There are " + howManyMoreTasks.get() + " more tasks to start.");

		if (result.count == result.perPage && howManyMoreTasks.get() > 0) {
			if (runningTasks.size() < MAX_CONCURRENT_TASKS) {
				startTask(taskCount.incrementAndGet());
			}
		}
	}

	private void startTask(int page) {
		new LoadArtTask(this).execute(PER_PAGE, page);
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

	private void processLoadedArt(List<Art> art) {
		if (art != null && !art.isEmpty()) {
			allArt.addAll(art);
			calculateMaximumDispersion();
			displayArt(filterArt(allArt));
			// save to database
		}
	}

	private void reDisplayArt() {
		displayArt(filterArt(allArt));
	}

	private void displayArt(RenderingContext art) {
		//remove art
		Log.d(Utils.TAG, "Removing " + art.artToRemove.size() + " pins.");
		for (Art a : art.artToRemove) {
			artOverlay.removeOverlay(items.get(a));
		}
		//add new art
		Log.d(Utils.TAG, "Adding " + art.artToAdd.size() + " pins.");
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
			OverlayItem pin = new OverlayItem(Utils.geo(a.latitude, a.longitude), a.title, a.locationDesc);
			items.put(a, pin);
			return pin;
		}
	}

	private void calculateMaximumDispersion() {
		final int allS = allArt.size();
		for (int i = 0; i < allS; ++i) {
			Art a = allArt.get(i);
			for (int j = i + 1; j < allS; ++j) {
				Art b = allArt.get(j);
				float dist = distance(a, b);
				a.mediumDistance += dist;
				b.mediumDistance += dist;
			}
		}
		for (int i = 0; i < allS; ++i) {
			Art a = allArt.get(i);
			a.mediumDistance /= allS;
		}
		Collections.sort(allArt, new ArtDispersionComparator());
		Collections.reverse(allArt);
	}

	private float distance(Art a, Art b) {
		return (float) Math.sqrt(Math.pow(a.latitude - b.latitude, 2) + Math.pow(a.longitude - b.longitude, 2));
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
		Log.d(Utils.TAG, "nr pins = " + newNrPins);

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
		Log.d(Utils.TAG, "Zoom changed from " + oldZoom + " to " + newZoom);
		//this.oldZoom = oldZoom;
		this.newZoom = newZoom;
		reDisplayArt();
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
			showDialog(DIALOG_UPDATE_FAILURE);
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
}