package us.artaround.android.common.task;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;

public class SubmitArtCommand extends ArtAroundAsyncCommand {
	private final Art art;

	public SubmitArtCommand(int token, String id, Art art) {
		super(token, id);
		this.art = art;
	}

	@Override
	public String execute() throws ArtAroundException {
		return ServiceFactory.getArtService().submitArt(art);
	}
}
