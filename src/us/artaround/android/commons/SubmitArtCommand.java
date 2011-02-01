package us.artaround.android.commons;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;

public class SubmitArtCommand extends BackgroundCommand {
	private final Art art;

	public SubmitArtCommand(int token, Art art) {
		super(token);
		this.art = art;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String execute() throws ArtAroundException {
		return ServiceFactory.getArtService().submitArt(art);
	}
}
