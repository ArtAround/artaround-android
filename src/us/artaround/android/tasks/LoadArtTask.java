package us.artaround.android.tasks;

import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import us.artaround.services.ParseResult;
import android.os.AsyncTask;
import android.util.Log;

public class LoadArtTask extends AsyncTask<Object, Void, ParseResult> {
	private LoadArtCallback callback;
	private Exception lastException;

	public LoadArtTask(LoadArtCallback callback) {
		this.callback = callback;
	}

	public void setCallback(LoadArtCallback callback) {
		this.callback = callback;
	}

	@Override
	protected ParseResult doInBackground(Object... params) {
		try {
			if (params == null || params.length < 2) {
				throw new IllegalArgumentException("LoadArtTask needs 'page' and 'per_page' parameters!");
			}
			Log.d(Utils.TAG, "--> Executing task with params " + params[0] + " " + params[1]);
			return ArtService.getArt((Integer) params[0], (Integer) params[1]);
		} catch (ArtAroundException e) {
			Log.w(Utils.TAG, "LoadArtTask exception", e);
			lastException = e;
			return null;
		} catch (IllegalArgumentException e) {
			Log.w(Utils.TAG, "LoadArtTask needs 'page' and 'per_page' parameters!");
			lastException = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(ParseResult result) {
		if (result != null)
			callback.onLoadArt(result, this);
		else
			callback.onLoadArtError(lastException);
	}

	public static interface LoadArtCallback {
		void onLoadArt(ParseResult result, LoadArtTask task);

		void onLoadArtError(Throwable e);
	}

}
