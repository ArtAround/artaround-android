package us.artaround.android.commons;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadCategoriesCommand extends ServerCallCommand {
	public static final int token = "LoadCategoriesCommand".hashCode();

	public LoadCategoriesCommand() {
		super(token);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void execute() throws ArtAroundException {
		ServiceFactory.getArtService().getCategories();
		return null;
	}

}
