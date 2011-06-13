package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.LoaderPayload;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Artist;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

public class ArtEdit extends FragmentActivity {
	private static final String TAG = "ArtAround.ArtEdit";
	private static final String TAG_MINIMAP = "minimap";
	private static final String TAG_MINI_GALLERY = "mini_gallery";

	public static final String EXTRA_ART = "art";

	private static final String ARG_NEW_ART = "new_art";

	private static final int LOAD_CATEGORIES = 0;
	private static final int LOAD_NEIGHBORHOODS = 1;
	private static final int LOAD_ARTISTS = 2;
	private static final int SUBMIT_ART = 3;
	private static final int SUBMIT_PHOTO = 4;

	private static final int DIALOG_EMPTY_INPUT = 0;

	public static final String SAVE_SCROLL_Y = "scroll_y";

	private Art art;

	private EditText tvName;
	private AutoCompleteTextView tvArtist;
	private AutoCompleteTextView tvCategory;
	private AutoCompleteTextView tvArea;
	private EditText tvYear;
	private EditText tvWard;
	private EditText tvDescription;
	private EditText tvLocationDescription;

	private ImageButton dropdownArtist;
	private ImageButton dropdownCategory;
	private ImageButton dropdownArea;
	private ImageView imgLoader;
	private Animation rotateAnim;
	private Button btnSubmit;
	private ScrollView scrollView;
	private int scrollY;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setTheme(this);
		Utils.enableDump(this);

		setContentView(R.layout.art_edit);

