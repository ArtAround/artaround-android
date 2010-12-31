package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.AsyncDeleteListener;
import us.artaround.android.commons.SharedPreferencesCompat;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDb.Arts;
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
import android.util.Log;
import android.widget.Toast;

public class Preferences extends PreferenceActivity implements AsyncDeleteListener {
	private final static String KEY_CLEAR_CACHE = "clear_cache";
	private final static int DIALOG_PROGRESS = 0;

	private SharedPreferences prefs;
	private NotifyingAsyncQueryHandler queryHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String key = preference.getKey();

		if (KEY_CLEAR_CACHE.equals(key)) {
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
		queryHandler.startDelete(Arts.CONTENT_URI);
	}

	@Override
	public void onDeleteComplete(int token, Object cookie, int result) {
		Log.d(Utils.TAG, "Deleted " + result + " from db.");
		dismissDialog(DIALOG_PROGRESS);

		Editor edit = prefs.edit();
		edit.putBoolean(Utils.KEY_CLEARED_CACHE, true);
		edit.putLong(Utils.KEY_LAST_UPDATE, 0);
		SharedPreferencesCompat.apply(edit);

		Toast.makeText(this, R.string.clear_cache_success, Toast.LENGTH_LONG).show();
	}
}
