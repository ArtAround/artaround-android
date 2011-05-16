package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

	public static final String EXTRA_ART = "art";

	private static final int LOAD_CATEGORIES = 0;
	private static final int LOAD_NEIGHBORHOODS = 1;
	private static final int LOAD_ARTISTS = 2;

	private static final int DIALOG_EMPTY_INPUT = 0;

	public static final String SAVE_SCROLL_Y = "scroll_y";

	private Art art;

	private EditText tvName;
	private AutoCompleteTextView tvArtist;
	private AutoCompleteTextView tvCategory;
	private AutoCompleteTextView tvArea;
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
					//TODO submit to server
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

		EditText tvYear = (EditText) findViewById(R.id.art_edit_input_year);
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

		EditText tvDesc = (EditText) findViewById(R.id.art_edit_input_description);
		if (!TextUtils.isEmpty(art.description)) {
			tvDesc.setText(Html.fromHtml(art.description)); // for special UTF-8 characters
		}
		Utils.setHintSpan(tvDesc, tvDesc.getHint());

		EditText tvWard = (EditText) findViewById(R.id.art_edit_input_ward);
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

		EditText tvLocDesc = (EditText) findViewById(R.id.art_edit_input_location_description);
		if (!TextUtils.isEmpty(art.locationDesc)) {
			tvLocDesc.setText(Html.fromHtml(art.locationDesc));
		}
		Utils.setHintSpan(tvLocDesc, tvLocDesc.getHint());
	}

	private void setupMiniGallery() {
		Bundle args = new Bundle();
		args.putStringArrayList(MiniGalleryFragment.ARG_PHOTOS, art.photoIds);
		args.putString(MiniGalleryFragment.ARG_TITLE, art.title);
		args.putBoolean(MiniGalleryFragment.ARG_EDIT_MODE, true);

		MiniGalleryFragment f = new MiniGalleryFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_gallery_placeholder, f).commit();
		Utils.d(TAG, "setupMiniGallery(): args=" + args);
	}

	private void setupMiniMap() {
		Bundle args = new Bundle();
		args.putBoolean(MiniMapFragment.ARG_EDIT_MODE, true);

		MiniMapFragment f = new MiniMapFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_map_placeholder, f).commit();
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

	private final LoaderCallbacks<Object> asyncLoader = new LoaderCallbacks<Object>() {

		@Override
		public void onLoaderReset(Loader<Object> loader) {
			onAsyncLoaderReset(loader);
		}

		@Override
		public void onLoadFinished(Loader<Object> loader, Object result) {
			onAsyncLoadFinished(loader, result);
		}

		@Override
		public Loader<Object> onCreateLoader(int id, Bundle args) {
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

	private void onAsyncLoaderReset(Loader<Object> loader) {
		// TODO Auto-generated method stub

	}

	private void onAsyncLoadFinished(Loader<Object> loader, Object result) {
		switch (loader.getId()) {
		case LOAD_CATEGORIES:
			if (result != null) {
				Utils.showToast(this, R.string.load_categories_error);
			}
			break;
		case LOAD_NEIGHBORHOODS:
			if (result != null) {
				Utils.showToast(this, R.string.load_neighborhoods_error);
			}
			break;
		}
	}

	private Loader<Object> onCreateAsyncLoader(int id, Bundle args) {
		switch (id) {
		case LOAD_CATEGORIES:
			return new AsyncLoader<Object>(this) {
				@Override
				public Object loadInBackground() {
					try {
						ServiceFactory.getArtService().getCategories();
						return null;
					}
					catch (ArtAroundException e) {
						return e;
					}
				}
			};
		case LOAD_NEIGHBORHOODS:
			return new AsyncLoader<Object>(this) {
				@Override
				public Object loadInBackground() {
					try {
						ServiceFactory.getArtService().getNeighborhoods();
						return null;
					}
					catch (ArtAroundException e) {
						return e;
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
