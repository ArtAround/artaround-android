package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.models.Art;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TabHost;

public class ArtPage extends TabActivity {
	public static final String TAG = "ArtAround.Page";

	private static final String WHERE = Arts.SLUG + "=?";

	private static final String TAB_DETAILS = "details";
	private static final String TAB_GALLERY = "gallery";
	private static final String TAB_COMMENTS = "comments";

	private Art art;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_page);

		String slug = getIntent().getStringExtra("slug");
		Cursor c = ArtAroundProvider.contentResolver.query(Arts.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION, WHERE,
				new String[] { slug }, null);

		if (c == null || !c.moveToFirst()) {
			Utils.w(ArtPage.TAG, "Someting is wrong, can't find the art with slug " + slug);
			finish();
			return;
		}

		art = ArtAroundDatabase.artFromCursor(c);
		c.close();

		setupTabs();
	}

	private void setupTabs() {
		TabHost tabHost = getTabHost();

		Resources res = getResources();

		Utils.addTab(this, tabHost, TAB_DETAILS, detailsIntent(), getString(R.string.tab_details),
				res.getDrawable(R.drawable.ic_tab_details));
		Utils.addTab(this, tabHost, TAB_COMMENTS, commentsIntent(), getString(R.string.tab_comments),
				res.getDrawable(R.drawable.ic_tab_comments));
		Utils.addTab(this, tabHost, TAB_GALLERY, galleryIntent(), getString(R.string.tab_gallery),
				res.getDrawable(R.drawable.ic_tab_gallery));

		tabHost.setCurrentTabByTag(TAB_DETAILS);
	}
	
	private Intent galleryIntent() {
		return new Intent(this, ArtGallery.class).putExtra("art", art);
	}

	private Intent commentsIntent() {
		return new Intent(this, ArtComments.class).putExtra("art_slug", art.slug);
	}

	private Intent detailsIntent() {
		return new Intent(this, ArtDetails.class).putExtra("art", art).putExtras(getIntent());
	}
}
