package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.LoaderPayload;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Comment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class ArtDetail extends FragmentActivity implements GallerySaver {
	public static final String TAG = "ArtDetail";
	private static final String TAG_MINI_GALLERY = "mini_gallery";

	public static final String EXTRA_ART = "art";
	public static final String EXTRA_EDIT = "edit";
	public static final String EXTRA_PHOTOS = "photos";

	private static final String SAVE_COMMENTS = "comments";
	private static final String SAVE_COMMENTS_COUNT = "comments_count";
	private static final String SAVE_MINI_GALLERY = "save_mini_gallery";

	private static final String ARG_ART_SLUG = "art_slug";
	private static final String ARG_NEW_COMMENT = "new_comment";

	private static final int LOADER_ASYNC_FAVORITE = 0;
	private static final int LOADER_ASYNC_COMMENTS = 1;
	private static final int MAX_COMMENTS = 3;
	private static final int SUBMIT_COMMENT = 4;

	private static final String PAGE_BASE_URL = "http://theartaround.us/arts/";

	private static final int DIALOG_EMPTY_INPUT = 0;

	private Art art;
	private Bundle savedGalleryState;
	private final ArrayList<Comment> comments = new ArrayList<Comment>();
	private int commentsCount = -1;

	private TextView tvComments;
	private TextView tvViewAll;
	private ImageButton btnFavorite;

	private EditText edCommentName;
	private EditText edCommentUrl;
	private EditText edCommentText;
	private EditText edCommentEmail;

	private ImageView imgLoader;
	private Animation rotateAnim;

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
			Utils.d(TAG, "onCreate(): art=", art);
			return;
		}

		setupUi();
		setupState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(SAVE_COMMENTS, comments);
		outState.putInt(SAVE_COMMENTS_COUNT, commentsCount);
		outState.putBundle(SAVE_MINI_GALLERY, savedGalleryState);
		super.onSaveInstanceState(outState);
	}

	@SuppressWarnings("unchecked")
	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			comments.addAll((ArrayList<Comment>) savedInstanceState.getSerializable(SAVE_COMMENTS));
			commentsCount = savedInstanceState.getInt(SAVE_COMMENTS_COUNT);
			if (savedInstanceState.containsKey(SAVE_MINI_GALLERY)) {
				savedGalleryState = savedInstanceState.getBundle(SAVE_MINI_GALLERY);
			}
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

		lm.restartLoader(LOADER_ASYNC_COMMENTS, args, asyncCallback);
	}

	private void setupUi() {
		setupActionbar();
		setupBottombar();

		setupFields();
		setupMiniGallery();
		setupMiniMap();
	}

	private void setupBottombar() {
		imgLoader = (ImageView) findViewById(R.id.bottombar_loader);
		rotateAnim = Utils.getRoateAnim(this);

		Button btnEdit = (Button) findViewById(R.id.art_detail_edit);
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent iEdit = new Intent(ArtDetail.this, ArtEdit.class);
				iEdit.putExtra(ArtEdit.EXTRA_ART, art);
				iEdit.putExtra(EXTRA_PHOTOS,
						((MiniGalleryFragment) getSupportFragmentManager().findFragmentByTag(TAG_MINI_GALLERY))
								.getPhotos());
				iEdit.putExtra(EXTRA_EDIT, true);
				startActivityForResult(iEdit, 0);
			}
		});

		Button btnSubmit = (Button) findViewById(R.id.art_detail_comment);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (validateTexts()) {
					onSubmitComment();
				}
			}
		});
	}

	private boolean validateTexts() {
		boolean ok = true;

		// all empty
		if (TextUtils.isEmpty(edCommentName.getText()) || TextUtils.isEmpty(edCommentEmail.getText())
				|| TextUtils.isEmpty(edCommentText.getText())) {
			ok = false;
			showDialog(DIALOG_EMPTY_INPUT);
		}
		return ok;
	}

	protected void onSubmitComment() {
		Comment comment = new Comment();
		comment.name = edCommentName.getText().toString();
		comment.url = edCommentUrl.getText().toString();
		comment.text = edCommentText.getText().toString();
		comment.email = edCommentEmail.getText().toString();

		LoaderManager lm = getSupportLoaderManager();
		Bundle args = new Bundle();
		args.putSerializable(ARG_NEW_COMMENT, comment);

		toggleLoading(true);
		lm.restartLoader(SUBMIT_COMMENT, args, asyncCallback);
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

	private void setupActionbar() {
		if (!TextUtils.isEmpty(art.title)) {
			((TextView) findViewById(R.id.art_detail_title)).setText(art.title);
		}

		btnFavorite = (ImageButton) findViewById(R.id.btn_favorite);
		if (art.isFavorite) {
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
		Bundle args = new Bundle();
		args.putString(ARG_ART_SLUG, art.slug);
		getSupportLoaderManager().restartLoader(LOADER_ASYNC_FAVORITE, args, cursorCallback);
	}

	private void setupFields() {
		StringBuilder str = new StringBuilder();

		TextView tvArtist = (TextView) findViewById(R.id.art_detail_artist);
		if (art.artist != null && !TextUtils.isEmpty(art.artist)) {
			str.append(art.artist);

			if (art.year > 0) {
				str.append(" - ");
			}
		}
		if (art.year > 0) {
			str.append("<b>").append(art.year).append("</b>");
		}
		if (str.length() > 0) {
			tvArtist.setText(Html.fromHtml(str.toString()));
		}
		else {
			tvArtist.setVisibility(View.GONE);
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
		edCommentEmail = (EditText) findViewById(R.id.art_detail_input_email);
		edCommentText = (EditText) findViewById(R.id.art_detail_input_message);
		Utils.setHintSpan(edCommentText, edCommentText.getHint());
	}

	private void clearCommentFields() {
		edCommentName.setText("");
		edCommentUrl.setText("");
		edCommentEmail.setText("");
		edCommentText.setText("");
	}

	private void setupMiniGallery() {
		Bundle args = new Bundle();
		args.putStringArrayList(MiniGalleryFragment.ARG_PHOTO_IDS, art.photoIds);
		args.putString(MiniGalleryFragment.ARG_TITLE, art.title);
		args.putBoolean(MiniGalleryFragment.ARG_EDIT_MODE, false);

		MiniGalleryFragment f = new MiniGalleryFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_gallery_placeholder, f, TAG_MINI_GALLERY).commit();
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
			Utils.d(TAG, "onLoadFinished(): payload=", payload);

			switch (loader.getId()) {
			case LOADER_ASYNC_COMMENTS:

				if (payload.getStatus() == LoaderPayload.STATUS_OK) {
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
				else {
					Toast.makeText(ArtDetail.this, R.string.load_data_failure,
							Toast.LENGTH_LONG).show();
				}
				break;
			case SUBMIT_COMMENT:
				toggleLoading(false);

				if (payload.getStatus() == LoaderPayload.STATUS_OK) {
					Boolean result = (Boolean) payload.getResult();
					if (result != null && result) {
						Toast.makeText(ArtDetail.this.getApplicationContext(), R.string.submit_comment_success,
								Toast.LENGTH_SHORT).show();
						clearCommentFields();
					}
					else {
						Toast.makeText(ArtDetail.this.getApplicationContext(), R.string.submit_comment_failure,
								Toast.LENGTH_LONG).show();
					}
				}
				else {
					Toast.makeText(ArtDetail.this.getApplicationContext(), R.string.submit_comment_failure,
							Toast.LENGTH_LONG).show();
				}
				break;
			}

			loader.stopLoading();
		}

		@Override
		public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
			switch (id) {
			case LOADER_ASYNC_COMMENTS:
				return new AsyncLoader<LoaderPayload>(ArtDetail.this) {
					@Override
					public LoaderPayload loadInBackground() {
						try {
							return new LoaderPayload(LoaderPayload.STATUS_OK, ServiceFactory.getArtService()
									.getComments(args.getString(ARG_ART_SLUG)));
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
							return new LoaderPayload(LoaderPayload.STATUS_OK, ServiceFactory.getArtService()
									.submitComment(art.slug, comment));
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
			case LOADER_ASYNC_FAVORITE:
				return new AsyncLoader<Boolean>(ArtDetail.this) {
					@Override
					public Boolean loadInBackground() {
						ContentValues values = new ContentValues(1);
						values.put(Arts.FAVORITE, !art.isFavorite ? 1 : 0);
						int ok = ArtAroundProvider.contentResolver.update(Arts.CONTENT_URI, values, Arts.SLUG + "=?",
								new String[] { args.getString(ARG_ART_SLUG) });
						if (ok == 1) {
							art.isFavorite = !art.isFavorite;
						}
						return art.isFavorite;
					}
				};
			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(Loader<Boolean> loader, Boolean result) {
			switch (loader.getId()) {
			case LOADER_ASYNC_FAVORITE:
				if (result != null && result) {
					btnFavorite.setImageResource(R.drawable.ic_remove_favorite_background);
					Toast.makeText(ArtDetail.this, R.string.art_added_favorite,
							Toast.LENGTH_SHORT).show();
				}
				else {
					btnFavorite.setImageResource(R.drawable.ic_add_favorite_background);
					Toast.makeText(ArtDetail.this, R.string.art_removed_favorite,
							Toast.LENGTH_SHORT).show();
				}
			}
			loader.stopLoading();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share_art:
			onShareArt();
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
			b.append(" ").append(art.artist);
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

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EMPTY_INPUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.art_detail_hint_input_empty_title);
			builder.setMessage(R.string.art_detail_hint_input_empty_msg);
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
	public void saveGalleryState(Bundle args) {
		savedGalleryState = args;
	}

	@Override
	public Bundle restoreGalleryState() {
		return savedGalleryState;
	}
}
