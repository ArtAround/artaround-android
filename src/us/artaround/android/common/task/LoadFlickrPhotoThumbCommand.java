package us.artaround.android.common.task;

import us.artaround.android.common.ImageDownloader;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;
import android.os.Bundle;

public class LoadFlickrPhotoThumbCommand extends ArtAroundAsyncCommand {
	private final Bundle args;

	public LoadFlickrPhotoThumbCommand(int token, String id, Bundle args) {
		super(token, id);
		this.args = args;
	}

	@Override
	public Object execute() throws ArtAroundException {
		FlickrService srv = FlickrService.getInstance();
		FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(id), args.getString(ImageDownloader.EXTRA_PHOTO_SIZE));

		if (photo != null) {
			args.putString(ImageDownloader.EXTRA_PHOTO_URL, photo.url);
			return ImageDownloader.getImageUri(args);
		}
		return null;
	}
}
