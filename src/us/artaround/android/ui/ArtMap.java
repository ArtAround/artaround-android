package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.android.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.tasks.LoadArtTask;
import us.artaround.android.tasks.LoadArtTask.LoadArtCallback;
import us.artaround.models.Art;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class ArtMap extends MapActivity implements LoadArtCallback {

	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 3;
	public static final int PER_PAGE = 20;

	private MapView mapView;

	private SharedPreferences prefs;
	private GeoPoint currentLocation = new GeoPoint(38895110, -77036370);
	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicBoolean runMoreTasks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);
		
		runMoreTasks = new AtomicBoolean(false);
		taskCount = new AtomicInteger(0);
		runningTasks = Collections.synchronizedSet(new HashSet<LoadArtTask>());
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		setupMap();
		loadArt();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void setupMap() {
		mapView = (MapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);
		
		MapController mc = mapView.getController();
		mc.setCenter(currentLocation);
        mc.setZoom(11);
	}

	private void loadArt() {
		Date lastUpdate = getLastUpdate();
		if(isOutdated(lastUpdate)) {
			loadArtFromServer();
		} else {
			loadArtFromDatabase();
		}
	}
	
	private void loadArtFromDatabase() {
		// TODO Auto-generated method stub

	}

	private void loadArtFromServer() {
		if (isLoadingArt()) {
			return;
		}

		runMoreTasks.set(true);
		taskCount.set(MAX_CONCURRENT_TASKS);
		for (int page = 1; page <= MAX_CONCURRENT_TASKS; page++) {
			startTask(page);
		}
	}

	private void loadMoreArtFromServer(ArrayList<Art> art, LoadArtTask task) {
		runningTasks.remove(task);
		if (art != null && art.size() == PER_PAGE && runMoreTasks.get()) {
			if (runningTasks.size() < MAX_CONCURRENT_TASKS) {
				startTask(taskCount.incrementAndGet());
			}
		} else {
			runMoreTasks.set(false);
		}
	}

	private boolean isLoadingArt() {
		return !runningTasks.isEmpty();
	}

	@Override
	public void onLoadArt(ArrayList<Art> art, LoadArtTask task) {
		loadMoreArtFromServer(art, task);
		processLoadedArt(art);
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

	private void processLoadedArt(ArrayList<Art> art) {
		if (art != null && !art.isEmpty()) {
			displayArt(filterArt(art));
			// save to database
		}
	}

	private void displayArt(ArrayList<Art> art) {
		if (art != null && !art.isEmpty()) {

		}
	}

	private ArrayList<Art> filterArt(ArrayList<Art> art) {
		String categoriesStr = prefs.getString(Utils.KEY_CATEGORIES, null);

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
		return null;
	}
	

}