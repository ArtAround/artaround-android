package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.NotifyingAsyncQueryHandler;
import us.artaround.android.common.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.common.Utils;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.LoadCategoriesCommand;
import us.artaround.android.common.task.LoadFlickrPhotosCommand;
import us.artaround.android.common.task.LoadNeighborhoodsCommand;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class ArtEdit extends ArtAroundMapActivity implements NotifyingAsyncQueryListener, LocationUpdaterCallback {
	public static final String EXTRA_ART = "art";

	private static final int QUERY_CATEGORIES = 0;
	private static final int QUERY_NEIGHBORHOODS = 1;
	private static final int QUERY_ARTISTS = 2;

	private static final int LOAD_PHOTOS = 3;

	public static final String SAVE_LOCATION = "location";
	public static final String SAVE_SCROLL_Y = "scroll_y";
	public static final String SAVE_ALL_PHOTO_URIS = "all_photo_uris";
	public static final String SAVE_NEW_PHOTO_URIS = "new_photo_uris";

	private static final int DIALOG_LOCATION_SETTINGS = 0;

	private Art art;
	private Location location;
	private ArrayList<String> allPhotoUris, newPhotoUris;

	private NotifyingAsyncQueryHandler queryHandler;
	private LocationUpdater locationUpdater;

	private TextView tvLabelMinimap;
	private EditText tvName;
	private AutoCompleteTextView tvArtist;
	private AutoCompleteTextView tvCategory;
	private AutoCompleteTextView tvNeighborhood;

	private ImageView imgLoader;
	private Animation rotateAnim;

	private Button btnSubmit;
	private Button btnCancel;

	private ScrollView scrollView;
	private Gallery gallery;
	private ArtMapView minimap;
	private CurrentOverlay currentOverlay;

	@Override
	protected void onChildCreate(Bundle savedInstanceState) {
		setContentView(R.layout.art_edit);

		// --- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		setupVars(savedInstanceState);
		setupUi();
	}

	@Override
	protected void onChildEndCreate(Bundle savedInstanceState) {
		setupState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (location == null) {
			startLocationUpdate();
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			final int scrollY = savedInstanceState.getInt(SAVE_SCROLL_Y, 0);
			if (scrollY > 0) {
				Utils.d(TAG, "saved scrollY=" + scrollY);
				scrollView.post(new Runnable() {
					@Override
					public void run() {
						scrollView.scrollTo(0, scrollY);
					}
				});
			}
			location = savedInstanceState.getParcelable(SAVE_LOCATION);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAVE_SCROLL_Y, scrollView.getScrollY());
		outState.putParcelable(SAVE_LOCATION, location);
		outState.putStringArrayList(SAVE_ALL_PHOTO_URIS, allPhotoUris);
		outState.putStringArrayList(SAVE_NEW_PHOTO_URIS, newPhotoUris);
		super.onSaveInstanceState(outState);
	}

	protected void setupVars(Bundle savedInstanceState) {
		Intent i = getIntent();
		if (i.hasExtra(EXTRA_ART)) {
			art = (Art) i.getSerializableExtra(EXTRA_ART);
		}
		else {
			art = new Art(); // will be used as a "holder" for user input
		}

		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
		locationUpdater = new LocationUpdater(this);

		if (savedInstanceState != null) {
			allPhotoUris = savedInstanceState.getStringArrayList(SAVE_ALL_PHOTO_URIS);
			newPhotoUris = savedInstanceState.getStringArrayList(SAVE_NEW_PHOTO_URIS);
		}

		if (allPhotoUris == null) {
			allPhotoUris = new ArrayList<String>();
		}
		if (newPhotoUris == null) {
			newPhotoUris = new ArrayList<String>();
		}
	}

	protected void setupUi() {
		imgLoader = (ImageView) findViewById(R.id.actionbar_loader);
		rotateAnim = Utils.getRoateAnim(this);

		btnSubmit = (Button) findViewById(R.id.btn_submit);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (validateTexts()) {
					//TODO submit to server
				}
			}
		});
		btnCancel = (Button) findViewById(R.id.btn_cancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO go back to main screen
			}
		});

		scrollView = (ScrollView) findViewById(R.id.scroll_view);

		setupArtFields();
		setupMiniMap();
		setupGallery();
	}

	private boolean validateTexts() {
		boolean ok = true;
		if (TextUtils.isEmpty(tvName.getText())) {
			tvName.setHint(getString(R.string.art_edit_hint_input_empty));
			ok = false;
		}
		if (TextUtils.isEmpty(tvCategory.getText())) {
			tvCategory.setHint(getString(R.string.art_edit_hint_input_empty));
			ok = false;
		}
		if (TextUtils.isEmpty(tvNeighborhood.getText())) {
			tvNeighborhood.setHint(getString(R.string.art_edit_hint_input_empty));
			ok = false;
		}
		return ok;
	}

	private void setupArtFields() {
		tvName = (EditText) findViewById(R.id.art_edit_input_name);
		if (!TextUtils.isEmpty(art.title)) {
			tvName.setText(art.title);
		}

		tvArtist = (AutoCompleteTextView) findViewById(R.id.art_edit_input_artist);
		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) {
			tvArtist.setText(art.artist.name);
		}

		EditText tvYear = (EditText) findViewById(R.id.art_edit_input_year);
		if (art.year > 0) {
			tvYear.setText(String.valueOf(art.year));
		}

		tvCategory = (AutoCompleteTextView) findViewById(R.id.art_edit_input_category);
		if (!TextUtils.isEmpty(art.category)) {
			tvCategory.setText(art.category);
		}

		EditText tvDesc = (EditText) findViewById(R.id.art_edit_input_description);
		if (!TextUtils.isEmpty(art.description)) {
			tvDesc.setText(art.description);
		}

		EditText tvWard = (EditText) findViewById(R.id.art_edit_input_ward);
		if (art.ward > 0) {
			tvWard.setText(String.valueOf(art.ward));
		}

		tvNeighborhood = (AutoCompleteTextView) findViewById(R.id.art_edit_input_neighborhood);
		if (!TextUtils.isEmpty(art.neighborhood)) {
			tvNeighborhood.setText(art.neighborhood);
		}

		EditText tvLocDesc = (EditText) findViewById(R.id.art_edit_input_location_description);
		if (!TextUtils.isEmpty(art.locationDesc)) {
			tvLocDesc.setText(art.locationDesc);
		}

		tvLabelMinimap = (TextView) findViewById(R.id.art_edit_label_minimap);
	}

	private void setupMiniMap() {
		minimap = (ArtMapView) findViewById(R.id.minimap);
		minimap.setBuiltInZoomControls(false);
	}

	private void setupGallery() {
		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new GalleryAdapter(this, allPhotoUris));
		gallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				//TODO open up the full-screen gallery
			}
		});
	}

	protected void toggleActionbarLoader(boolean show) {
		if (show) {
			imgLoader.setVisibility(View.VISIBLE);
			imgLoader.startAnimation(rotateAnim);
		}
		else {
			imgLoader.clearAnimation();
			imgLoader.setVisibility(View.INVISIBLE);
		}
	}

	private void setupState() {
		toggleActionbarLoader(true);

		// load artists, categories, neighborhoods from database
		queryHandler.startQuery(QUERY_ARTISTS, null, Artists.CONTENT_URI, ArtAroundDatabase.ARTISTS_PROJECTION, null,
				null, null);
		queryHandler.startQuery(QUERY_CATEGORIES, null, Categories.CONTENT_URI,
				ArtAroundDatabase.CATEGORIES_PROJECTION, null, null, null);
		queryHandler.startQuery(QUERY_NEIGHBORHOODS, null, Neighborhoods.CONTENT_URI,
				ArtAroundDatabase.NEIGHBORHOODS_PROJECTION, null, null, null);

		// load art photos from server/cache, if necessary
		int size = 0;
		if (art != null && art.photoIds != null && (size = art.photoIds.size()) > 0 && allPhotoUris.isEmpty()
				&& !hasRunningTasks(LOAD_PHOTOS)) {
			for (int i = 0; i < size; i++) {
				startTask(new LoadFlickrPhotosCommand(LOAD_PHOTOS, art.photoIds.get(i)));
			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (cursor == null) return;

		startManagingCursor(cursor);

		if (!cursor.moveToFirst()) {
			loadFromServer(token);
		}

		toggleActionbarLoader(false);

		SimpleCursorAdapter adapter = null;
		switch (token) {
		case QUERY_ARTISTS:
			adapter = new SimpleCursorAdapter(this, R.layout.autocomplete_item, cursor,
					new String[] { ArtAroundDatabase.Artists.NAME }, new int[] { R.id.text1 });
			adapter.setCursorToStringConverter(new CursorToStringConverter() {
				@Override
				public CharSequence convertToString(Cursor cursor) {
					final int columnIndex = cursor.getColumnIndexOrThrow(ArtAroundDatabase.Artists.NAME);
					final String str = cursor.getString(columnIndex);
					return str;
				}
			});

			tvArtist.setAdapter(adapter);
			break;
		case QUERY_CATEGORIES:
			adapter = new SimpleCursorAdapter(this, R.layout.autocomplete_item, cursor,
					new String[] { ArtAroundDatabase.Categories.NAME }, new int[] { R.id.text1 });
			adapter.setCursorToStringConverter(new CursorToStringConverter() {
				@Override
				public CharSequence convertToString(Cursor cursor) {
					final int columnIndex = cursor.getColumnIndexOrThrow(ArtAroundDatabase.Categories.NAME);
					final String str = cursor.getString(columnIndex);
					return str;
				}
			});

			tvCategory.setAdapter(adapter);
			break;
		case QUERY_NEIGHBORHOODS:
			adapter = new SimpleCursorAdapter(this, R.layout.autocomplete_item, cursor,
					new String[] { ArtAroundDatabase.Neighborhoods.NAME }, new int[] { R.id.text1 });
			adapter.setCursorToStringConverter(new CursorToStringConverter() {
				@Override
				public CharSequence convertToString(Cursor cursor) {
					final int columnIndex = cursor.getColumnIndexOrThrow(ArtAroundDatabase.Neighborhoods.NAME);
					final String str = cursor.getString(columnIndex);
					return str;
				}
			});

			tvNeighborhood.setAdapter(adapter);
			break;
		}
	}

	private void loadFromServer(int token) {
		switch (token) {
		case QUERY_CATEGORIES:
			startTask(new LoadCategoriesCommand(token, String.valueOf(token)));
			break;

		case QUERY_NEIGHBORHOODS:
			startTask(new LoadNeighborhoodsCommand(token, String.valueOf(token)));
			break;
		}
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {
		super.onPreExecute(command);
	}

	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		super.onPostExecute(command, result, exception);

		switch (command.token) {
		case QUERY_CATEGORIES:
			toggleActionbarLoader(false);
			if (exception != null) {
				Utils.showToast(this, R.string.load_categories_error);
			}
			break;
		case QUERY_NEIGHBORHOODS:
			toggleActionbarLoader(false);
			if (exception != null) {
				Utils.showToast(this, R.string.load_neighborhoods_error);
			}
			break;
		case LOAD_PHOTOS:
			if (exception != null) {
				Uri[] uris = (Uri[]) result;
				if (uris != null && uris.length == 2) {
					allPhotoUris.add(uris[0].toString());
					((GalleryAdapter) gallery.getAdapter()).notifyDataSetChanged();
				}
			}
			else {
				Utils.showToast(this, R.string.load_photos_error);
			}

			break;
		}
	}

	private void startLocationUpdate() {
		toggleActionbarLoader(true);
		locationUpdater.updateLocation();
		tvLabelMinimap.setText(R.string.art_edit_label_minimap_loading);
	}

	@Override
	public void onSuggestLocationSettings() {
		toggleActionbarLoader(false);
		showDialog(DIALOG_LOCATION_SETTINGS);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.location = location;
		toggleActionbarLoader(false);

		// position the minimap on the user
		MapController ctrl = minimap.getController();
		GeoPoint geo = Utils.geo(location);
		ctrl.animateTo(geo);
		ctrl.setZoom(Utils.MINIMAP_ZOOM);

		currentOverlay = new CurrentOverlay(this, R.drawable.ic_pin, geo, R.id.minimap_drag);
		minimap.getOverlays().add(currentOverlay);
		minimap.invalidate();

		tvLabelMinimap.setText(R.string.art_edit_label_minimap);
		TextView tvCoords = (TextView) findViewById(R.id.minimap_coords);
		tvCoords.setText(Utils.formatCoords(location));
	}

	@Override
	public void onLocationUpdateError() {
		toggleActionbarLoader(false);
		tvLabelMinimap.setText(R.string.location_update_failure);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOCATION_SETTINGS:
			return Utils.locationSettingsDialog(this).create();
		default:
			return super.onCreateDialog(id);
		}
	}
}
