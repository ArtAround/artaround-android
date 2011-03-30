package us.artaround.android.common;

import java.util.UUID;

import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadArtCommand extends BackgroundCommand {
	private final int page;
	private final int perPage;

	public LoadArtCommand(int page, int perPage) {
		super(0, UUID.randomUUID().toString());
		this.page = page;
		this.perPage = perPage;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ParseResult execute() throws ArtAroundException {
		return ServiceFactory.getArtService().getArts(page, perPage);
	}
}
