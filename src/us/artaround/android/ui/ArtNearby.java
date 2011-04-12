package us.artaround.android.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.AddressUpdaterCallback;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.Utils;
import us.artaround.models.Art;
import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtNearby extends ListActivity implements LocationUpdaterCallback, AddressUpdaterCallback {
	public final int MAX_TO_DISPLAY = 50;

	private LocationUpdater locationUpdater;
	private Location location;
	private List<Art> arts;

	private Animation rotateAnim;
	private ImageView imgRefresh;
	private TextView tvTitle;
	private View loading;
	private ImageButton btnLocation;
	private final NumberFormat distanceFormat = NumberFormat.getInstance();
	{
		distanceFormat.setMaximumFractionDigits(2);
		distanceFormat.setMinimumFractionDigits(2);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_nearby);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		setupUi();

		locationUpdater = new LocationUpdater(this);
		arts = (List<Art>) getIntent().getSerializableExtra("arts");

		location = (Location) getLastNonConfigurationInstance();
		if (location == null) {
			startLocationUpdate();
		}
		else {
			//TODO save these properly
			onLocationUpdate(location);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return location;
	}

	private void setupUi() {
		tvTitle = (TextView) findViewById(R.id.app_label);
		tvTitle.setText(R.string.art_nearby);
		tvTitle.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				scrollUpList();
			}
		});
		
		btnLocation = (ImageButton) findViewById(R.id.btn_location);
		btnLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		imgRefresh = (ImageView) findViewById(R.id.img_refresh);
		rotateAnim = Utils.getRoateAnim(this);
	}

	private void startLocationUpdate() {
		showLoading();
		tvTitle.setText(getString(R.string.art_nearby));
		locationUpdater.updateLocation();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationUpdater.removeUpdates();
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.location = location;
		locationUpdater.updateAddress(location);

		computeDistances();
		displayItems();
	}

	private void scrollUpList() {
		ListView listView = getListView();
		if (listView.getFirstVisiblePosition() > 1) {
			listView.getHandler().post(runScrollUp);
		}
	}

	private final Runnable runScrollUp = new Runnable() {
		@Override
		public void run() {
			ListView listView = getListView();
			int firstPos = listView.getFirstVisiblePosition();
			int skipCount = firstPos >> 2;

			listView.setSelection(skipCount < 1 ? 0 : firstPos - skipCount);
			scrollUpList();
		}
	};

	private void computeDistances() {
		double currentLatitude = location.getLatitude();
		double currentLongitude = location.getLongitude();
		float[] buf = new float[2];
		int n = arts.size();
		for (int i = 0; i < n; ++i) {
			Art art = arts.get(i);
			Location.distanceBetween(currentLatitude, currentLongitude, art.latitude, art.longitude, buf);
			art._distanceFromCurrentPosition = buf[0]; //distance is in meters
			art._bearingFromCurrentPosition = buf[1];
		}
		Collections.sort(arts, new DistanceFromCurrentPositionComparator());
	}

	private void showLoading() {
		if (loading == null) {
			loading = findViewById(R.id.stub_loading);
		}
		loading.setVisibility(View.VISIBLE);

		btnLocation.setEnabled(false);
		imgRefresh.setVisibility(View.VISIBLE);
		imgRefresh.startAnimation(rotateAnim);
	}
	
	private void hideLoading() {
		if (loading != null) {
			loading.setVisibility(View.GONE);
		}

		btnLocation.setEnabled(true);
		imgRefresh.setVisibility(View.INVISIBLE);
		imgRefresh.clearAnimation();
	}

	private void displayItems() {
		int n = Math.min(MAX_TO_DISPLAY, arts.size());
		ArrayList<Art> data = new ArrayList<Art>(n);

		for (int i = 0; i < n; i++) {
			data.add(arts.get(i));
		}

		ListView listView = getListView();
		listView.setAdapter(new CustomArtAdapter(this, data));
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Art a = arts.get(position);
				//				startActivity(new Intent(ArtNearby.this, ArtInfo.class).putExtra("art", a).putExtra("location",
				//						location));
			}
		});
	}

	@Override
	public void onSuggestLocationSettings() {
		hideLoading();
		Utils.getLocationSettingsDialog(this).show();
	}

	@Override
	public void onLocationUpdateError() {
		hideLoading();
	}

	public static class DistanceFromCurrentPositionComparator implements Comparator<Art> {
		@Override
		public int compare(Art a, Art b) {
			return (int) (a._distanceFromCurrentPosition - b._distanceFromCurrentPosition);
		}
	}

	private class CustomArtAdapter extends ArrayAdapter<Art> {
		private final ArrayList<Art> data;

		public CustomArtAdapter(Context context, ArrayList<Art> data) {
			super(context, R.layout.art_nearby_item, data);
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if(convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.art_nearby_item, parent, false);
				holder = new ViewHolder();
				holder.tvTitle = (TextView) convertView.findViewById(R.id.art_title);
				holder.tvDescription = (TextView) convertView.findViewById(R.id.art_description);
				holder.tvDistance = (TextView) convertView.findViewById(R.id.distance);
				holder.imgDirection = (ImageView) convertView.findViewById(R.id.direction);
				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			Art a = data.get(position);
			holder.tvTitle.setText(a.title);
			holder.tvDescription.setText(a.locationDesc);
			
			String measurement = a._distanceFromCurrentPosition >= 1000 ? " km" : " m";
			String tvDist = null;
			if (a._distanceFromCurrentPosition < 1000)
				tvDist = (int) a._distanceFromCurrentPosition + measurement;
			else
				tvDist = distanceFormat.format(((float) Math.round(a._distanceFromCurrentPosition)) / 1000)
						+ measurement;

			holder.tvDistance.setText(tvDist);

			//EAST is +90deg
			//WEST is -90deg
			//SOUTH is +/-180deg
			//NORTH is 0deg

			//			double ang = a._bearingFromCurrentPosition;
			//			if (between(-22.5, 22.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_n);
			//			}
			//			else if (between(22.5, 67.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_ne);
			//			}
			//			else if (between(67.5, 112.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_e);
			//			}
			//			else if (between(112.5, 157.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_se);
			//			}
			//			else if (between(157.5, 180, ang) || between(-180, -157.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_s);
			//			}
			//			else if (between(-157.5, -112.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_sw);
			//			}
			//			else if (between(-112.5, -67.5, ang)) {
			//				holder.imgDirection.setImageResource(R.drawable.ic_w);
			//			}
			//			else {
			//				holder.imgDirection.setImageResource(R.drawable.ic_nw);
			//			}
			//			
			return convertView;
		}

		private boolean between(double a, double b, double c) {
			return c >= a && c <= b;
		}
	}

	private class ViewHolder {
		TextView tvTitle, tvDescription, tvDistance;
		ImageView imgDirection;
	}

	@Override
	public void onAddressUpdate(String address) {
		hideLoading();
		tvTitle.setText(tvTitle.getText() + " " + address);
	}

	@Override
	public void onAddressUpdateError() {
		hideLoading();
	}
}
