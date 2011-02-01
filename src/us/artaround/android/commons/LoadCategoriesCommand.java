package us.artaround.android.commons;

import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;

public class LoadCategoriesCommand extends BackgroundCommand {

	public LoadCategoriesCommand(int token) {
		super(token);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void execute() throws ArtAroundException {
		ServiceFactory.getArtService().getCategories();
		return null;
	}

}
