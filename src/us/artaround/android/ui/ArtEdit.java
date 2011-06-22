package us.artaround.android.ui;

import java.util.ArrayList;

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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.TextView;
import android.widget.Toast;

public class ArtEdit extends FragmentActivity {
	//private static final String TAG = "ArtEdit";
	private static final String TAG_MINI_MAP = "mini_map";
	private static final String TAG_MINI_GALLERY = "mini_gallery";

	public static final String EXTRA_ART = "art";

	private static final String ARG_NEW_ART = "new_art";
	private static final String ARG_FILE_NAME = "filename";
	private static final String ARG_POSITION = "position";

	private static final int LOAD_CATEGORIES = 0;
	private static final int LOAD_NEIGHBORHOODS = 1;
	private static final int LOAD_ARTISTS = 2;
	private static final int SUBMIT_ART = 3;
	private static final int UPLOAD_PHOTO = 4;
	private static final int EDIT_ART = 5;

	private static final int DIALOG_EMPTY_INPUT = 0;
	private static final int DIALOG_EMPTY_LOCATION = 1;
	private static final int DIALOG_EMPTY_PHOTOS = 2;
	private static final int DIALOG_DISCARD_EDIT = 3;
	private static final int DIALOG_PROGRESS = 4;

	private static final String SAVE_SCROLL_Y = "save_scroll_y";
	private static final String SAVE_ART = "save_art";
	private static final String SAVE_HAS_CHANGES = "save_has_changes";

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
	private boolean hasChanges;
	private ArrayList<String> uris;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setTheme(this);
		Utils.enableDump(this);

		ServiceFactory.init(getApplicationContext());

		setContentView(R.layout.art_edit);

