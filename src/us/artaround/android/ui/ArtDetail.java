package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.Utils;
import us.artaround.models.Art;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class ArtDetail extends FragmentActivity {
	public static final String TAG = "ArtAround.ArtDetail";
	public static final String EXTRA_ART = "art";

	private Art art;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);
		
		setContentView(R.layout.art_detail);
		
		Intent intent = getIntent();
		if(intent.hasExtra(EXTRA_ART)) {
			art = (Art) intent.getSerializableExtra(EXTRA_ART);
		}
		if(art == null) {
			Utils.w(TAG, "onCreate(): art=" + art);
			return;
		}
		
		setupUi();
	}

	private void setupUi() {
		setupMiniGallery();
		setupMiniMap();
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
	}

	private void setupMiniMap() {
		Bundle args = new Bundle();
		args.putBoolean(MiniMapFragment.ARG_EDIT_MODE, true);

		MiniMapFragment f = new MiniMapFragment();
		f.setArguments(args);

		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.mini_map_placeholder, f).commit();
	}


}
