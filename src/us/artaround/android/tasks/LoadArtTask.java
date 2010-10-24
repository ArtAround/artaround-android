package us.artaround.android.tasks;

import java.util.ArrayList;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import android.os.AsyncTask;
import android.util.Log;

public class LoadArtTask extends AsyncTask<Object, Void, ArrayList<Art>> {
	private LoadArtCallback callback;

	public LoadArtTask(LoadArtCallback callback) {
		this.callback = callback;
	}

	public void setCallback(LoadArtCallback callback) {
		this.callback = callback;
	}

	@Override
	protected ArrayList<Art> doInBackground(Object... params) {
		if (params == null || params.length < 2) {
			Log.w(Utils.TAG, "LoadArtTask needs limit and page parameters!");
			return null;
		}
		try {
			return ArtService.getArt((Integer) params[0], (Integer) params[1]);
		} catch (ArtAroundException e) {
			Log.w(Utils.TAG, "LoadArtTask exception", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<Art> result) {
		callback.onLoadArt(result, this);
	}

	public static interface LoadArtCallback {
		void onLoadArt(ArrayList<Art> art, LoadArtTask task);
	}

}