		setupVars(savedInstanceState);
		setupUi();
		setupState();
	}

	private void restoreState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			scrollY = savedInstanceState.getInt(SAVE_SCROLL_Y, 0);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAVE_SCROLL_Y, scrollView.getScrollY());
		super.onSaveInstanceState(outState);
	}

	private void setupVars(Bundle savedInstanceState) {
		Intent i = getIntent();
		if (i.hasExtra(EXTRA_ART)) {
			art = (Art) i.getSerializableExtra(EXTRA_ART);
		}
		else {
			art = new Art(); // will be used as a "holder" for user input
		}
		restoreState(savedInstanceState);
	}

	private void setupState() {
		LoaderManager lm = getSupportLoaderManager();
		// start loading categories
		if (lm.getLoader(LOAD_CATEGORIES) == null) {
			lm.initLoader(LOAD_CATEGORIES, null, cursorLoaderCallback);
		}
		else {
			lm.restartLoader(LOAD_CATEGORIES, null, cursorLoaderCallback);
		}

		// start loading artists
		if (lm.getLoader(LOAD_ARTISTS) == null) {
			lm.initLoader(LOAD_ARTISTS, null, cursorLoaderCallback);
		}
		else {
			lm.restartLoader(LOAD_ARTISTS, null, cursorLoaderCallback);
		}

		// start loading neighborhoods
		if (lm.getLoader(LOAD_NEIGHBORHOODS) == null) {
			lm.initLoader(LOAD_NEIGHBORHOODS, null, cursorLoaderCallback);
		}
		else {
			lm.restartLoader(LOAD_NEIGHBORHOODS, null, cursorLoaderCallback);
		}
	}

	private void setupUi() {
		imgLoader = (ImageView) findViewById(R.id.bottombar_loader);
		rotateAnim = Utils.getRoateAnim(this);

		btnSubmit = (Button) findViewById(R.id.btn_submit);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (validateTexts()) {
					onSubmitArt();
				}
			}
		});

		scrollView = (ScrollView) findViewById(R.id.scroll_view);
		if (scrollY > 0) {
			Utils.d(TAG, "saved scrollY=" + scrollY);
			scrollView.post(new Runnable() {
				@Override
				public void run() {
					scrollView.scrollTo(0, scrollY);
					scrollY = 0;
				}
			});
		}

		setupArtFields();
		setupMiniMap();
		setupMiniGallery();
	}

	protected void onSubmitArt() {
		if (art == null) {
			art = new Art();
		}

		art.title = tvName.getText().toString();
		art.artist = new Artist(tvArtist.getText().toString());

		Location location = ((MiniMapFragment) getSupportFragmentManager().findFragmentByTag(TAG_MINIMAP))
				.getLocation();
		if (location != null) {
			art.latitude = location.getLatitude();
			art.longitude = location.getLongitude();
		}
		art.category = tvCategory.getText().toString();
		art.neighborhood = tvArea.getText().toString();
		art.description = tvDescription.getText().toString();
		art.locationDesc = tvDescription.getText().toString();
		String ward = tvWard.getText().toString();
		if (!TextUtils.isEmpty(ward)) {
			art.ward = Integer.parseInt(ward);
		}
		String year = tvYear.getText().toString();
		if (!TextUtils.isEmpty(year)) {
			art.year = Integer.parseInt(year);
		}
		Utils.d(TAG, "Sumbitting new art art " + art);
	}

	private boolean validateTexts() {
		boolean ok = true;
		if (TextUtils.isEmpty(tvCategory.getText()) || TextUtils.isEmpty(tvArea.getText())) {
			ok = false;
			showDialog(DIALOG_EMPTY_INPUT);
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
		dropdownArtist = (ImageButton) findViewById(R.id.dropdown_artist);
		dropdownArtist.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (tvArtist.isPopupShowing())
					tvArtist.dismissDropDown();
				else
					tvArtist.showDropDown();
			}
		});

		tvYear = (EditText) findViewById(R.id.art_edit_input_year);
		if (art.year > 0) {
			tvYear.setText(String.valueOf(art.year));
		}

		tvCategory = (AutoCompleteTextView) findViewById(R.id.art_edit_input_category);
		if (!TextUtils.isEmpty(art.category)) {
			tvCategory.setText(art.category);
		}
		dropdownCategory = (ImageButton) findViewById(R.id.dropdown_category);
		dropdownCategory.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (tvCategory.isPopupShowing())
					tvCategory.dismissDropDown();
				else
					tvCategory.showDropDown();
			}
		});

		tvDescription = (EditText) findViewById(R.id.art_edit_input_description);
		if (!TextUtils.isEmpty(art.description)) {
			tvDescription.setText(Html.fromHtml(art.description)); // for special UTF-8 characters
		}
		Utils.setHintSpan(tvDescription, tvDescription.getHint());

		tvWard = (EditText) findViewById(R.id.art_edit_input_ward);
		if (art.ward > 0) {
			tvWard.setText(String.valueOf(art.ward));
		}

		tvArea = (AutoCompleteTextView) findViewById(R.id.art_edit_input_area);
		if (!TextUtils.isEmpty(art.neighborhood)) {
			tvArea.setText(art.neighborhood);
		}
		dropdownArea = (ImageButton) findViewById(R.id.dropdown_area);
		dropdownArea.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (tvArea.isPopupShowing())
					tvArea.dismissDropDown();
				else
					tvArea.showDropDown();
			}
		});

		tvLocationDescription = (EditText) findViewById(R.id.art_edit_input_location_description);
		if (!TextUtils.isEmpty(art.locationDesc)) {
			tvLocationDescription.setText(Html.fromHtml(art.locationDesc));
		}
		Utils.setHintSpan(tvLocationDescription, tvLocationDescription.getHint());
	}

	private void setupMiniGallery() {
		Bundle args = new Bundle();
		args.putStringArrayList(MiniGalleryFragment.ARG_PHOTOS, art.photoIds);
		args.putString(MiniGalleryFragment.ARG_TITLE, art.title);
		args.putBoolean(MiniGalleryFragment.ARG_EDIT_MODE, true);

		MiniGalleryFragment f = new MiniGalleryFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_gallery_placeholder, f, TAG_MINI_GALLERY).commit();
		Utils.d(TAG, "setupMiniGallery(): args=" + args);
	}

	private void setupMiniMap() {
		Bundle args = new Bundle();
		args.putBoolean(MiniMapFragment.ARG_EDIT_MODE, true);

		MiniMapFragment f = new MiniMapFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_map_placeholder, f, TAG_MINIMAP).commit();
	}

	private void toggleLoading(boolean show) {
		if (show) {
			imgLoader.setVisibility(View.VISIBLE);
			imgLoader.startAnimation(rotateAnim);
		}
		else {
			imgLoader.clearAnimation();
			imgLoader.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private final LoaderCallbacks<Cursor> cursorLoaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			onCursorLoaderReset(loader);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			onCursorLoadFinished(loader, cursor);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return onCreateCursorLoader(id, args);
		}
	};

	private final LoaderCallbacks<LoaderPayload> asyncLoader = new LoaderCallbacks<LoaderPayload>() {

		@Override
		public void onLoaderReset(Loader<LoaderPayload> loader) {
			onAsyncLoaderReset(loader);
		}

		@Override
		public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
			onAsyncLoadFinished(loader, payload);
		}

		@Override
		public Loader<LoaderPayload> onCreateLoader(int id, Bundle args) {
			return onCreateAsyncLoader(id, args);
		}
	};

	private void onCursorLoaderReset(Loader<Cursor> loader) {
		Utils.d(TAG, "onCursorLoaderReset(): id=" + loader.getId());
	}

	private void onCursorLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Utils.d(TAG, "onCursorLoadFinished(): id=" + loader.getId());

		if (cursor == null || !cursor.moveToFirst()) {
			onStartServerLoading(loader.getId());
			return;
		}

		SimpleCursorAdapter adapter = null;
		switch (loader.getId()) {
		case LOAD_CATEGORIES:
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
		case LOAD_ARTISTS:
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
		case LOAD_NEIGHBORHOODS:
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
			tvArea.setAdapter(adapter);
			break;
		}
	}

	private void onStartServerLoading(int id) {
		LoaderManager lm = getSupportLoaderManager();
		if (lm.getLoader(LOAD_CATEGORIES) == null) {
			lm.initLoader(LOAD_CATEGORIES, null, asyncLoader);
		}
		else {
			lm.restartLoader(LOAD_CATEGORIES, null, asyncLoader);
		}

		if (lm.getLoader(LOAD_NEIGHBORHOODS) == null) {
			lm.initLoader(LOAD_NEIGHBORHOODS, null, asyncLoader);
		}
		else {
			lm.restartLoader(LOAD_NEIGHBORHOODS, null, asyncLoader);
		}
	}

	private Loader<Cursor> onCreateCursorLoader(int id, Bundle args) {
		Utils.d(TAG, "onCreateCursorLoader(): id=" + id);
		switch (id) {
		case LOAD_CATEGORIES:
			return new CursorLoader(this, Categories.CONTENT_URI, ArtAroundDatabase.CATEGORIES_PROJECTION, null, null,
					null);
		case LOAD_ARTISTS:
			return new CursorLoader(this, Artists.CONTENT_URI, ArtAroundDatabase.ARTISTS_PROJECTION, null, null, null);
		case LOAD_NEIGHBORHOODS:
			return new CursorLoader(this, Neighborhoods.CONTENT_URI, ArtAroundDatabase.NEIGHBORHOODS_PROJECTION, null,
					null, null);
		default:
			return null;
		}
	}

	private void onAsyncLoaderReset(Loader<LoaderPayload> loader) {
		// TODO Auto-generated method stub

	}

	private void onAsyncLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
		switch (loader.getId()) {
		case LOAD_CATEGORIES:
			if (payload.getStatus() == LoaderPayload.RESULT_ERROR) {
				Utils.showToast(this, R.string.load_categories_error);
			}
			break;
		case LOAD_NEIGHBORHOODS:
			if (payload.getStatus() == LoaderPayload.RESULT_ERROR) {
				Utils.showToast(this, R.string.load_neighborhoods_error);
			}
			break;
		case SUBMIT_ART:
			if (payload.getStatus() == LoaderPayload.RESULT_OK) {
				String newSlug = (String) payload.getResult();
				art.slug = newSlug;

				if (!TextUtils.isEmpty(newSlug)) {
					// upload pictures
				}
			}
			else {
				Utils.showToast(this, R.string.submit_art_failure);
			}
		}
	}

	private Loader<LoaderPayload> onCreateAsyncLoader(int id, final Bundle args) {
		switch (id) {
		case LOAD_CATEGORIES:
			return new AsyncLoader<LoaderPayload>(this) {
				@Override
				public LoaderPayload loadInBackground() {

					try {
						ServiceFactory.getArtService().getCategories();
						return new LoaderPayload(LoaderPayload.RESULT_OK);
					}
					catch (ArtAroundException e) {
						return new LoaderPayload(e);
					}
				}
			};
		case LOAD_NEIGHBORHOODS:
			return new AsyncLoader<LoaderPayload>(this) {
				@Override
				public LoaderPayload loadInBackground() {
					try {
						ServiceFactory.getArtService().getNeighborhoods();
						return new LoaderPayload(LoaderPayload.RESULT_OK);
					}
					catch (ArtAroundException e) {
						return new LoaderPayload(e);
					}
				}
			};
		case SUBMIT_ART:
			return new AsyncLoader<LoaderPayload>(this) {

				@Override
				public LoaderPayload loadInBackground() {
					try {
						return new LoaderPayload(ServiceFactory.getArtService().submitArt(
								(Art) args.getSerializable(ARG_NEW_ART)));
					}
					catch (ArtAroundException e) {
						return new LoaderPayload(e);
					}
				}
			};
		default:
			return null;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EMPTY_INPUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_edit_hint_input_empty_title);
			builder.setMessage(R.string.art_edit_hint_input_empty_msg);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.setCancelable(true);
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			onFinish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		onFinish();
		return;
	}

	private void onFinish() {
		Intent iHome = Utils.getHomeIntent(this);
		iHome.putExtras(getIntent()); // saved things from ArtMap
		startActivity(iHome);
		finish();
	}
}
