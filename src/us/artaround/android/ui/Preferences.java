package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.SharedPreferencesCompat;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
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

	private void clearCache() {
		showDialog(DIALOG_PROGRESS);

		ArtAroundProvider.contentResolver.delete(Arts.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Artists.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Categories.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Neighborhoods.CONTENT_URI, null, null);

		dismissDialog(DIALOG_PROGRESS);

		Editor edit = prefs.edit();
		edit.putBoolean(Utils.KEY_CLEARED_CACHE, true);
		edit.putLong(Utils.KEY_LAST_UPDATE, 0);
		SharedPreferencesCompat.apply(edit);

		Toast.makeText(this, R.string.clear_cache_success, Toast.LENGTH_LONG).show();
	}

}