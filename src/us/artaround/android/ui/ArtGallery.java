package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.LoadFlickrPhotoCommand;
import us.artaround.models.ArtAroundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ArtGallery extends ArtAroundActivity {
	private static final String TAG = "ArtAround.ArtGallery";

	public static final String EXTRAS_PHOTOS = "photos";
	public static final String EXTRAS_TITLE = "title";
	public static final String EXTRAS_CURRENT_PHOTO = "current_photo";

	private ArrayList<PhotoWrapper> photoUris;
	private String title;

	private Animation rotateAnim;
	private Gallery gallery;
	private ImageView imgLoader;

	@SuppressWarnings("unchecked")
	@Override
	protected void onChildCreate(Bundle savedInstanceState) {
		setContentView(R.layout.art_gallery);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		Intent intent = getIntent();
		if (intent.hasExtra(EXTRAS_PHOTOS)) {
			photoUris = (ArrayList<PhotoWrapper>) intent.getExtras().getSerializable(EXTRAS_PHOTOS);
			Utils.d(TAG, "onChildCreate(): photoUris= " + photoUris);
		}
		if (intent.hasExtra(EXTRAS_TITLE)) {
			title = intent.getExtras().getString(EXTRAS_TITLE);
		}

		setupUi();
	}

	@Override
	protected void onChildEndCreate(Bundle savedInstanceState) {
		setupState();
	}

	private void setupUi() {
		imgLoader = (ImageView) findViewById(R.id.img_loader);
		rotateAnim = Utils.getRoateAnim(this);

		if (!TextUtils.isEmpty(title)) {
			TextView tvTitle = (TextView) findViewById(R.id.title);
			tvTitle.setText(title);
		}

		ImageButton btnComment = (ImageButton) findViewById(R.id.btn_comment);
		ImageButton btnAddPhoto = (ImageButton) findViewById(R.id.btn_add_image);

		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new GalleryAdapter(this, photoUris));
	}

	private void setupState() {
		for (int i = 0; i < photoUris.size(); i++) {
			PhotoWrapper pw = photoUris.get(i);
			if (pw != null) {
				if (!TextUtils.isEmpty(pw.id) && pw.id.indexOf(MiniGalleryAdapter.NEW_PHOTO) == -1) {
					startTask(new LoadFlickrPhotoCommand(0, pw.id));
				}
			}
		}
	}

	private void showLoading(boolean loading) {
		if (loading) {
			imgLoader.setVisibility(View.VISIBLE);
			imgLoader.startAnimation(rotateAnim);
		}
		else {
			imgLoader.clearAnimation();
			imgLoader.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {
		super.onPreExecute(command);
		showLoading(true);
	}

	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		super.onPostExecute(command, result, exception);

		Utils.d(TAG, "onPostExecute(): LOAD_PHOTO");
		showLoading(false);

		if (exception != null) {
			Utils.d(TAG, "onPostExecute(): exception " + exception);
			Utils.showToast(this, R.string.load_photos_error);
			return;
		}

		GalleryAdapter adapter = (GalleryAdapter) gallery.getAdapter();
		for (PhotoWrapper pw : photoUris) {
			if (pw != null && pw.id.equals(command.id)) {
				pw.drawable = (Drawable) result;
				adapter.notifyDataSetChanged();
				break;
			}
		}
	}
}