		setupVars(savedInstanceState);
		setupUi(savedInstanceState);
		setupState();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAVE_SCROLL_Y, scrollView.getScrollY());
		outState.putSerializable(SAVE_ART, art);
		outState.putBoolean(SAVE_HAS_CHANGES, hasChanges);
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

	private void restoreState(Bundle savedInstanceState) {
		Utils.d(Utils.TAG, "restoreState(): savedInstance=", savedInstanceState);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(SAVE_SCROLL_Y)) {
				scrollY = savedInstanceState.getInt(SAVE_SCROLL_Y, 0);
			}
			if (savedInstanceState.containsKey(SAVE_ART)) {
				art = (Art) savedInstanceState.getSerializable(SAVE_ART);
			}
			if (savedInstanceState.containsKey(SAVE_HAS_CHANGES)) {
				hasChanges = savedInstanceState.getBoolean(SAVE_HAS_CHANGES, false);
			}
		}
	}

	private void setupState() {
		// start loading categories
		getSupportLoaderManager().restartLoader(LOAD_CATEGORIES, null, cursorLoaderCallback);

		// start loading artists
		getSupportLoaderManager().restartLoader(LOAD_ARTISTS, null, cursorLoaderCallback);
		getSupportLoaderManager().restartLoader(LOAD_NEIGHBORHOODS, null, cursorLoaderCallback);
	}

	private void setupUi(Bundle savedInstanceState) {
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
			Utils.d(Utils.TAG, "saved scrollY=", scrollY);
			scrollView.post(new Runnable() {
				@Override
				public void run() {
					scrollView.scrollTo(0, scrollY);
					scrollY = 0;
				}
			});
		}

		setupArtFields();
		setupMiniMap(savedInstanceState);
		setupMiniGallery(savedInstanceState);
	}

	protected void onSubmitArt() {
		Location location = ((MiniMapFragment) getSupportFragmentManager().findFragmentByTag(TAG_MINI_MAP))
				.getLocation();
		if (location != null) {
			art.latitude = location.getLatitude();
			art.longitude = location.getLongitude();
		}
		if (art.longitude == 0 || art.latitude == 0) {
			showDialog(DIALOG_EMPTY_LOCATION);
			return;
		}

		Bundle args = new Bundle();
		args.putSerializable(ARG_NEW_ART, art);
		if (!TextUtils.isEmpty(art.slug)) {
			showDialog(DIALOG_PROGRESS);
			getSupportLoaderManager().restartLoader(EDIT_ART, args, asyncLoader);
		}
		else {
			// check to see if there are photos attached
			MiniGalleryFragment f = (MiniGalleryFragment) getSupportFragmentManager().findFragmentByTag(
					TAG_MINI_GALLERY);
			if (f == null) {
				Utils.d(Utils.TAG, "onUploadPictures(): MiniGalleryFragment is null!");
				return; //something bad happened
			}

			uris = f.getNewPhotoUris();
			if (uris == null || uris.isEmpty()) {
				showDialog(DIALOG_EMPTY_PHOTOS);
				Utils.d(Utils.TAG, "There are no photos to be uploaded!");
				return;
			}

			showDialog(DIALOG_PROGRESS);
			getSupportLoaderManager().restartLoader(SUBMIT_ART, args, asyncLoader);
		}
	}

	protected void onUploadPictures(int position) {
		if (uris == null || uris.isEmpty()) {
			Utils.d(Utils.TAG, "There are no photos to be uploaded!");
			return;
		}

		if (position == 0) {
			showDialog(DIALOG_PROGRESS);
		}

		if (position == uris.size()) {
			hasChanges = false;
			dismissDialog(DIALOG_PROGRESS);
			Toast.makeText(getApplicationContext(), R.string.submit_art_success, Toast.LENGTH_LONG).show();

			onFinish();
			return;
		}

		String filename = uris.get(position);
		Bundle args = new Bundle();
		args.putInt(ARG_POSITION, position);
		args.putString(ARG_FILE_NAME, filename);

		Utils.d(Utils.TAG, "Uploading file", filename);
		getSupportLoaderManager().restartLoader(UPLOAD_PHOTO, args, asyncLoader);
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
		TextWatcher watcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				View focus = ArtEdit.this.getCurrentFocus();
				if (!(focus instanceof EditText) && !(focus instanceof AutoCompleteTextView)) return;
				TextView tvFocused = (TextView) focus;
				String str = s.toString();
				hasChanges = true;

				switch (tvFocused.getId()) {
				case R.id.art_edit_input_name:
					art.title = str;
					break;
				case R.id.art_edit_input_artist:
					art.artist = str;
					break;
				case R.id.art_edit_input_category:
					art.category = str;
					break;
				case R.id.art_edit_input_description:
					art.description = str;
					break;
				case R.id.art_edit_input_area:
					art.neighborhood = str;
					break;
				case R.id.art_edit_input_location_description:
					art.locationDesc = str;
					break;
				case R.id.art_edit_input_ward:
					int ward = 0;
					try {
						ward = Integer.parseInt(str);
					}
					catch (NumberFormatException e) {}
					art.ward = ward;
					break;
				case R.id.art_edit_input_year:
					int year = 0;
					try {
						year = Integer.parseInt(str);
					}
					catch (NumberFormatException e) {}
					art.year = year;
					break;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		};

		tvName = (EditText) findViewById(R.id.art_edit_input_name);
		tvName.addTextChangedListener(watcher);
		if (!TextUtils.isEmpty(art.title)) {
			tvName.setText(art.title);
		}

		tvArtist = (AutoCompleteTextView) findViewById(R.id.art_edit_input_artist);
		tvArtist.addTextChangedListener(watcher);
		if (art.artist != null && !TextUtils.isEmpty(art.artist)) {
			tvArtist.setText(art.artist);
		}
		dropdownArtist = (ImageButton) findViewById(R.id.dropdown_artist);
		dropdownArtist.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				tvArtist.requestFocus();
				if (tvArtist.isPopupShowing())
					tvArtist.dismissDropDown();
				else
					tvArtist.showDropDown();
			}
		});

		tvYear = (EditText) findViewById(R.id.art_edit_input_year);
		tvYear.addTextChangedListener(watcher);
		if (art.year > 0) {
			tvYear.setText(String.valueOf(art.year));
		}

		tvCategory = (AutoCompleteTextView) findViewById(R.id.art_edit_input_category);
		tvCategory.addTextChangedListener(watcher);
		if (!TextUtils.isEmpty(art.category)) {
			tvCategory.setText(art.category);
		}
		dropdownCategory = (ImageButton) findViewById(R.id.dropdown_category);
		dropdownCategory.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tvCategory.requestFocus();
				if (tvCategory.isPopupShowing())
					tvCategory.dismissDropDown();
				else
					tvCategory.showDropDown();
			}
		});

		tvDescription = (EditText) findViewById(R.id.art_edit_input_description);
		tvDescription.addTextChangedListener(watcher);
		if (!TextUtils.isEmpty(art.description)) {
			tvDescription.setText(Html.fromHtml(art.description)); // for special UTF-8 characters
		}
		Utils.setHintSpan(tvDescription, tvDescription.getHint());

		tvWard = (EditText) findViewById(R.id.art_edit_input_ward);
		tvWard.addTextChangedListener(watcher);
		if (art.ward > 0) {
			tvWard.setText(String.valueOf(art.ward));
		}

		tvArea = (AutoCompleteTextView) findViewById(R.id.art_edit_input_area);
		tvArea.addTextChangedListener(watcher);
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
		tvLocationDescription.addTextChangedListener(watcher);
		if (!TextUtils.isEmpty(art.locationDesc)) {
			tvLocationDescription.setText(Html.fromHtml(art.locationDesc));
		}
		Utils.setHintSpan(tvLocationDescription, tvLocationDescription.getHint());
	}

	private void setupMiniGallery(Bundle savedInstanceState) {
		if (savedInstanceState != null) return;

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.mini_gallery_placeholder, new MiniGalleryFragment(art.title, art.photoIds, true),
						TAG_MINI_GALLERY).commit();
	}

	private void setupMiniMap(Bundle savedInstanceState) {
		if (savedInstanceState != null) return;

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.mini_map_placeholder, new MiniMapFragment(art.latitude, art.longitude, art.slug == null),
						TAG_MINI_MAP).commit();
	}

	public void toggleLoading(boolean show) {
		if (show) {
			imgLoader.setVisibility(View.VISIBLE);
			imgLoader.startAnimation(rotateAnim);
		}
		else {
			imgLoader.clearAnimation();
			imgLoader.setVisibility(View.INVISIBLE);
		}
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

	private void onStartServerLoading(int id) {
		LoaderManager lm = getSupportLoaderManager();
		lm.restartLoader(LOAD_CATEGORIES, null, asyncLoader);
		lm.restartLoader(LOAD_NEIGHBORHOODS, null, asyncLoader);
	}

	private Loader<Cursor> onCreateCursorLoader(int id, Bundle args) {
		Utils.d(Utils.TAG, "onCreateCursorLoader(): id=", id);
		switch (id) {
		case LOAD_CATEGORIES:
			return new CursorLoader(this, Categories.CONTENT_URI, ArtAroundDatabase.CATEGORIES_PROJECTION, null, null,
					null);
		case LOAD_ARTISTS:
			StringBuilder b = new StringBuilder(Artists.NAME).append(" NOT NULL AND ").append(Artists.NAME)
					.append("<> ''");
			return new CursorLoader(this, Artists.CONTENT_URI, new String[] { Artists._ID, Artists.NAME },
					b.toString(), null,
					null);
		case LOAD_NEIGHBORHOODS:
			return new CursorLoader(this, Neighborhoods.CONTENT_URI, ArtAroundDatabase.NEIGHBORHOODS_PROJECTION, null,
					null, null);
		default:
			return null;
		}
	}

	private void onCursorLoaderReset(Loader<Cursor> loader) {
		Utils.d(Utils.TAG, "onCursorLoaderReset(): id=", loader.getId());
	}

	private void onCursorLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Utils.d(Utils.TAG, "onCursorLoadFinished(): id=", loader.getId());
		loader.stopLoading();

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
			adapter = new SimpleCursorAdapter(this, R.layout.autocomplete_item, cursor, new String[] { Artists.NAME },
					new int[] { R.id.text1 });
			adapter.setCursorToStringConverter(new CursorToStringConverter() {
				@Override
				public CharSequence convertToString(Cursor cursor) {
					final int columnIndex = cursor.getColumnIndexOrThrow(Artists.NAME);
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

	private Loader<LoaderPayload> onCreateAsyncLoader(int id, final Bundle args) {
		switch (id) {
		case LOAD_CATEGORIES:
			return new AsyncLoader<LoaderPayload>(this) {
				@Override
				public LoaderPayload loadInBackground() {

					try {
						ServiceFactory.getArtService().getCategories();
						return new LoaderPayload(LoaderPayload.STATUS_OK);
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
						return new LoaderPayload(LoaderPayload.STATUS_OK);
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
						return new LoaderPayload(LoaderPayload.STATUS_OK, ServiceFactory.getArtService().submitArt(
								(Art) args.getSerializable(ARG_NEW_ART)));
					}
					catch (ArtAroundException e) {
						return new LoaderPayload(e);
					}
				}
			};
		case EDIT_ART:
			return new AsyncLoader<LoaderPayload>(this) {
				@Override
				public LoaderPayload loadInBackground() {
					try {
						return new LoaderPayload(LoaderPayload.STATUS_OK, ServiceFactory.getArtService().editArt(
								(Art) args.getSerializable(ARG_NEW_ART)));
					}
					catch (ArtAroundException e) {
						return new LoaderPayload(e);
					}
				}
			};
		case UPLOAD_PHOTO:
			return new AsyncLoader<LoaderPayload>(this) {
				@Override
				public LoaderPayload loadInBackground() {
					try {
						String response = ServiceFactory.getArtService().uploadPhoto(art.slug,
								args.getString(ARG_FILE_NAME));
						Utils.d(Utils.TAG, "uploading photo: response =", response);
						return new LoaderPayload(LoaderPayload.STATUS_OK, args.getInt(ARG_POSITION));
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

	private void onAsyncLoaderReset(Loader<LoaderPayload> loader) {}

	private void onAsyncLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
		switch (loader.getId()) {
		case LOAD_CATEGORIES:
			if (payload.getStatus() == LoaderPayload.STATUS_ERROR) {
				Toast.makeText(this, R.string.load_categories_error, Toast.LENGTH_LONG).show();
			}
			break;
		case LOAD_NEIGHBORHOODS:
			if (payload.getStatus() == LoaderPayload.STATUS_ERROR) {
				Toast.makeText(this, R.string.load_neighborhoods_error, Toast.LENGTH_LONG).show();
			}
			break;
		case SUBMIT_ART:
			if (payload.getStatus() == LoaderPayload.STATUS_OK) {
				String newSlug = (String) payload.getResult();
				art.slug = newSlug;

				if (!TextUtils.isEmpty(newSlug)) {
					onUploadPictures(0);
				}
				else {
					hasChanges = false;
					dismissDialog(DIALOG_PROGRESS);
					Toast.makeText(getApplicationContext(), R.string.submit_art_success, Toast.LENGTH_LONG).show();
				}
			}
			else {
				dismissDialog(DIALOG_PROGRESS);
				Toast.makeText(getApplicationContext(), R.string.submit_art_failure, Toast.LENGTH_LONG).show();
			}
			break;
		case EDIT_ART:
			if (payload.getStatus() == LoaderPayload.STATUS_OK) {
				onUploadPictures(0);
			}
			else {
				dismissDialog(DIALOG_PROGRESS);
				Toast.makeText(getApplicationContext(), R.string.submit_art_failure, Toast.LENGTH_LONG).show();
			}
			break;
		case UPLOAD_PHOTO:
			if (payload.getStatus() == LoaderPayload.STATUS_OK) {
				Object obj = payload.getResult();
				if (obj != null && obj instanceof Integer) {
					int pos = (Integer) obj;
					onUploadPictures(pos + 1);
				}
			}
			else {
				dismissDialog(DIALOG_PROGRESS);
				Toast.makeText(getApplicationContext(), R.string.upload_picture_failure, Toast.LENGTH_LONG).show();
			}
			break;
		}
		loader.stopLoading();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EMPTY_INPUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_edit_input_empty_title);
			builder.setMessage(R.string.art_edit_input_empty_msg);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.setCancelable(true);
			return builder.create();
		case DIALOG_EMPTY_LOCATION:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_edit_input_empty_location_title);
			builder.setMessage(R.string.art_edit_input_empty_location_msg);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.setCancelable(true);
			return builder.create();
		case DIALOG_EMPTY_PHOTOS:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_edit_input_empty_photos_title);
			builder.setMessage(R.string.art_edit_input_empty_photos_msg);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.setCancelable(true);
			return builder.create();
		case DIALOG_DISCARD_EDIT:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_edit_discard_changes_title);
			builder.setMessage(R.string.art_edit_discard_changes_msg);
			builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					setResult(RESULT_OK);
					finish();
				}
			});
			builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			return builder.create();
		case DIALOG_PROGRESS:
			return ProgressDialog.show(this, "", getString(R.string.loading), false);
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
		// delete all temporary photos			
		Utils.deleteCachedFiles();

		if (getIntent().hasExtra(EXTRA_ART)) {
			if (hasChanges) {
				showDialog(DIALOG_DISCARD_EDIT);
			}
			else {
				setResult(RESULT_OK);
				finish();
			}
		}
		else {
			Intent iHome = Utils.getHomeIntent(this);
			iHome.putExtras(getIntent()); // saved things from ArtMap
			startActivity(iHome);
			finish();
		}
	}
}
