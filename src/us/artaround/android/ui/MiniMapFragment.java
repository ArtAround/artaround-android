package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.SharedPreferencesCompat;
import us.artaround.android.common.Utils;
import us.artaround.android.ui.CurrentLocationOverlay.CurrentOverlayDragCallback;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class MiniMapFragment extends Fragment implements LocationUpdaterCallback, CurrentOverlayDragCallback {
	private static final String TAG = "MiniMap";

	private static final int ZOOM_DEFAULT_LEVEL = 15;

	private boolean isEditMode;
	private double latitude;
	private double longitude;

	private ArtMapView miniMap;
	private TextView tvCoords;
	private Location location;
	private CurrentLocationOverlay currentOverlay;
	private LocationUpdater locationUpdater;

	private LocationSettingsDialog dialog;

	public MiniMapFragment() {}

	public MiniMapFragment(double latitude, double longitude, boolean isEditMode) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.isEditMode = isEditMode;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mini_map_fragment, container, false);
		miniMap = (ArtMapView) view.findViewById(R.id.mini_map);
		miniMap.setZoomLevel(ZOOM_DEFAULT_LEVEL);

		if (isEditMode) {
			tvCoords = (TextView) view.findViewById(R.id.mini_map_coords);
			tvCoords.setVisibility(View.VISIBLE);
			//view.findViewById(R.id.mini_map_label).setVisibility(View.VISIBLE);
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		locationUpdater = new LocationUpdater(getActivity(), this);

		if (isEditMode && location == null) {
			startLocationUpdate();
		}
		else {
			centerMiniMap();
		}
	}

	private void centerMiniMap() {
		if (latitude != 0 && longitude != 0) {
			GeoPoint geo = Utils.geo(latitude, longitude);
			Utils.d(TAG, "centerMiniMap(): geo=", geo);
			currentOverlay = new CurrentLocationOverlay(getActivity(), this, R.drawable.ic_pin, geo, null);
			miniMap.getOverlays().add(currentOverlay);
			miniMap.getController().animateTo(geo);
			miniMap.invalidate();
		}
		else if (location != null) {
			GeoPoint geo = Utils.geo(location);
			Utils.d(TAG, "centerMiniMap(): geo=", geo);
			currentOverlay = new CurrentLocationOverlay(getActivity(), this, R.drawable.ic_pin, geo, R.id.mini_map_drag);
			miniMap.getOverlays().add(currentOverlay);
			miniMap.getController().animateTo(geo);
			miniMap.invalidate();

			if (tvCoords != null) {
				tvCoords.setText(getString(R.string.art_edit_label_minimap_coords) + " " + Utils.formatCoords(location));
			}
		}
	}

	private void startLocationUpdate() {
		Utils.d(TAG, "startLocationUpdate()");

		if (tvCoords != null) {
			tvCoords.setText(R.string.art_edit_label_minimap_loading);
		}
		locationUpdater.updateLocation();
	}

	@Override
	public void onDragOverlay(double latitude, double longitude) {
		if (tvCoords != null) {
			tvCoords.setText(Utils.formatCoords(latitude, longitude));
		}
	}

	public Location getLocation() {
		return location;
	}

	public static class LocationSettingsDialog extends DialogFragment {
		public LocationSettingsDialog() {}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return Utils.getLocationSettingsDialog(getActivity());
		}
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.location = location;
		centerMiniMap();
	}

	@Override
	public void onSuggestLocationSettings() {
		if (tvCoords != null) {
			tvCoords.setText(R.string.location_update_failure);
		}

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (sharedPrefs.getBoolean(Utils.KEY_CHECK_LOCATION_PREFS, true)) {
			FragmentManager fm = getFragmentManager();
			Fragment f = fm.findFragmentByTag("dlgLocation");
			if (f != null) {
				getFragmentManager().beginTransaction().remove(f).commit();
			}
			dialog = new LocationSettingsDialog();
			dialog.show(fm, "dlgLocation");

			Editor edit = sharedPrefs.edit();
			edit.putBoolean(Utils.KEY_CHECK_LOCATION_PREFS, false);
			SharedPreferencesCompat.apply(edit);
		}
	}

	@Override
	public void onLocationUpdateError() {
		if (tvCoords != null) {
			tvCoords.setText(R.string.location_update_failure);
		}
	}
}
