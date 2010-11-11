package us.artaround.android.commons.tasks;

import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import us.artaround.services.ParseResult;
import android.os.AsyncTask;
import android.util.Log;

public class LoadArtTask extends AsyncTask<Object, Void, ParseResult> {
	private LoadArtCallback callback;

	public LoadArtTask(LoadArtCallback callback) {
		this.callback = callback;
	}

	public void setCallback(LoadArtCallback callback) {
		this.callback = callback;
	}

	@Override
	protected ParseResult doInBackground(Object... params) {
		if (params == null || params.length < 2) {
			Log.w(Utils.TAG, "LoadArtTask needs 'page' and 'per_page' parameters!");
			return null;
		}
		try {
			Log.d(Utils.TAG, "--> Executing task with params " + params[0] + " " + params[1]);
			return ArtService.getArt((Integer) params[0], (Integer) params[1]);
		} catch (ArtAroundException e) {
			Log.w(Utils.TAG, "LoadArtTask exception", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ParseResult result) {
		callback.onLoadArt(result, this);
	}

	public static interface LoadArtCallback {
		void onLoadArt(ParseResult result, LoadArtTask task);
	}

}
