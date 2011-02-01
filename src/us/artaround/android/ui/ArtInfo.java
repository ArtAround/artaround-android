package us.artaround.android.ui;

import java.util.List;

import us.artaround.R;
import us.artaround.android.commons.BackgroundCommand;
import us.artaround.android.commons.LoadFlickrPhotosCommand;
import us.artaround.android.commons.LoadingTask;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.navigation.Navigation;
import us.artaround.android.commons.navigation.Navigation.NavigationListener;
import us.artaround.android.commons.navigation.Route;
import us.artaround.android.commons.navigation.RouteLineOverlay;
import us.artaround.android.services.FlickrService;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.maps.GeoPoint;

public class ArtInfo extends NewArtInfo implements OverlayTapListener, NavigationListener {
	private final static int LOAD_FLICKR_PHOTOS = 200;

	private boolean isRoadShowing;
	private Navigation navigation;

	// TODO support download multiple photos

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void setupState() {
		super.setupState();
		// TODO restore tasks

		if (art.photoIds != null && art.photoIds.size() > 0) {
			new LoadingTask(this, new LoadFlickrPhotosCommand(LOAD_FLICKR_PHOTOS, art.photoIds.get(0),
					FlickrService.SIZE_MEDIUM)).execute();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder h = (Holder) super.onRetainNonConfigurationInstance();
		return h;
	}

	@Override
	protected void setupUi() {
		super.setupUi();
		viewFlipper.addView(getLayoutInflater().inflate(R.layout.art_comments, null));
		fields[0].setVisibility(View.GONE);
	}

	@Override
	protected void setupVars() {
		super.setupVars();
		art = (Art) getIntent().getSerializableExtra("art");
		isModeEditing = false;
		navigation = new Navigation(true);
	}

	@Override
	protected void setupMiniMap() {
		super.setupMiniMap();
		ArtOverlay artOverlay = new ArtOverlay(getResources().getDrawable(R.drawable.ic_pin), this);
		artOverlay.addOverlay(new ArtOverlayItem(art));
		miniMap.getOverlays().add(artOverlay);

		artOverlay.doPopulate();
		miniMap.invalidate();
		miniMap.getController().animateTo(Utils.geo(art.latitude, art.longitude));
	}

	@Override
	protected void changePageTitle(int page) {
		if (page == 3) {
			tvAppTitle.setText(R.string.art_comments);
		}
		else {
			super.changePageTitle(page);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.art_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share_art:
			doShareArt();
			return true;
		case R.id.favorite_art:
			doFavoriteArt();
			return true;
		case R.id.streetview:
			startActivity(Utils.getStreetViewIntent(art.latitude, art.longitude));
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private String getShareText() {
		StringBuilder b = new StringBuilder(getString(R.string.share_art_header));
		b.append(Utils.NL).append(getString(R.string.label_artist)).append(":");
		if (art.artist != null) {
			b.append(art.artist.name);
		}
		else {
			b.append(getString(R.string.value_unset));
		}
		b.append(Utils.NL).append(art.locationDesc);
		b.append(Utils.NL).append(getString(R.string.label_category)).append(":").append(art.category);
		b.append(Utils.NL).append(getString(R.string.label_neighborhood)).append(":").append(art.neighborhood);
		return b.toString();
	}

	private void doShareArt() {
		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
				.putExtra(Intent.EXTRA_TEXT, getShareText());
		startActivity(Intent.createChooser(intent, getString(R.string.share_art_title)));
	}

	private void doFavoriteArt() {
		//TODO
	}


	@Override
	public void onSuggestLocationSettings() {
		tvFooter.setText(R.string.location_update_failure);
		showLoading(false);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.currentLocation = location;
		this.currentGeo = Utils.geo(location);
		tvFooter.setText(getString(R.string.current_location) + " "
				+ Utils.coordinateFormatter.format(location.getLatitude()) + ", "
				+ Utils.coordinateFormatter.format(location.getLongitude()));
		showLoading(false);
	}

	@Override
	public void onLocationUpdateError() {
		tvFooter.setText(getString(R.string.location_update_failure));
		showLoading(false);
	}

	private void showRoad() {
		if (currentGeo == null) {
			return;
		}
		navigation.navigateTo(currentGeo, Utils.geo(art.latitude, art.longitude), Navigation.TYPE_WALKING, this);
	}

	@Override
	public void onNavigationAvailable(int type, Route route) {
		List<GeoPoint> points = route.getGeoPoints();
		RouteLineOverlay rlo = new RouteLineOverlay(this, miniMap, points);
		miniMap.getOverlays().add(rlo);
		miniMap.getOverlays().add(
				new CurrentOverlay(this, getResources().getDrawable(R.drawable.ic_pin_current), currentGeo, false));
		miniMap.invalidate();

		String dist = route.getTotalDistance();
		if (!TextUtils.isEmpty(dist)) {
			tvFooter.setText(tvFooter.getText() + " " + dist);
		}
	}

	@Override
	public void onNavigationUnavailable(GeoPoint startPoint, GeoPoint endPoint) {
		tvFooter.setText("Can't get directions from " + startPoint + " to " + endPoint);
	}

	@Override
	public void onTap(Object item) {
		Utils.d(Utils.TAG, "Tapping to show road from current geo " + currentGeo);
		if (!isRoadShowing && currentGeo != null) {
			showRoad();
		}
	}

	@Override
	public void beforeLoadingTask(BackgroundCommand command) {
		super.beforeLoadingTask(command);
		if (command.getToken() == LOAD_FLICKR_PHOTOS) {
			showLoading(true);
		}
	}

	@Override
	public void afterLoadingTask(BackgroundCommand command, Object result) {
		super.afterLoadingTask(command, result);
		if (command.getToken() == LOAD_FLICKR_PHOTOS) {
			showLoading(false);
			Uri uri = (Uri) result;
			if (uri != null) {
				allUris.add(uri);
				setGallerySelection(allUris.size() - 1);
			}
		}
	}

	@Override
	public void onLoadingTaskError(BackgroundCommand command, ArtAroundException exception) {
		super.onLoadingTaskError(command, exception);
		if (command.getToken() == LOAD_FLICKR_PHOTOS) {
			showLoading(false);
		}
	}

}
