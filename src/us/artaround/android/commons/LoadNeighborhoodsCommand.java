package us.artaround.android.commons;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadNeighborhoodsCommand extends ServerCallCommand {
	public static final int token = "LoadNeighborhoodsCommand".hashCode();

	public LoadNeighborhoodsCommand() {
		super(token);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void execute() throws ArtAroundException {
		ServiceFactory.getArtService().getNeighborhoods();
		return null;
	}
}
