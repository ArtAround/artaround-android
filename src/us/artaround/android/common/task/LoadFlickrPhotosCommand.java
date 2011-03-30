package us.artaround.android.common.task;

import us.artaround.android.common.ImageDownloader;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;
import android.net.Uri;

public class LoadFlickrPhotosCommand extends ArtAroundAsyncCommand {

	public LoadFlickrPhotosCommand(int token, String id) {
		super(token, id);
	}

	@Override
	public Object execute() throws ArtAroundException {
		FlickrService srv = FlickrService.getInstance();
		FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(id));
		String name = id + FlickrService.IMAGE_FORMAT;
		Uri[] uris = new Uri[2];

		uris[0] = ImageDownloader.getImage(name, FlickrService.SIZE_MEDIUM,
				photo.sizes.get(FlickrService.SIZE_MEDIUM).url);

		uris[1] = ImageDownloader.getImage(name, FlickrService.SIZE_ORIGINAL,
				photo.sizes.get(FlickrService.SIZE_ORIGINAL).url);

		return uris;
	}

}
