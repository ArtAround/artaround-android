package us.artaround.android.ui;

import us.artaround.android.R;
import us.artaround.android.commons.LocationUpdateReceiver;
import us.artaround.android.commons.LocationUpdateable;
import us.artaround.android.commons.LocationUpdater;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class Home extends MapActivity implements LocationUpdateable {
	private static final int UPDATE_LOCATION_DIALOG = 0;

	private MapView mapView;
	private ProgressDialog progress;

	private String provider;
	private Location location;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		Saved holder = (Saved) getLastNonConfigurationInstance();
		if (holder != null) {
			location = holder.location;
			provider = holder.provider;
		}

		setupControls();
	}

	@Override
	protected void onPause() {
		LocationUpdateReceiver.removeListener(this);
		LocationUpdater.cleanup(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		initMyLocation();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Saved holder = new Saved();
		holder.location = location;
		holder.provider = provider;
		return holder;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case UPDATE_LOCATION_DIALOG:
			progress = new ProgressDialog(this);
			progress.setMessage(getString(R.string.updating_location, provider));
			progress.setCancelable(true);
			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					LocationUpdater.cleanup(Home.this);
				}
			});
			return progress;
		default:
			return null;
		}
	}

	private void setupControls() {
		mapView = (MapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);
	}

	private void initMyLocation() {
		if (location == null) {
			// try last known location from all providers enabled
			location = LocationUpdater.getStoredLocation(this);

			if (location == null) {
				LocationUpdateReceiver.addListener(this);
				provider = LocationManager.GPS_PROVIDER;

				showDialog(UPDATE_LOCATION_DIALOG);

				// try GPS first, since is more accurate than network/wireless
				LocationUpdater.requestSingleUpdate(this, provider);
			}
		}
	}

	private void dismissDialog() {
		if (progress != null && progress.isShowing()) {
			dismissDialog(UPDATE_LOCATION_DIALOG);
		}
	}

	private static class Saved {
		Location location;
		String provider;
	}

	@Override
	public void onLocationUpdate(String provider, Location location) {
		this.location = location;
		dismissDialog();

		GeoPoint currentPoint = new GeoPoint((int) (location.getLatitude() * 1000000),
				(int) (location.getLongitude() * 1000000));
		mapView.getController().animateTo(currentPoint);

		Toast.makeText(Home.this, "Location update success!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationUpdateError(String provider, int code) {
		// fail-safe for GPS failure
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			this.provider = LocationManager.NETWORK_PROVIDER;

			if (progress != null && progress.isShowing()) {
				progress.setMessage(getString(R.string.updating_location, provider));
			}

			LocationUpdater.requestSingleUpdate(getApplicationContext(), this.provider);
		}
		// failure to fetch location from all providers
		else {
			dismissDialog();

			Toast.makeText(Home.this, "Location update failure!", Toast.LENGTH_SHORT).show();
		}
	}

}