package us.artaround.services;

import java.util.HashMap;
import java.util.Map;

import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Artist;

public class ArtService extends BaseService {
	private static final String METHOD_GET_ART = "arts";

	private static final String PARAM_PER_PAGE = "per_page";
	private static final String PARAM_PAGE = "page";

	public static Map<String, Artist> artists = new HashMap<String, Artist>();

	public static ParseResult getArt(int perPage, int page) throws ArtAroundException {
		artists.clear();

		StringBuilder query = new StringBuilder()
			.append(PARAM_PER_PAGE).append("=").append(perPage)
			.append("&").append(PARAM_PAGE).append("=").append(page);
		
		return ArtParser.parseArt(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}

	public static Art getArt(String slug) throws ArtAroundException {
		StringBuilder query = new StringBuilder(ArtParser.PARAM_SLUG).append(slug);
		return ArtParser.parse(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}
}
