package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import us.artaround.R;
import us.artaround.android.commons.LocationUpdater;
import us.artaround.android.commons.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtNearby extends ListActivity implements LocationUpdaterCallback {
	public final int MAX_TO_DISPLAY = 100;

	private static final float RAD_TO_DEG = 57.29f;

	private LocationUpdater locationUpdater;
	private Location location;
	private List<Art> arts;

	private View loading;
	private LoadingButton btnLoading;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_nearby);

		setupUi();
		locationUpdater = new LocationUpdater(this);
		arts = (List<Art>) getIntent().getSerializableExtra("arts"); //create new list so when we sort we don't mess with the filter order
		startLocationUpdate();
	}

	private void setupUi() {
		TextView title = (TextView) findViewById(R.id.app_label);
		title.setText(R.string.art_nearby);
		
		findViewById(R.id.btn_0).setVisibility(View.GONE);
		btnLoading = (LoadingButton) findViewById(R.id.btn_1);
		btnLoading.setImageResource(R.drawable.ic_btn_refresh);
		btnLoading.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startLocationUpdate();
			}
		});

		LoadingButton btnHome = (LoadingButton) findViewById(R.id.btn_2);
		btnHome.setImageResource(R.drawable.ic_btn_home);
		btnHome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Utils.getHomeIntent(ArtNearby.this));
			}
		});
	}

	private void startLocationUpdate() {
		showLoading();
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
		hideLoading();
		computeDistances();
		computeDirections();
		displayItems();
	}

	private void computeDistances() {
		double currentLatitude = location.getLatitude();
		double currentLongitude = location.getLongitude();
		float[] buf = new float[1];
		int n = arts.size();
		for (int i = 0; i < n; ++i) {
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
			art._directionVectorAngle = RAD_TO_DEG
					* Math.atan((art.latitude - currentLatitude) / (art.longitude - currentLongitude));
		}
	}

	private void showLoading() {
		if (loading == null) {
			loading = findViewById(R.id.stub_loading);
		}
		loading.setVisibility(View.VISIBLE);
		btnLoading.showLoading(true);

	}
	
	private void hideLoading() {
		if (loading != null) {
			loading.setVisibility(View.GONE);
		}
		btnLoading.showLoading(false);
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
				startActivity(new Intent(ArtNearby.this, ArtPage.class).putExtra("slug", a.slug));
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
		private ArrayList<Art> data;

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
			float dist = a._distanceFromCurrentPosition < 1000 ? (int) a._distanceFromCurrentPosition : Math
					.round(a._distanceFromCurrentPosition) / 1000;

			holder.tvDistance.setText(dist + measurement);

			double ang = a._directionVectorAngle;
			if (between(0, 30, ang) || between(330, 360, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_n);
			}
			else if (between(31, 60, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_ne);
			}
			else if (between(61, 120, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_e);
			}
			else if (between(121, 150, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_se);
			}
			else if (between(151, 210, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_s);
			}
			else if (between(211, 240, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_sw);
			}
			else if (between(241, 300, ang)) {
				holder.imgDirection.setImageResource(R.drawable.ic_w);
			}
			else {
				holder.imgDirection.setImageResource(R.drawable.ic_nw);
			}
			
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
}
