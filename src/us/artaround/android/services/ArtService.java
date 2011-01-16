package us.artaround.android.services;

import java.util.List;

import us.artaround.android.commons.Utils;
import us.artaround.android.parsers.BaseParser;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.parsers.StreamData;
import us.artaround.models.ArtAroundException;
import us.artaround.models.City;
import us.artaround.models.Comment;

public class ArtService extends BaseService {
	private final static String TAG = "ArtAround.ArtService";

	private static final String ENDPOINT_ARTS = "arts";
	private static final String ENDPOINT_CATEGORIES = "categories";
	private final String ENDPOINT_NEIGHBORHOODS = "neighborhoods";

	private final String PARAM_PER_PAGE = "per_page=";
	private final String PARAM_PAGE = "page=";

	private City city;

	public ArtService(City city) {
		this.city = city;
		Utils.d(TAG, "Constructor: using city " + city);
	}

	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

	@Override
	protected void getMethod(StreamData data, String uri) throws ArtAroundException {
		super.getMethod(data, city.serverUrl + uri);
		BaseParser.parseResponse(data);
	}

	public ParseResult getArts(int page, int perPage) throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_ARTS);
		data.setAuxData(false);
		String uri = addParams(ENDPOINT_ARTS, PARAM_PAGE + page, PARAM_PER_PAGE + perPage);
		getMethod(data, uri);
		return (ParseResult) data.getAuxData()[0];
	}

	public void getCategories() throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_CATEGORIES);
		data.setAuxData(false);
		String uri = addParams(ENDPOINT_CATEGORIES);
		getMethod(data, uri);
	}

	public void getNeighborhoods() throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_NEIGHBORHOODS);
		data.setAuxData(false);
		String uri = addParams(ENDPOINT_NEIGHBORHOODS);
		getMethod(data, uri);
	}

	@SuppressWarnings("unchecked")
	public List<Comment> getComments(String artSlug) throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_COMMENTS);
		data.setAuxData(artSlug, true);
		String uri = addParams(ENDPOINT_ARTS + "/" + artSlug);
		getMethod(data, uri);
		return (List<Comment>) data.getAuxData()[0];
	}
}
