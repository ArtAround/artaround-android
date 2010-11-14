package us.artaround.android.commons.tasks;

import java.util.List;

import us.artaround.android.commons.Database;
import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.os.AsyncTask;
import android.util.Log;

public class SaveArtTask extends AsyncTask<Void, Void, Void> {
	private Database db;
	private List<Art> arts;

	public SaveArtTask(List<Art> art, Database db) {
		this.db = db;
		this.arts = art;
	}

	public void setDatabase(Database database) {
		this.db = database;
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (!db.isOpen()) {
			Log.w(Utils.TAG, "SaveArtTask needs an open database connection!");
			return null;
		}
		db.updateCache(arts);
		return null;
	}
}
