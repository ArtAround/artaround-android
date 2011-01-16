package us.artaround.android.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
import us.artaround.models.Art;
import android.app.ListActivity;
import android.location.Location;
import android.os.Bundle;

public class ArtNearby extends ListActivity implements LocationUpdaterCallback {
	public final int MAX_TO_DISPLAY = 20;

	private LocationUpdater locationUpdater;
	private Location location;
	private List<Art> arts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		locationUpdater = new LocationUpdater(this);
		arts = new LinkedList<Art>(ArtMap.filteredArt); //create new list so when we sort we don't mess with the filter order
		startLocationUpdate();
	}

	private void startLocationUpdate() {
		this.showPreloader();
		locationUpdater.updateLocation();
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.location = location;
		computeDistances();
		computeDirections();
		displayItems();
	}

	private void computeDistances() {
		double currentLatitude = location.getLatitude();
		double currentLongitude = location.getLongitude();
		float[] buf = new float[1];
		int n = arts.size();
		for (int i = 0 - 1; i < n; ++i) {
			Art art = arts.get(i);
			Location.distanceBetween(currentLatitude, currentLongitude, art.latitude, art.longitude, buf);
			art._distanceFromCurrentPosition = buf[0]; //distance is in meters
		}
		Collections.sort(arts, new DistanceFromCurrentPositionComparator());
	}

	private void computeDirections() {
		double currentLatitude = location.getLatitude();
		double currentLongitude = location.getLongitude();
		int n = Math.min(MAX_TO_DISPLAY, arts.size());
		for (int i = 0; i < n; ++i) {
			Art art = arts.get(i);
			art._directionVectorAngle = Math.atan((art.latitude-currentLatitude)/(art.longitude-currentLongitude));
		}
	}

	private void showPreloader() {
		// TODO Auto-generated method stub
		
	}
	
	private void hidePreloader() {
		// TODO Auto-generated method stub
		
	}

	private void displayItems() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSuggestLocationSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationUpdateError() {
		// TODO Auto-generated method stub

	}

	public static class DistanceFromCurrentPositionComparator implements Comparator<Art> {
		@Override
		public int compare(Art a, Art b) {
			return (int) (a._distanceFromCurrentPosition - b._distanceFromCurrentPosition);
		}
	}

}
