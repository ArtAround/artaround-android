package us.artaround.android.common.task;

import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadArtCommand extends ArtAroundAsyncCommand {
	private final int page;
	private final int perPage;

	public LoadArtCommand(int token, String id, int page, int perPage) {
		super(token, id);
		this.page = page;
		this.perPage = perPage;
	}

	@Override
	public ParseResult execute() throws ArtAroundException {
		return ServiceFactory.getArtService().getArts(page, perPage);
	}
}
