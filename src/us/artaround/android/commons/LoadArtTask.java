package us.artaround.android.commons;

import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;
import android.os.AsyncTask;

public class LoadArtTask extends AsyncTask<Void, Void, ParseResult> {
	private LoadArtCallback callback;
	private Exception exception;
	private int perPage, page;

	public LoadArtTask(LoadArtCallback callback, int page, int perPage) {
		this.callback = callback;
		this.perPage = perPage;
		this.page = page;
	}

	public void attach(LoadArtCallback callback) {
		this.callback = callback;
	}

	public void detach() {
		this.callback = null;
	}

	@Override
	public ParseResult doInBackground(Void... params) {
		try {
			Utils.d(Utils.TAG, "Loading art page=" + page);
			return ServiceFactory.getArtService().getArts(page, perPage);
		} catch (ArtAroundException e) {
			Utils.w(Utils.TAG, "LoadArtTask exception!", e);
			exception = e;
			return null;
		}
	}

	@Override
	public void onPostExecute(ParseResult result) {
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
