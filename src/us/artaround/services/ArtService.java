package us.artaround.services;

import java.util.ArrayList;

import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;

public class ArtService extends BaseService {
	private static final String METHOD_GET_ART = "arts";

	private static final String PARAM_LIMIT = "per_page";
	private static final String PARAM_PAGE = "page";
	private static final String PARAM_ORDER = "order";

	private static final String DEFAULT_ORDER = ArtParser.PARAM_CREATED_AT;

	public static ArrayList<Art> getArt(int limit, int page)
			throws ArtAroundException {
		StringBuilder query = new StringBuilder()
			.append(PARAM_LIMIT).append("=").append(limit)
			.append("&").append(PARAM_PAGE).append("=").append(page)
			.append("&").append(PARAM_ORDER).append("=").append(DEFAULT_ORDER);
		
		return ArtParser.parseA(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}

	public static Art getArt(String slug) throws ArtAroundException {
		StringBuilder query = new StringBuilder(ArtParser.PARAM_SLUG).append(slug);
		return ArtParser.parse(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}
}
