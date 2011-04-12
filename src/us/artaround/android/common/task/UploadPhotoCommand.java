package us.artaround.android.common.task;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class UploadPhotoCommand extends ArtAroundAsyncCommand {
	public final static int token = 200;

	private final String artSlug;
	private final String filePath;

	public UploadPhotoCommand(int token, String id, String artSlug, String filePath) {
		super(token, id);
		this.artSlug = artSlug;
		this.filePath = filePath;
	}

	@Override
	public Void execute() throws ArtAroundException {
		ServiceFactory.getArtService().uploadPhoto(artSlug, filePath);
		return null;
	}
}
