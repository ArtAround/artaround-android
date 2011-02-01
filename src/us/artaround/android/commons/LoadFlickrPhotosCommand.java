package us.artaround.android.commons;

import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;
import android.net.Uri;

public class LoadFlickrPhotosCommand extends BackgroundCommand {
	private final String size;
	private final String photoId;

	public LoadFlickrPhotosCommand(int token, String photoId, String size) {
		super(token);
		this.size = size;
		this.photoId = photoId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Uri execute() throws ArtAroundException {
		FlickrService srv = FlickrService.getInstance();
		FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(photoId));
		String name = photoId + FlickrService.IMAGE_FORMAT;
		return ImageDownloader.getImage(name, size, photo.sizes.get(FlickrService.SIZE_MEDIUM).url);
	}
}
