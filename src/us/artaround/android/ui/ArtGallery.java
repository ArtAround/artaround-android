package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import us.artaround.android.services.ServiceFactory;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ArtGallery extends FragmentActivity {

	public static final String EXTRA_WRAPPERS = "extra_wrappers";
	public static final String EXTRA_TITLE = "extra_title";
	public static final String EXTRA_IS_EDIT_MODE = "extra_edit_mode";

	private ArrayList<PhotoWrapper> photos;
	private String artTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);

		ServiceFactory.init(getApplicationContext());

		setupState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//FIXME OutOfMemoryExceptions
		System.gc();
	}

	@SuppressWarnings("unchecked")
	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null) return;

		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_WRAPPERS)) {
			photos = (ArrayList<PhotoWrapper>) intent.getExtras().getSerializable(EXTRA_WRAPPERS);
		}
		if (intent.hasExtra(EXTRA_TITLE)) {
			artTitle = intent.getExtras().getString(EXTRA_TITLE);
		}

		getSupportFragmentManager()
				.beginTransaction()
				.replace(android.R.id.content,
						new GalleryFragment(photos, artTitle, intent.getExtras().getBoolean(EXTRA_IS_EDIT_MODE, false)))
				.commit();
	}
}
