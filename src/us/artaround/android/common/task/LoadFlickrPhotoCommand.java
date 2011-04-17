package us.artaround.android.common.task;

import us.artaround.android.common.ImageDownloader;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;

public class LoadFlickrPhotoCommand extends ArtAroundAsyncCommand {

	public LoadFlickrPhotoCommand(int token, String id) {
		super(token, id);
	}

	@Override
	public Object execute() throws ArtAroundException {
		FlickrService srv = FlickrService.getInstance();
		FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(id), FlickrService.SIZE_ORIGINAL);

		if (photo != null) {
			return ImageDownloader.getImageDrawable(photo.url);
		}
		return null;
	}

}
