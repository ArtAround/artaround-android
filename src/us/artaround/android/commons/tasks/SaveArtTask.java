package us.artaround.android.commons.tasks;

import java.util.List;

import us.artaround.android.commons.Database;
import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.os.AsyncTask;
import android.util.Log;

public class SaveArtTask extends AsyncTask<Object, Void, Void> {

	@SuppressWarnings("unchecked")
	@Override
	protected Void doInBackground(Object... params) {
		if (params == null || params.length < 2) {
			Log.w(Utils.TAG, "SaveArtTask needs 'database' and 'arts' parameter!");
			return null;
		}
		Database db = (Database) params[0];
		List<Art> arts = (List<Art>) params[1];
		
		if (!db.isOpen()) {
			Log.w(Utils.TAG, "SaveArtTask needs an open database connection!");
			return null;
		}

		db.updateCache(arts);
		return null;
	}
}
