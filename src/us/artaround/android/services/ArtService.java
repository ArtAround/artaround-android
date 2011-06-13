package us.artaround.android.services;

import java.io.File;
import java.util.List;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import us.artaround.android.common.Utils;
import us.artaround.android.parsers.BaseParser;
import us.artaround.android.parsers.ParseResult;
import us.artaround.android.parsers.StreamData;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.City;
import us.artaround.models.Comment;

public class ArtService extends BaseService {
	private final static String TAG = "ArtAround.ArtService";

	private static final String ENDPOINT_ARTS = "arts";
	private static final String ENDPOINT_CATEGORIES = "categories";
	private static final String ENDPOINT_NEIGHBORHOODS = "neighborhoods";
	private static final String ENDPOINT_COMMENTS = "/comments";

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
	protected void getMethod(StreamData data, String url) throws ArtAroundException {
		super.getMethod(data, city.serverUrl + url);
		BaseParser.parseResponse(data);
	}

	@Override
	protected void postMethod(StreamData data, String url, MultipartEntity entity) throws ArtAroundException {
		super.postMethod(data, city.serverUrl + url, entity);
		BaseParser.parseResponse(data);
	}

	@Override
	protected void postMethod(StreamData data, String url, String body) throws ArtAroundException {
		super.postMethod(data, city.serverUrl + url, body);
		BaseParser.parseResponse(data);
	}

	@Override
	protected void putMethod(StreamData data, String url, String body) throws ArtAroundException {
		super.putMethod(data, city.serverUrl + url, body);
		BaseParser.parseResponse(data);
	}

	public ParseResult getArts(int page, int perPage) throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_ARTS);
		data.setAuxData(true);
		String uri = addParams(ENDPOINT_ARTS + FORMAT, PARAM_PAGE + page, PARAM_PER_PAGE + perPage);
		getMethod(data, uri);
		return (ParseResult) data.getAuxData()[0];
	}

	public void getCategories() throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_CATEGORIES);
		data.setAuxData(true);
		String uri = addParams(ENDPOINT_CATEGORIES + FORMAT);
		getMethod(data, uri);
	}

	public void getNeighborhoods() throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_NEIGHBORHOODS);
		data.setAuxData(true);
		String uri = addParams(ENDPOINT_NEIGHBORHOODS + FORMAT);
		getMethod(data, uri);
	}

	@SuppressWarnings("unchecked")
	public List<Comment> getComments(String artSlug) throws ArtAroundException {
		StreamData data = new StreamData(BaseParser.TYPE_COMMENTS);
		data.setAuxData(artSlug, true);
		String uri = addParams(ENDPOINT_ARTS + "/" + artSlug + FORMAT);
		getMethod(data, uri);
		return (List<Comment>) data.getAuxData()[0];
	}

	public void uploadPhoto(String artSlug, String filePath) throws ArtAroundException {
		try {
			MultipartEntity multiEntity = new MultipartEntity(HttpMultipartMode.STRICT);
			File file = new File(filePath);
			FileBody fBody = new FileBody(file, "jpg");
			multiEntity.addPart("file", fBody);
			StreamData data = new StreamData(BaseParser.TYPE_NONE);
			data.setAuxData(artSlug, true);
			postMethod(data, ENDPOINT_ARTS + "/" + artSlug + "/photos" + FORMAT, multiEntity);
		}
		catch (VerifyError e) {
			throw new ArtAroundException(e);
		}
	}

	public String submitArt(Art art) throws ArtAroundException {
		String json = BaseParser.writeArt(art);
		Utils.d(Utils.TAG, "Sending new art json " + json);

		StreamData data = new StreamData(BaseParser.TYPE_RESPONSE);
		postMethod(data, ENDPOINT_ARTS + FORMAT, json);
		Object[] obj = data.getAuxData();
		if (obj != null && obj.length > 0) {
			return (String) obj[0];
		}
		return null;
	}

	public String editArt(Art art) throws ArtAroundException {
		String json = BaseParser.writeArt(art);
		Utils.d(Utils.TAG, "Sending new art json " + json);

		StreamData data = new StreamData(BaseParser.TYPE_RESPONSE);
		putMethod(data, ENDPOINT_ARTS + "/" + art.slug + FORMAT, json);
		Object[] obj = data.getAuxData();
		if (obj != null && obj.length > 0) {
			return (String) obj[0];
		}
		return null;
	}

	public Boolean submitComment(String artSlug, Comment comment) throws ArtAroundException {
		String json = BaseParser.writeComment(comment);
		Utils.d(Utils.TAG, "Sending new comment json " + json);

		StreamData data = new StreamData(BaseParser.TYPE_RESPONSE);
		postMethod(data, ENDPOINT_ARTS + "/" + artSlug + ENDPOINT_COMMENTS + FORMAT, json);
		Object[] obj = data.getAuxData();
		if (obj != null && obj.length > 0) {
			return Boolean.valueOf((String) obj[0]);
		}
		return null;
	}
} 
