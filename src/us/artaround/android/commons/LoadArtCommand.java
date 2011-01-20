package us.artaround.android.commons;

import java.util.UUID;

import us.artaround.android.parsers.ParseResult;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadArtCommand extends ServerCallCommand {
	private int page;
	private int perPage;

	public LoadArtCommand(int page, int perPage) {
		super();
		this.token = UUID.randomUUID().hashCode();
		this.page = page;
		this.perPage = perPage;
	}

	@Override
	public int getToken() {
		return token;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ParseResult execute() throws ArtAroundException {
		return ServiceFactory.getArtService().getArts(page, perPage);
	}

}
