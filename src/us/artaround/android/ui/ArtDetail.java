package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase.ArtFavorites;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Comment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class ArtDetail extends FragmentActivity {
	public static final String TAG = "ArtAround.ArtDetail";

	public static final String EXTRA_ART = "art";

	private static final String SAVE_COMMENTS = "comments";
	private static final String SAVE_COMMENTS_COUNT = "comments_count";

	private static final String ARG_ART_SLUG = "art_slug";

	private static final int LOAD_FAVORITE = 0;
	private static final int LOAD_COMMENTS = 1;
	private static final int MAX_COMMENTS = 3;

	private static final String PAGE_BASE_URL = "http://theartaround.us/arts/";

	private Art art;
	private final ArrayList<Comment> comments = new ArrayList<Comment>();
	private int commentsCount = -1;

	private TextView tvComments;
	private TextView tvViewAll;
	private ImageButton btnFavorite;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);

		setContentView(R.layout.art_detail);

		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_ART)) {
			art = (Art) intent.getSerializableExtra(EXTRA_ART);
		}
		if (art == null) {
			Utils.w(TAG, "onCreate(): art=" + art);
			return;
		}

		setupUi();
		setupState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(SAVE_COMMENTS, comments);
		outState.putInt(SAVE_COMMENTS_COUNT, commentsCount);
		super.onSaveInstanceState(outState);
	}

	@SuppressWarnings("unchecked")
	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			comments.addAll((ArrayList<Comment>) savedInstanceState.getSerializable(SAVE_COMMENTS));
			commentsCount = savedInstanceState.getInt(SAVE_COMMENTS_COUNT);
		}
		if (comments.isEmpty()) {
			onLoadComments();
		}
	}

	private void onLoadComments() {
		Utils.d(TAG, "onLoadComments()");

		LoaderManager lm = getSupportLoaderManager();
		Bundle args = new Bundle();
		args.putString(ARG_ART_SLUG, art.slug);

		if (lm.getLoader(LOAD_COMMENTS) == null) {
			lm.initLoader(LOAD_COMMENTS, args, asyncCallback);
		}
		else {
			lm.restartLoader(LOAD_COMMENTS, args, asyncCallback);
		}
	}

	private void setupUi() {
		setupActionbar();
		setupBottombar();

		setupFields();
		setupMiniGallery();
		setupMiniMap();
	}

	private void setupBottombar() {
		// TODO Auto-generated method stub

	}

	private void setupActionbar() {
		if (!TextUtils.isEmpty(art.title)) {
			((TextView) findViewById(R.id.art_detail_title)).setText(art.title);
		}

		btnFavorite = (ImageButton) findViewById(R.id.btn_favorite);
		if (isFavorite()) {
			btnFavorite.setImageResource(R.drawable.ic_remove_favorite_background);
		}
		btnFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onFavoriteArt();
			}
		});

		ImageButton btnHome = (ImageButton) findViewById(R.id.btn_home);
		btnHome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onFinish();
			}
		});
	}

	protected void onFavoriteArt() {
		LoaderManager lm = getSupportLoaderManager();
		Bundle args = new Bundle();
		args.putString(ARG_ART_SLUG, art.slug);
		if (lm.getLoader(LOAD_FAVORITE) == null) {
			lm.initLoader(LOAD_FAVORITE, args, cursorCallback);
		}
		else {
			lm.restartLoader(LOAD_FAVORITE, args, cursorCallback);
		}
	}

	private boolean isFavorite() {
		boolean isFavorite = false;
		Cursor cursor = managedQuery(ArtFavorites.CONTENT_URI, null, ArtFavorites.TABLE_NAME + "." + ArtFavorites.SLUG
				+ "=?", new String[] { art.slug }, null);

		isFavorite = (cursor != null && cursor.moveToFirst());

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return isFavorite;
	}

	private void setupFields() {
		String str = null;

		TextView tvArtist = (TextView) findViewById(R.id.art_detail_artist);
		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) {
			tvArtist.setText(art.artist.name);
			str = " - ";
		}
		else {
			tvArtist.setVisibility(View.GONE);
		}

		TextView tvYear = (TextView) findViewById(R.id.art_detail_year);
		if (art.year > 0) {
			str = (str != null) ? (str + art.year) : "" + art.year;
			tvYear.setText(str);
		}
		else {
			tvYear.setVisibility(View.GONE);
		}

		TextView tvCategory = (TextView) findViewById(R.id.art_detail_category);
		if (!TextUtils.isEmpty(art.category)) {
			tvCategory.setText(art.category.toUpperCase());
		}
		else {
			tvCategory.setVisibility(View.GONE);
		}

		TextView tvDetail = (TextView) findViewById(R.id.art_detail_description);
		if (!TextUtils.isEmpty(art.description)) {
			tvDetail.setText(art.description);
		}
		else {
			tvDetail.setVisibility(View.GONE);
		}

		TextView tvLocation = (TextView) findViewById(R.id.art_detail_location_heading);
		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Georgia_Bold_Italic.ttf");
		tvLocation.setTypeface(tf);

		TextView tvArea = (TextView) findViewById(R.id.art_detail_area);
		if (!TextUtils.isEmpty(art.neighborhood)) {
			tvArea.setText(art.neighborhood);
		}
		else {
			tvArea.setVisibility(View.GONE);
		}

		TextView tvWard = (TextView) findViewById(R.id.art_detail_ward);
		if (art.ward > 0) {
			tvWard.setText("" + art.ward);
		}
		else {
			tvWard.setVisibility(View.GONE);
		}

		TextView tvLocDesc = (TextView) findViewById(R.id.art_detail_location);
		if (!TextUtils.isEmpty(art.locationDesc)) {
			tvLocDesc.setText(art.locationDesc);
		}
		else {
			tvLocDesc.setVisibility(View.GONE);
		}

		tvComments = (TextView) findViewById(R.id.art_detail_comments_heading);
		tvComments.setTypeface(tf);

		if (commentsCount > -1) {
			tvComments.setText(tvComments.getText() + " (" + commentsCount + ")");
		}

		tvViewAll = (TextView) findViewById(R.id.art_detail_view_all);

		SpannableString underlineSpan = new SpannableString(tvViewAll.getText());
		underlineSpan.setSpan(new UnderlineSpan(), 0, underlineSpan.length(), 0);
		tvViewAll.setText(underlineSpan);

		tvViewAll.setMovementMethod(LinkMovementMethod.getInstance());
		tvViewAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// open in browser
				String url = PAGE_BASE_URL + art.slug;
				Intent iBrowser = new Intent(Intent.ACTION_VIEW);
				iBrowser.setData(Uri.parse(url));
				startActivity(iBrowser);
			}
		});
		if (commentsCount > MAX_COMMENTS) {
			tvViewAll.setVisibility(View.VISIBLE);
		}

		TextView tvLeaveComment = (TextView) findViewById(R.id.art_detail_leave_comment_heading);
		tvLeaveComment.setTypeface(tf);

		EditText editMessage = (EditText) findViewById(R.id.art_detail_input_message);
		Utils.setHintSpan(editMessage, editMessage.getHint());
	}

	private void setupMiniGallery() {
		Bundle args = new Bundle();
		args.putStringArrayList(MiniGalleryFragment.ARG_PHOTOS, art.photoIds);
		args.putString(MiniGalleryFragment.ARG_TITLE, art.title);
		args.putBoolean(MiniGalleryFragment.ARG_EDIT_MODE, false);

		MiniGalleryFragment f = new MiniGalleryFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_gallery_placeholder, f).commit();
	}

	private void setupMiniMap() {
		FragmentManager fm = getSupportFragmentManager();
		MiniMapFragment f = (MiniMapFragment) fm.findFragmentByTag("minimap");

		if (f == null) {
			Bundle args = new Bundle();
			args.putBoolean(MiniMapFragment.ARG_EDIT_MODE, false);
			args.putDouble(MiniMapFragment.ARG_LATITUDE, art.latitude);
			args.putDouble(MiniMapFragment.ARG_LONGITUDE, art.longitude);

			f = new MiniMapFragment();
			f.setArguments(args);

			fm.beginTransaction().replace(R.id.mini_map_placeholder, f, "minimap").commit();
		}
	}

	private final LoaderCallbacks<List<Comment>> asyncCallback = new LoaderCallbacks<List<Comment>>() {

		@Override
		public void onLoaderReset(Loader<List<Comment>> loader) {}

		@Override
		public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> result) {
			switch (loader.getId()) {
			case LOAD_COMMENTS:
				Utils.d(TAG, "onLoadFinished(): result=" + result);

				if (result == null || result.isEmpty()) {
					findViewById(R.id.art_detail_comments).setVisibility(View.GONE);

					commentsCount = 0;
					tvComments.setText(tvComments.getText() + " (" + commentsCount + ")");
					return;
				}

				commentsCount = result.size();
				tvComments.setText(tvComments.getText() + " (" + commentsCount + ")");

				int displayCount = result.size() < MAX_COMMENTS ? result.size() : MAX_COMMENTS;
				if (displayCount < commentsCount) {
					tvViewAll.setVisibility(View.VISIBLE);
				}

				LinearLayout parent = (LinearLayout) findViewById(R.id.art_detail_comments);
				for (int i = 0; i < displayCount; i++) {
					Comment c = result.get(i);
					c.artSlug = art.slug;
					comments.add(c);
					populateComment(parent, i, c);
				}
				break;
			}
		}

		@Override
		public Loader<List<Comment>> onCreateLoader(int id, final Bundle args) {
			switch (id) {
			case LOAD_COMMENTS:
				return new AsyncLoader<List<Comment>>(ArtDetail.this) {
					@Override
					public List<Comment> loadInBackground() {
						try {
							return ServiceFactory.getArtService().getComments(args.getString(ARG_ART_SLUG));
						}
						catch (ArtAroundException e) {
							return null;
						}
					}
				};
			default:
				return null;
			}
		}
	};

	private final LoaderCallbacks<Boolean> cursorCallback = new LoaderCallbacks<Boolean>() {
		@Override
		public Loader<Boolean> onCreateLoader(int id, final Bundle args) {
			switch (id) {
			case LOAD_FAVORITE:
				return new AsyncLoader<Boolean>(ArtDetail.this) {
					@Override
					public Boolean loadInBackground() {
						if (!isFavorite()) {
							ContentValues values = new ContentValues(1);
							values.put(ArtFavorites.SLUG, args.getString(ARG_ART_SLUG));
							ArtAroundProvider.contentResolver.insert(ArtFavorites.CONTENT_URI, values);
							return true;
						}
						else {
							ArtAroundProvider.contentResolver.delete(ArtFavorites.CONTENT_URI,
									ArtFavorites.SLUG + "=?", new String[] { art.slug });
							return false;
						}
					}
				};
			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(Loader<Boolean> loader, Boolean result) {
			switch (loader.getId()) {
			case LOAD_FAVORITE:
				if (result != null && result) {
					btnFavorite.setImageResource(R.drawable.ic_remove_favorite_background);
					Utils.showToast(ArtDetail.this, R.string.art_added_favorite);
				}
				else {
					btnFavorite.setImageResource(R.drawable.ic_add_favorite_background);
					Utils.showToast(ArtDetail.this, R.string.art_removed_favorite);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Boolean> loader) {}
	};

	// all this because of the problems with ListView inside ScrollView :(
	private void populateComment(LinearLayout parent, int i, Comment c) {
		View view = getLayoutInflater().inflate(R.layout.comment_item, null);
		view.setId(i);

		LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.topMargin = 10;
		view.setLayoutParams(params);

		String username = c.username;
		if (!TextUtils.isEmpty(c.url)) {
			((TextView) view.findViewById(R.id.comment_url)).setText(c.url);
			if (username != null) username += " | ";
		}

		if (!TextUtils.isEmpty(username)) {
			((TextView) view.findViewById(R.id.comment_username)).setText(username);
		}

		if (c.date != null) {
			((TextView) view.findViewById(R.id.comment_date)).setText(Utils.textDateFormatter.format(c.date));
		}

		if (!TextUtils.isEmpty(c.message)) {
			((TextView) view.findViewById(R.id.comment_message)).setText(c.message);
		}

		parent.addView(view);
		parent.invalidate();
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
