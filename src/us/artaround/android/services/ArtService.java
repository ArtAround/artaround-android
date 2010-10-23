package us.artaround.android.services;

import java.util.ArrayList;

import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;

public class ArtService extends BaseService {

	private static final String METHOD_GET_ART = "art";

	private static final String PARAM_LIMIT = "limit";
	private static final String PARAM_PAGE = "page";
	private static final String PARAM_ORDER = "order";

	private static final String DEFAULT_ORDER = ArtParser.PARAM_CREATED_AT;

	public static ArrayList<Art> getArt(long latitude, long longitude, int limit, int page)
			throws ArtAroundException {
		StringBuilder query = new StringBuilder(ArtParser.PARAM_LATITUDE).append(latitude)
				.append("&").append(ArtParser.PARAM_LONGITUDE).append(longitude)
				.append("&").append(PARAM_LIMIT).append(limit)
				.append("&").append(PARAM_PAGE).append(page)
				.append("&").append(PARAM_ORDER).append(DEFAULT_ORDER);
		
		return ArtParser.parseA(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}

	public static Art getArt(String id) throws ArtAroundException {
		StringBuilder query = new StringBuilder(ArtParser.PARAM_ID).append(id);
		return ArtParser.parse(getMethod(formUrl(METHOD_GET_ART, query.toString())));
	}

	
}
