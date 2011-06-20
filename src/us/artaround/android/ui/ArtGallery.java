package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class ArtGallery extends FragmentActivity implements GallerySaver {
	private static final String TAG = "ArtGallery";
	private static final String TAG_GALLERY = "gallery";

	public static final String EXTRA_PHOTOS = "photos";
	public static final String EXTRA_TITLE = "title";
	private static final String SAVE_GALLERY = "save_gallery";

	private Bundle savedGalleryState;
	private ArrayList<PhotoWrapper> photos;
	private String title;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);

		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_PHOTOS)) {
			photos = (ArrayList<PhotoWrapper>) intent.getExtras().getSerializable(EXTRA_PHOTOS);
			Utils.d(TAG, "onCreate(): photos=", photos);
		}
		if (intent.hasExtra(EXTRA_TITLE)) {
			title = intent.getExtras().getString(EXTRA_TITLE);
		}

		setupState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBundle(SAVE_GALLERY, savedGalleryState);
		super.onSaveInstanceState(outState);
	}

	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_GALLERY)) {
			savedGalleryState = savedInstanceState.getBundle(SAVE_GALLERY);
			return;
		}

		GalleryFragment f = new GalleryFragment();
		Bundle args = new Bundle();
		args.putSerializable(GalleryFragment.ARG_PHOTOS, photos);
		args.putString(GalleryFragment.ARG_TITLE, title);
		f.setArguments(args);
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(android.R.id.content, f, TAG_GALLERY).commit();
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
