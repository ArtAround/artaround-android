package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.LocatorFragment;
import us.artaround.android.common.LocatorFragment.LocatorCallback;
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

public class MiniMapFragment extends Fragment implements LocatorCallback, CurrentOverlayDragCallback {
	private static final String TAG = "ArtAround.MiniMapFragment";

	public static final String ARG_EDIT_MODE = "edit_mode";

	private static final String SAVE_LOCATION = "location";

	private boolean isEditMode;

	private ArtMapView miniMap;
	private TextView tvCoords;
	private Location location;
	private CurrentLocationOverlay currentOverlay;

	private LocationSettingsDialog dialog;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setRetainInstance(true);
		Bundle args = getArguments();
		isEditMode = args.getBoolean(ARG_EDIT_MODE, false);

		Utils.d(TAG, "onAttach()");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mini_map_fragment, container, false);
		miniMap = (ArtMapView) view.findViewById(R.id.mini_map);

		if (isEditMode) {
			tvCoords = (TextView) view.findViewById(R.id.mini_map_coords);
			tvCoords.setVisibility(View.VISIBLE);
			view.findViewById(R.id.mini_map_label).setVisibility(View.VISIBLE);
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Utils.d(TAG, "onActivityCreated(): savedInstanceState=" + savedInstanceState);

		if (savedInstanceState != null) {
			location = savedInstanceState.getParcelable(SAVE_LOCATION);
		}
		if (location == null) {
			startLocationUpdate();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(SAVE_LOCATION, location);
		super.onSaveInstanceState(outState);
	}

	private void startLocationUpdate() {
		Utils.d(TAG, "startLocationUpdate()");

		if (tvCoords != null) {
			tvCoords.setText(R.string.art_edit_label_minimap_loading);
		}
		Bundle args = new Bundle();
		args.putBoolean(LocatorFragment.ARG_ADDRESS_UPDATE, false);

		LocatorFragment f = new LocatorFragment(this);
		f.setArguments(args);
		f.setTargetFragment(this, 0);
		getFragmentManager().beginTransaction().add(f, "locator").commit();
	}

	@Override
	public void onDragOverlay(double latitude, double longitude) {
		if (tvCoords != null) {
			tvCoords.setText(Utils.formatCoords(latitude, longitude));
		}
	}

	@Override
	public void onLocationUpdate(Location location) {
		Utils.d(TAG, "onLocationUpdate(): location=" + location);
		this.location = location;

		GeoPoint geo = Utils.geo(location);
		currentOverlay = new CurrentLocationOverlay(getActivity(), this, R.drawable.ic_pin, geo, R.id.mini_map_drag);
		miniMap.getOverlays().add(currentOverlay);
		miniMap.getController().animateTo(geo);
		miniMap.invalidate();

		if (tvCoords != null) {
			tvCoords.setText(Utils.formatCoords(location));
		}
	}

	@Override
	public void onLocationUpdateError(int errorCode) {
		Utils.d(TAG, "onLocationUpdateError(): errorCode=" + errorCode);
		if (errorCode == LocatorFragment.ERROR_NO_PROVIDER) {
			FragmentManager fm = getFragmentManager();
			
			if (fm == null) return; //FIXME wtf?! fm is null?!

			Fragment f = fm.findFragmentByTag("dlgLocation");
			if (f != null) {
				getFragmentManager().beginTransaction().remove(f).commit();
			}
			dialog = new LocationSettingsDialog();
			dialog.show(fm, "dlgLocation");
		}

		if (tvCoords != null) {
			tvCoords.setText(R.string.location_update_failure);
		}
	}

	public static class LocationSettingsDialog extends DialogFragment {
		public LocationSettingsDialog() {}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return Utils.getLocationSettingsDialog(getActivity());
		}
	}
}
