package us.artaround.android.commons.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import android.os.AsyncTask;
import android.util.Log;

public class LoadDataTask extends AsyncTask<Void, Void, Map<Integer, List<String>>> {

	public static final int TYPE_CATEGORIES = 0;
	public static final int TYPE_NEIGHBORHOODS = 1;

	public static interface LoadDataCallback {
		void onLoadData(Map<Integer, List<String>> data);
	}

	private LoadDataCallback callback;

	public LoadDataTask(LoadDataCallback callback) {
		this.callback = callback;
	}

	public void attach(LoadDataCallback callback) {
		this.callback = callback;
	}

	public void detach() {
		this.callback = null;
	}

	@Override
	protected Map<Integer, List<String>> doInBackground(Void... params) {
		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
		try {
			map.put(TYPE_CATEGORIES, ArtService.getCategories());
			map.put(TYPE_NEIGHBORHOODS, ArtService.getNeighborhoods());
		}
		catch (ArtAroundException e) {
			Log.w(Utils.TAG, "Could not load categories and neighborhoods", e);
		}

		return map;
	}

	@Override
	protected void onPostExecute(Map<Integer, List<String>> result) {
		if (callback != null) {
			callback.onLoadData(result);
		}
	}
}
