package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.Utils;
import us.artaround.android.ui.CurrentLocationOverlay.CurrentOverlayDragCallback;
import android.app.Activity;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

//FIXME why this fragment doesn't save its own state?
public class MiniMapFragment extends Fragment implements LocationUpdaterCallback, CurrentOverlayDragCallback {
	private static final String TAG = "MiniMap";

	private static final int ZOOM_DEFAULT_LEVEL = 15;

	public static final String ARG_EDIT_MODE = "edit_mode";
	public static final String ARG_LATITUDE = "latitude";
	public static final String ARG_LONGITUDE = "longitude";

	private static final String SAVE_LOCATION = "location";

	private boolean isEditMode;
	private double latitude;
	private double longitude;

	private ArtMapView miniMap;
	private TextView tvCoords;
	private Location location;
	private CurrentLocationOverlay currentOverlay;
	private LocationUpdater locationUpdater;

	private LocationSettingsDialog dialog;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setRetainInstance(true);

		Bundle args = getArguments();
		isEditMode = args.getBoolean(ARG_EDIT_MODE, false);
		latitude = args.getDouble(ARG_LATITUDE);
		longitude = args.getDouble(ARG_LONGITUDE);

		Utils.d(TAG, "onAttach()");
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
		Utils.d(TAG, "onActivityCreated(): savedInstanceState=", savedInstanceState);

		locationUpdater = new LocationUpdater(getActivity(), this);

		if (savedInstanceState != null) {
			location = savedInstanceState.getParcelable(SAVE_LOCATION);
		}
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(SAVE_LOCATION, location);
		super.onSaveInstanceState(outState);
		Utils.d(TAG, "onSaveInstanceState(): outState=", outState);
	}

	private void startLocationUpdate() {
		Utils.d(TAG, "startLocationUpdate()");

		if (tvCoords != null) {
			tvCoords.setText(R.string.art_edit_label_minimap_loading);
		}
		locationUpdater.updateLocation();

		// FIXME fix this hard-coded thing
		if (getActivity() instanceof ArtEdit) {
			((ArtEdit) getActivity()).toggleLoading(true);
		}
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
		if (getActivity() == null) return;

		Utils.d(TAG, "onLocationUpdate(): location=", location);
		this.location = location;

		centerMiniMap();

		//FIXME remove this
		if (getActivity() instanceof ArtEdit) {
			((ArtEdit) getActivity()).toggleLoading(false);
		}
	}

	@Override
	public void onSuggestLocationSettings() {
		// FIXME fix this hard-coded thing
		if (getActivity() instanceof ArtEdit) {
			((ArtEdit) getActivity()).toggleLoading(true);
		}

		if (tvCoords != null) {
			tvCoords.setText(R.string.location_update_failure);
		}

		FragmentManager fm = getFragmentManager();
		Fragment f = fm.findFragmentByTag("dlgLocation");
		if (f != null) {
			getFragmentManager().beginTransaction().remove(f).commit();
		}
		dialog = new LocationSettingsDialog();
		dialog.show(fm, "dlgLocation");
	}

	@Override
	public void onLocationUpdateError() {
		// FIXME fix this hard-coded thing
		if (getActivity() instanceof ArtEdit) {
			((ArtEdit) getActivity()).toggleLoading(true);
		}

		if (tvCoords != null) {
			tvCoords.setText(R.string.location_update_failure);
		}
	}
}
