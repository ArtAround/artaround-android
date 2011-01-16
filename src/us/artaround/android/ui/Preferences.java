package us.artaround.android.ui;

import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.AsyncDeleteListener;
import us.artaround.android.commons.SharedPreferencesCompat;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.City;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class Preferences extends PreferenceActivity implements AsyncDeleteListener {
	private final static String CLEAR_CACHE_KEY = "clear_cache";
	private final static String CHANGE_CITY_KEY = "change_city";

	private final static int DIALOG_PROGRESS = 0;
	private final static int DIALOG_CHANGE_CITY = 1;

	private SharedPreferences prefs;
	private NotifyingAsyncQueryHandler queryHandler;
	private AtomicInteger leftToClear;

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
		if (CLEAR_CACHE_KEY.equals(key)) {
			clearCache();
		}
		else if (CHANGE_CITY_KEY.equals(key)) {
			showDialog(DIALOG_CHANGE_CITY);
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
		case DIALOG_CHANGE_CITY:
			final Dialog dialog = new Dialog(this, R.style.CustomDialog);
			dialog.setTitle(R.string.choose_city);
			dialog.setContentView(R.layout.dialog_cities);

			LayoutInflater inflater = LayoutInflater.from(this);
			final RadioGroup gr = (RadioGroup) dialog.findViewById(R.id.cities);
			City[] cities = ServiceFactory.getCities();
			City current = ServiceFactory.getCurrentCity();

			for (int i = 0; i < cities.length; i++) {
				View view = inflater.inflate(R.layout.city_row, null);
				view.setTag(cities[i].code);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int code = (Integer) v.getTag();
						gr.clearCheck();
						gr.check(code);
						setCurrentCity(code, dialog);
					}
				});

				RadioButton rb = (RadioButton) view.findViewById(R.id.btn_radio);
				rb.setText(cities[i].name);
				rb.setId(cities[i].code);
				rb.setChecked(current.code == i);

				ImageView img = (ImageView) view.findViewById(R.id.img_flag);
				img.setImageResource(i == cities[0].code ? R.drawable.ic_flag_us : R.drawable.ic_flag_ro);

				gr.addView(view, i);
			}

			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	private void setCurrentCity(int cityCode, Dialog dialog) {
		Editor editor = prefs.edit();
		editor.putInt(Utils.KEY_CITY_CODE, cityCode);
		SharedPreferencesCompat.apply(editor);
		dialog.dismiss();
		Utils.d(Utils.TAG, "City changed to " + cityCode);
	}

	private void clearCache() {
		showDialog(DIALOG_PROGRESS);

		leftToClear = new AtomicInteger(4);

		queryHandler.startDelete(Arts.CONTENT_URI, Arts.CONTENT_URI);
		queryHandler.startDelete(Artists.CONTENT_URI, Artists.CONTENT_URI);
		queryHandler.startDelete(Categories.CONTENT_URI, Categories.CONTENT_URI);
		queryHandler.startDelete(Neighborhoods.CONTENT_URI, Neighborhoods.CONTENT_URI);
	}

	@Override
	public void onDeleteComplete(int token, Object cookie, int result) {
		Log.d(Utils.TAG, "Deleted " + result + " from " + cookie);

		int left = leftToClear.decrementAndGet();

		if (left == 0) {
			dismissDialog(DIALOG_PROGRESS);

			Editor edit = prefs.edit();
			edit.putBoolean(Utils.KEY_CLEARED_CACHE, true);
			edit.putLong(Utils.KEY_LAST_UPDATE, 0);
			SharedPreferencesCompat.apply(edit);

			Toast.makeText(this, R.string.clear_cache_success, Toast.LENGTH_LONG).show();
		}
	}
}