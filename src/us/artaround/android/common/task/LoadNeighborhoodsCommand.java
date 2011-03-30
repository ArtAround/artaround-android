package us.artaround.android.common.task;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadNeighborhoodsCommand extends ArtAroundAsyncCommand {

	public LoadNeighborhoodsCommand(int token, String id) {
		super(token, id);
	}

	@Override
	public Object execute() throws ArtAroundException {
		ServiceFactory.getArtService().getNeighborhoods();
		return null;
	}
}
