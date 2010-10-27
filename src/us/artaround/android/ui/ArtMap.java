package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.android.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.tasks.LoadArtTask;
import us.artaround.android.tasks.LoadArtTask.LoadArtCallback;
import us.artaround.models.Art;
import us.artaround.models.ArtDispersionComparator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.OverlayItem;

public class ArtMap extends MapActivity implements LoadArtCallback, ZoomListener {
	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int MAX_CONCURRENT_TASKS = 1;
	public static final int PER_PAGE = 1000;
	public static final int DEFAULT_ZOOM_LEVEL = 11;

	private ArtMapView mapView;
	private MapController mapController;
	
	private ArtItemOverlay artOverlay;
	private ArrayList<Art> allArt;

	private SharedPreferences prefs;
	private GeoPoint currentLocation = new GeoPoint(38899811, -77020373);
	private Set<LoadArtTask> runningTasks;
	private AtomicInteger taskCount;
	private AtomicBoolean runMoreTasks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_map);
		
		runMoreTasks = new AtomicBoolean(false);
		taskCount = new AtomicInteger(0);
		allArt = new ArrayList<Art>();
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
		mapView = (ArtMapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);
		mapView.setZoomLevel(DEFAULT_ZOOM_LEVEL);
		mapView.setZoomListener(this);
		
		artOverlay = new ArtItemOverlay(getResources().getDrawable(R.drawable.pin), this);
		mapView.getOverlays().add(artOverlay);
		
		mapController = mapView.getController();
		mapController.setCenter(currentLocation);
	}

	private void loadArt() {
		Date lastUpdate = getLastUpdate();
		this.allArt.clear();
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
			allArt.addAll(art);
			calculateMaximumDispersion();
			displayArt(filterArt(allArt));
			// save to database
		}
	}
	
	private void reDisplayArt(){
		displayArt(filterArt(allArt));
	}

	private HashMap<Art, OverlayItem> items = new HashMap<Art, OverlayItem>();
	private void displayArt(ArrayList<Art> art){
		//remove art
		Log.d(Utils.TAG,"Removing "+artToRemove.size()+" pins.");
		for(Art a : artToRemove){
			artOverlay.removeOverlay(items.get(a));
		}
		//add new art
		Log.d(Utils.TAG,"Adding "+artToAdd.size()+" pins.");
		for(Art a : artToAdd){
			artOverlay.addOverlay(newOverlay(a));
		}
		//redraw
		artOverlay.doPopulate();
		mapView.invalidate();
	}
	
	private OverlayItem newOverlay(Art a){
		if(items.containsKey(a)){
			return items.get(a);
		}else{
			OverlayItem pin = new OverlayItem(Utils.geo(a.latitude, a.longitude), a.title, a.locationDesc);
			items.put(a, pin);
			return pin;
		}
	}
	
	private void calculateMaximumDispersion(){
		final int allS = allArt.size();
		for(int i=0;i<allS;++i){
			Art a = allArt.get(i);
			for(int j=i+1;j<allS;++j){
				Art b = allArt.get(j);
				float dist = distance(a,b);
				a.mediumDistance += dist;
				b.mediumDistance += dist;
			}
		}
		for(int i=0;i<allS;++i){
			Art a = allArt.get(i);
			a.mediumDistance /= allS;
		}
		Collections.sort(allArt,new ArtDispersionComparator());
		Collections.reverse(allArt);
	}

	private float distance(Art a, Art b) {
		return (float) Math.sqrt(Math.pow(a.latitude - b.latitude,2)+Math.pow(a.longitude - b.longitude, 2));
	}
	
	/*private void displayArt(ArrayList<Art> art) {
		
		if (art != null && !art.isEmpty()) {
			for (int i = 0; i < art.size(); i++) {
				OverlayItem pin = new OverlayItem(Utils.geo(a.latitude, a.longitude), a.title, a.locationDesc);
				artOverlay.addOverlay(pin);
			}
			// re-draw map view
			mapView.invalidate();
		}
	}*/

	private ArrayList<Art> filterArt(ArrayList<Art> art) {
		return filterByZoom( 
			   filterByCategories(
			   art));
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
	}

	private static final int[] MAX_PINS_PER_LEVEL = {3,5,10,15,20,30,40,60}; 
	private static final int MIN_LEVEL = 7;
	private static final int MAX_LEVEL = 15;
	private final ArrayList<Art> artFiltered = new ArrayList<Art>();
	private final ArrayList<Art> artToAdd = new ArrayList<Art>();
	private final ArrayList<Art> artToRemove = new ArrayList<Art>();
	
	private ArrayList<Art> filterByZoom(ArrayList<Art> art) {
		int newNrPins = 0;
		if(newZoom<=MIN_LEVEL){
			newNrPins = 1;
		}else if(newZoom>MAX_LEVEL){
			newNrPins = art.size();
		}else{
			newNrPins = MAX_PINS_PER_LEVEL[newZoom-MIN_LEVEL-1];
		}
		
		int oldNrPins = artFiltered.size();
		
		artToAdd.clear();
		artToRemove.clear();
		
		if(newNrPins > oldNrPins){ //must add pins
			for(int i=oldNrPins;i<newNrPins;++i){
				artToAdd.add(art.get(i));
			}
			artFiltered.addAll(artToAdd);
		}else{ //must remove pins
			for(int i=oldNrPins-1;i>=newNrPins;--i){
				artToRemove.add(artFiltered.get(i));
				artFiltered.remove(i);
			}
		}
		return artFiltered;		
	}

	private ArrayList<Art> filterByCategories(ArrayList<Art> art) {
		return art;
	}

	private int oldZoom;
	private int newZoom;
	@Override
	public void onZoom(int oldZoom, int newZoom) {
		Log.d(Utils.TAG, "Zoom changed from "+oldZoom+" to "+newZoom);
		this.oldZoom = oldZoom;
		this.newZoom = newZoom;
		reDisplayArt();
	}	
}