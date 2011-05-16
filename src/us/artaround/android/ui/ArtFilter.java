package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class ArtFilter extends FragmentActivity {

	public static final int TYPE_CATEGORY = 0;
	public static final int TYPE_NEIGHBORHOOD = 1;
	public static final int TYPE_TITLE = 2;
	public static final int TYPE_ARTIST = 3;

	public static final int[] FILTER_TYPE_NAMES = { R.string.art_filter_by_category, R.string.art_filter_by_area,
			R.string.art_filter_by_title, R.string.art_filter_by_artist };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.setTheme(this);
		Utils.enableDump(this);

		setContentView(R.layout.art_filter);
		((TextView) findViewById(R.id.window_title)).setText(R.string.art_filter_title);

		setupUi();
	}

	private void setupUi() {
		final String[] items = new String[FILTER_TYPE_NAMES.length];
		for (int i = 0; i < FILTER_TYPE_NAMES.length; i++) {
			items[i] = getString(FILTER_TYPE_NAMES[i]);
		}

		final InstantAutoComplete tvType = (InstantAutoComplete) findViewById(R.id.filter_type);
		tvType.clearListSelection();
		tvType.setInputType(0); // hide the keyboard and text cursor

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.autocomplete_item, items);
		tvType.setAdapter(adapter);

		ImageButton dropdown = (ImageButton) findViewById(R.id.dropdown_filter_type);
		dropdown.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!TextUtils.isEmpty(tvType.getText())) {
					tvType.setHint(tvType.getText());
					tvType.setText("");
				}

				if (tvType.isPopupShowing())
					tvType.dismissDropDown();
				else
					tvType.showDropDown();
			}
		});
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
		iHome.putExtra(ArtMap.EXTRA_FILTERS,
				((ArtFilterListFragment) getSupportFragmentManager().findFragmentById(R.id.filters)).getFilters());
		setResult(ArtMap.REQUEST_FILTER, iHome);
		finish();
	}
}
