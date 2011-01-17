package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class ArtDetails extends MapActivity implements OverlayTapListener {
	private Art art;
	private boolean isEditing;

	private ArtField[] fields;
	private View miniMapWrap;
	private Button btnSubmit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_details);

		setupVars();
		setupUi();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupVars() {
		Intent i = getIntent();
		art = (Art) i.getSerializableExtra("art");
	}

	private void setupActionBarUi() {
		TextView titlebar = (TextView) getParent().findViewById(R.id.app_label);
		titlebar.setText(art.title);

		getParent().findViewById(R.id.btn_0).setVisibility(View.GONE);

		LoadingButton btnHome = (LoadingButton) getParent().findViewById(R.id.btn_1);
		btnHome.setImageResource(R.drawable.ic_btn_home);
		btnHome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doFinish();
			}
		});

		LoadingButton btnEdit = (LoadingButton) getParent().findViewById(R.id.btn_2);
		btnEdit.setImageResource(R.drawable.ic_btn_edit);
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doEditArt();
			}
		});

		btnSubmit = (Button) findViewById(R.id.btn_submit);
	}

	private void setupUi() {
		setupActionBarUi();
		setupFields();
		setupMiniMap();
	}

	private void setupMiniMap() {
		miniMapWrap = findViewById(R.id.mini_map_wrap);
		MapView miniMap = (MapView) findViewById(R.id.mini_map);
		miniMap.setBuiltInZoomControls(false);

		ArtOverlay artOverlay = new ArtOverlay(getResources().getDrawable(R.drawable.ic_pin), this);
		artOverlay.addOverlay(new ArtOverlayItem(art));
		miniMap.getOverlays().add(artOverlay);

		artOverlay.doPopulate();
		miniMap.invalidate();

		MapController controller = miniMap.getController();
		controller.setZoom(miniMap.getMaxZoomLevel());
		controller.animateTo(Utils.geo(art.latitude, art.longitude));
	}

	private void setupFields() {

		ArtField title = (ArtField) findViewById(R.id.title);
		title.setLabelText(getString(R.string.label_title));
		if (!TextUtils.isEmpty(art.title)) title.setValueText(art.title);

		ArtField artist = (ArtField) findViewById(R.id.artist);
		artist.setLabelText(getString(R.string.label_artist));
		if (!TextUtils.isEmpty(art.artist.name)) artist.setValueText(art.artist.name);

		ArtField year = (ArtField) findViewById(R.id.year);
		year.setLabelText(getString(R.string.label_year));
		if (art.year > 0) year.setValueText("" + art.year);

		ArtField ward = (ArtField) findViewById(R.id.ward);
		ward.setLabelText(getString(R.string.label_ward));
		if (art.ward > 0) ward.setValueText("" + art.ward);

		ArtField locationDesc = (ArtField) findViewById(R.id.location_desc);
		locationDesc.setLabelText(getString(R.string.label_location_desc));
		if (!TextUtils.isEmpty(art.locationDesc)) locationDesc.setValueText(art.locationDesc);

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(title.getWindowToken(), 0);

		fields = new ArtField[] { title, artist, year, ward, locationDesc };
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

	private void doEditArt() {
		isEditing = !isEditing;

		int size = fields.length;
		for (int i = 0; i < size; i++) {
			fields[i].setEditing(isEditing);
		}

		miniMapWrap.setVisibility(isEditing ? View.GONE : View.VISIBLE);
		btnSubmit.setVisibility(isEditing ? View.VISIBLE : View.GONE);
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
	public void onTap(Object item) {
		startActivity(Utils.getHomeIntent(this).putExtra("toLat", art.latitude).putExtra("toLong", art.longitude));
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			doFinish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}

	private void doFinish() {
		Intent iHome = Utils.getHomeIntent(this);
		iHome.putExtras(getIntent()); // saved things from ArtMap
		startActivity(iHome);
		finish();
	}
}
