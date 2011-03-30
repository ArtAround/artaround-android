package us.artaround.android.common;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class UploadPhotoCommand extends BackgroundCommand {
	public final static int token = 200;

	private final String artSlug;
	private final String filePath;

	public UploadPhotoCommand(int token, String id, String artSlug, String filePath) {
		super(token, id);
		this.artSlug = artSlug;
		this.filePath = filePath;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void execute() throws ArtAroundException {
		ServiceFactory.getArtService().uploadPhoto(artSlug, filePath);
		return null;
	}
}
