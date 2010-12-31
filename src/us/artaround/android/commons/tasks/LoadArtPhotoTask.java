package us.artaround.android.commons.tasks;

import us.artaround.android.commons.ImageDownloader;
import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import us.artaround.services.ArtService;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class LoadArtPhotoTask extends AsyncTask<String, Void, Uri> {
	private LoadPhotoCallback callback;
	private String id;

	public LoadArtPhotoTask(LoadPhotoCallback callback, String id) {
		this.callback = callback;
		this.id = id;
	}

	public void attach(LoadPhotoCallback callback) {
		this.callback = callback;
	}

	public void detach() {
		this.callback = null;
	}

	@Override
	protected Uri doInBackground(String... params) {
		try {
			if (params == null || params.length < 1) {
				Log.e(Utils.TAG, "LoadPhotoTask must receive the 'size' param!");
				return null;
			}
			String size = params[0];
			String url = ArtService.getPhotoUrl(id, size);
			String name = ArtService.getImageName(id);

			return ImageDownloader.getImage((Context) callback, name, size, url);
		}
		catch (ArtAroundException e) {
			Log.e(Utils.TAG, "Could not get Flickr image with id " + id, e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Uri photoUri) {
		if (photoUri != null && callback != null) {
			callback.onLoadPhoto(id, photoUri, this);
		}
	}

	public static interface LoadPhotoCallback {
		void onLoadPhoto(String id, Uri photoUri, LoadArtPhotoTask task);
	}
}
