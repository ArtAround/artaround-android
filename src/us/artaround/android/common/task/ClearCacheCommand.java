package us.artaround.android.common.task;

import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.models.ArtAroundException;

public class ClearCacheCommand extends ArtAroundAsyncCommand {

	public ClearCacheCommand(int token, String id) {
		super(token, id);
	}

	@Override
	public Object execute() throws ArtAroundException {
		ArtAroundProvider.contentResolver.delete(Arts.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Artists.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Categories.CONTENT_URI, null, null);
		ArtAroundProvider.contentResolver.delete(Neighborhoods.CONTENT_URI, null, null);
		return null;
	}

}
