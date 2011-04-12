package us.artaround.android.common.task;

import us.artaround.android.common.ImageDownloader;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.android.services.FlickrService.FlickrPhotoSize;
import us.artaround.models.ArtAroundException;
import android.os.Bundle;

public class LoadFlickrPhotosCommand extends ArtAroundAsyncCommand {
	private final Bundle args;

	public LoadFlickrPhotosCommand(int token, String id, Bundle args) {
		super(token, id);
		this.args = args;
	}

	@Override
	public Object execute() throws ArtAroundException {
		FlickrService srv = FlickrService.getInstance();
		FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(id));

		String photoSize = args.getString(ImageDownloader.EXTRA_PHOTO_SIZE);
		FlickrPhotoSize size = photo.sizes.get(photoSize);
		if (size != null) {
			args.putString(ImageDownloader.EXTRA_PHOTO_URL, size.url);
			return ImageDownloader.getImage(args);
		}
		return null;
	}
}
