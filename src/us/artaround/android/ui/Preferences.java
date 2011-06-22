package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.SharedPreferencesCompat;
import us.artaround.android.common.Utils;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	private final static String CLEAR_CACHE_KEY = "clear_cache";

	private final static int DIALOG_PROGRESS = 0;

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String key = preference.getKey();
		if (CLEAR_CACHE_KEY.equals(key)) {
			clearCache();
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			ProgressDialog progress = new ProgressDialog(this);
			progress.setCancelable(false);
			progress.setMessage(getString(R.string.clear_cache_progress));
			return progress;
		default:
			return super.onCreateDialog(id);
		}
	}

	private final Runnable clearCacheFinish = new Runnable() {

		@Override
		public void run() {
			dismissDialog(DIALOG_PROGRESS);

			Editor edit = prefs.edit();
			edit.putBoolean(Utils.KEY_CLEARED_CACHE, true);
			edit.putLong(Utils.KEY_LAST_UPDATE, 0);
			SharedPreferencesCompat.apply(edit);

			Toast.makeText(Preferences.this.getApplicationContext(), R.string.clear_cache_success, Toast.LENGTH_LONG)
					.show();
		}
	};

	private void clearCache() {
		showDialog(DIALOG_PROGRESS);

		new Thread() {
			@Override
			public void run() {
				Utils.clearCache();
				Utils.deleteCachedFiles();

				runOnUiThread(clearCacheFinish);
			}
		}.start();
	}

}