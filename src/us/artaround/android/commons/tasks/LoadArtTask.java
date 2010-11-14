package us.artaround.android.commons.tasks;

import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import us.artaround.services.ParseResult;
import android.os.AsyncTask;
import android.util.Log;

public class LoadArtTask extends AsyncTask<Void, Void, ParseResult> {
	private LoadArtCallback callback;
	private Exception exception;
	private int perPage, page;

	public LoadArtTask(LoadArtCallback callback, int page, int perPage) {
		this.callback = callback;
		this.perPage = perPage;
		this.page = page;
	}

	public void setCallback(LoadArtCallback callback) {
		this.callback = callback;
	}

	@Override
	protected ParseResult doInBackground(Void... params) {
		try {
			Log.d(Utils.TAG, "Running LoadArtTask with page= " + page + " and perPage= " + perPage);
			return ArtService.getArt(page, perPage);
		} catch (ArtAroundException e) {
			Log.e(Utils.TAG, "LoadArtTask exception!", e);
			exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(ParseResult result) {
		if (result != null)
			callback.onLoadArt(result, this);
		else
			callback.onLoadArtError(exception);
	}

	public int getPerPage() {
		return perPage;
	}

	public int getPage() {
		return page;
	}

	public static interface LoadArtCallback {
		void onLoadArt(ParseResult result, LoadArtTask task);

		void onLoadArtError(Throwable e);
	}

}
