package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.NotifyingAsyncQueryHandler;
import us.artaround.android.common.Utils;
import us.artaround.android.common.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.ArtFavorites;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundProvider;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ArtFavs extends ListActivity implements NotifyingAsyncQueryListener {
	private static final int QUERY_FAVORITES = 0;

	private NotifyingAsyncQueryHandler queryHandler;

	private Animation rotateAnim;
	private ImageView imgRefresh;
	private View loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_favorites);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		initVars();
		setupUi();
	}

	private void initVars() {
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
	}

	private void setupUi() {
		imgRefresh = (ImageView) findViewById(R.id.img_refresh);
		rotateAnim = Utils.getRoateAnim(this);

		showLoading(true);
		queryHandler.startQuery(QUERY_FAVORITES, null, ArtFavorites.CONTENT_URI, ArtAroundDatabase.ARTS_PROJECTION,
				null, null, null);
		registerForContextMenu(getListView());
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		startManagingCursor(cursor);
		showLoading(false);
		switch (token) {
		case QUERY_FAVORITES:
			if (cursor != null && cursor.moveToFirst()) {
				Utils.d(Utils.TAG, "There are " + cursor.getCount() + " favorites");
				ListView listView = getListView();
				listView.setAdapter(new SimpleCursorAdapter(this, R.layout.art_favorites_item, cursor, new String[] {
						Arts.TITLE, Arts.LOCATION_DESCRIPTION }, new int[] { R.id.art_title, R.id.art_description }));

				listView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						startActivity(new Intent(ArtFavs.this, ArtInfo.class).putExtra("art", ArtAroundDatabase
								.artFromCursor(((SimpleCursorAdapter) getListView().getAdapter()).getCursor())));
					}
				});
			}
			break;
		}
	}

	private void showLoading(boolean isLoading) {
		if (isLoading) {
			if (loading == null) {
				loading = findViewById(R.id.stub_loading);
			}
			loading.setVisibility(View.VISIBLE);

			imgRefresh.setVisibility(View.VISIBLE);
			imgRefresh.startAnimation(rotateAnim);
		}
		else {
			if (loading != null) {
				loading.setVisibility(View.GONE);
			}

			imgRefresh.clearAnimation();
			imgRefresh.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onContextItemSelected(item);
	}
}
