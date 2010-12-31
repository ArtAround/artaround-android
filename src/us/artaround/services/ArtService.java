package us.artaround.services;

import java.util.List;

import us.artaround.models.ArtAroundException;
import android.content.Context;

public class ArtService extends BaseService {
	private static final String METHOD_GET_ARTS = "arts";
	private static final String METHOD_GET_CATEGORIES = "categories";
	private static final String METHOD_GET_NEIGHBORHOODS = "neighborhoods";

	private static final String PARAM_PER_PAGE = "per_page";
	private static final String PARAM_PAGE = "page";

	public static ParseResult getArts(int page, int perPage) throws ArtAroundException {
		if (page == 1) {
			TempCache.clear();
		}

		StringBuilder query = new StringBuilder()
			.append(PARAM_PER_PAGE).append("=").append(perPage)
			.append("&").append(PARAM_PAGE).append("=").append(page);
		
		return ArtParser.parseArts(getMethod(formUrl(METHOD_GET_ARTS, query.toString())));
	}

	public static List<String> getCategories() throws ArtAroundException {
		return ArtParser.parseStringArray(getMethod(formUrl(METHOD_GET_CATEGORIES, null)));
	}

	public static List<String> getNeighborhoods() throws ArtAroundException {
		return ArtParser.parseStringArray(getMethod(formUrl(METHOD_GET_NEIGHBORHOODS, null)));
	}

	public static void init(Context context) {
		Flickr.setup(context);
	}

	public static String getPhotoUrl(String photoId, String size) throws ArtAroundException {
		return Flickr.parsePhoto(Flickr.getPhotoJson(photoId)).sizes.get(size).url;
	}

	public static String getImageName(String id) {
		return id + Flickr.IMAGE_FORMAT;
	}

	public static String getImageSmallSize() {
		return Flickr.SIZE_SMALL;
	}
}
