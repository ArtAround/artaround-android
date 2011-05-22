package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.LoaderPayload;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
	private static final String ARG_NEW_COMMENT = "new_comment";

	private static final int LOAD_FAVORITE = 0;
	private static final int LOAD_COMMENTS = 1;
	private static final int MAX_COMMENTS = 3;
	private static final int SUBMIT_COMMENT = 4;

	private static final String PAGE_BASE_URL = "http://theartaround.us/arts/";

	private Art art;
	private final ArrayList<Comment> comments = new ArrayList<Comment>();
	private int commentsCount = -1;

	private TextView tvComments;
	private TextView tvViewAll;
	private ImageButton btnFavorite;

	private EditText edCommentName;
	private EditText edCommentUrl;
	private EditText edCommentText;

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
		Button btnEdit = (Button) findViewById(R.id.art_detail_edit);
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent iEdit = new Intent(ArtDetail.this, ArtEdit.class);
				iEdit.putExtra(ArtEdit.EXTRA_ART, art);
				startActivity(iEdit);
			}
		});

		Button btnSubmit = (Button) findViewById(R.id.art_detail_comment);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSubmitComment();
			}
		});
	}

	protected void onSubmitComment() {
		Comment comment = new Comment();
		comment.name = edCommentName.getText().toString();
		comment.url = edCommentUrl.getText().toString();
		comment.text = edCommentText.getText().toString();

		if (TextUtils.isEmpty(comment.name) && TextUtils.isEmpty(comment.url) && TextUtils.isEmpty(comment.text))
			return;

		LoaderManager lm = getSupportLoaderManager();
		Bundle args = new Bundle();
		args.putSerializable(ARG_NEW_COMMENT, comment);

		if (lm.getLoader(SUBMIT_COMMENT) == null) {
			lm.initLoader(SUBMIT_COMMENT, args, asyncCallback);
		}
		else {
			lm.restartLoader(SUBMIT_COMMENT, args, asyncCallback);
		}
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

		edCommentName = (EditText) findViewById(R.id.art_detail_input_name);

		edCommentUrl = (EditText) findViewById(R.id.art_detail_input_url);

		edCommentText = (EditText) findViewById(R.id.art_detail_input_message);
		Utils.setHintSpan(edCommentText, edCommentText.getHint());
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

	private final LoaderCallbacks<LoaderPayload> asyncCallback = new LoaderCallbacks<LoaderPayload>() {

		@Override
		public void onLoaderReset(Loader<LoaderPayload> loader) {}

		@SuppressWarnings("unchecked")
		@Override
		public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
			switch (loader.getId()) {
			case LOAD_COMMENTS:
				Utils.d(TAG, "onLoadFinished(): result=" + payload);

				if (payload.getStatus() == LoaderPayload.RESULT_OK) {
					List<Comment> result = (List<Comment>) payload.getResult();

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
				}
				break;
			case SUBMIT_COMMENT:
				break;
			}
		}

		@Override
		public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
			switch (id) {
			case LOAD_COMMENTS:
				return new AsyncLoader<LoaderPayload>(ArtDetail.this) {
					@Override
					public LoaderPayload loadInBackground() {
						try {
							return new LoaderPayload(ServiceFactory.getArtService().getComments(
									args.getString(ARG_ART_SLUG)));
						}
						catch (ArtAroundException e) {
							return new LoaderPayload(e);
						}
					}
				};
			case SUBMIT_COMMENT:
				return new AsyncLoader<LoaderPayload>(ArtDetail.this) {
					@Override
					public LoaderPayload loadInBackground() {
						Comment comment = (Comment) args.getSerializable(ARG_NEW_COMMENT);
						try {
							return new LoaderPayload(ServiceFactory.getArtService().submitComment(art.slug, comment));
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

		String username = c.name;
		if (!TextUtils.isEmpty(c.url)) {
			((TextView) view.findViewById(R.id.comment_url)).setText(c.url);
			if (username != null) username += " | ";
		}

		if (!TextUtils.isEmpty(username)) {
			((TextView) view.findViewById(R.id.comment_username)).setText(username);
		}

		if (c.createdAt != null) {
			((TextView) view.findViewById(R.id.comment_date)).setText(Utils.textDateFormatter.format(c.createdAt));
		}

		if (!TextUtils.isEmpty(c.text)) {
			((TextView) view.findViewById(R.id.comment_message)).setText(c.text);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.art_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.favorite_art).setTitle(isFavorite() ? R.string.remove_favorite : R.string.add_favorite);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share_art:
			onShareArt();
			return true;
		case R.id.favorite_art:
			onFavoriteArt();
			return true;
		case R.id.street_view:
			startActivity(Utils.getStreetViewIntent(art.latitude, art.longitude));
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private String getShareText() {
		StringBuilder b = new StringBuilder(getString(R.string.share_art_header));
		b.append(Utils.NL).append(getString(R.string.share_artist));
		if (art.artist != null) {
			b.append(" ").append(art.artist.name);
		}
		else {
			b.append(getString(R.string.unknown));
		}
		b.append(Utils.NL).append(art.locationDesc);
		b.append(Utils.NL).append(getString(R.string.share_category)).append(" ").append(art.category);
		b.append(Utils.NL).append(getString(R.string.share_neighborhood)).append(" ").append(art.neighborhood);
		return b.toString();
	}

	private void onShareArt() {
		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
				.putExtra(Intent.EXTRA_TEXT, getShareText());
		startActivity(Intent.createChooser(intent, getString(R.string.share_art_title)));
	}
}
