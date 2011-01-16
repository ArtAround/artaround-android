package us.artaround.android.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class FlickrService {
	public static final String FORMAT = "json";
	public static final String REST_URL = "http://api.flickr.com/services/rest/";
	public static final String BASE_PHOTOS_URL = "http://www.flickr.com/photos/";
	public static final String METHOD_GET_PHOTO = "flickr.photos.getSizes";

	public static final String SIZES_KEY = "sizes";
	public static final String SIZE_KEY = "size";
	public static final String LABEL_KEY = "label";
	public static final String SOURCE_KEY = "source";
	public static final String WIDTH_KEY = "width";
	public static final String HEIGHT_KEY = "height";

	public final static String IMAGE_FORMAT = ".jpg";
	public final static String SIZE_SMALL = "Small";
	public final static String SIZE_LARGE = "Large";

	public final static int THUMB_WIDTH = 240;
	public final static int THUMB_HEIGHT = 180;

	// these needs to be set before calling any methods
	public String apiKey;
	public String username;

	private static FlickrService instance;

	private FlickrService(Context context) {
		Resources res = context.getResources();
		setApiKey(res.getString(R.string.flickr_api));
		setUsername(res.getString(R.string.flickr_username));
	}

	public static void init(Context context) {
		if (instance == null) {
			instance = new FlickrService(context);
		}
	}

	public static FlickrService getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException("FlickrService must be initialized with a context!");
		}
		return instance;
	}

	public void setApiKey(String key) {
		apiKey = key;
	}

	public void setUsername(String user) {
		username = user;
	}

	private String getMethodUrl(String method, String apiKey, String photoId) {
		return REST_URL + "?method=" + method + "&api_key=" + apiKey + "&photo_id=" + photoId + "&format=" + FORMAT;
	}

	private String getSizeLetter(String size) throws ArtAroundException {
		if(SIZE_SMALL.equals(size)) {
			return "s";
		}
		if(SIZE_LARGE.equals(size)) {
			return "l";
		}
		throw new ArtAroundException("Size " + size + " not supported!");
	}

	public String getPhotoUrl(String photoId, String size) throws ArtAroundException {
		return BASE_PHOTOS_URL + username + "/" + photoId + "/sizes/" + getSizeLetter(size) + "/";
	}

	public String getPhotoJson(String photoId) throws ArtAroundException {
		String url = getMethodUrl(METHOD_GET_PHOTO, apiKey, photoId);

		HttpGet request = new HttpGet(url);
		request.addHeader("User-Agent", Utils.USER_AGENT);

		DefaultHttpClient client = new DefaultHttpClient();

		try {
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_OK) {
				String body = EntityUtils.toString(response.getEntity());
				return body;
			}
			else {
				throw new ArtAroundException("Bad status code " + statusCode + " on fetching data from Flickr.");
			}
		}
		catch (Exception e) {
			throw new ArtAroundException("Exception on fetching data from Flickr.", e);
		}
	}

	public FlickrPhoto parsePhoto(String json) throws ArtAroundException {
		Log.i(Utils.TAG, "Flickr response is " + json);

		try {
			json = json.replace("jsonFlickrApi(", "");
			json = json.substring(0, json.length() - 1);

			JSONArray arr = new JSONObject(json).getJSONObject(SIZES_KEY).getJSONArray(SIZE_KEY);
			FlickrPhoto photo = new FlickrPhoto();
			photo.sizes = new HashMap<String, FlickrService.FlickrPhotoSize>();

			int length = arr.length();
			for (int i = 0; i < length; i++) {
				JSONObject obj = arr.getJSONObject(i);
				String size = obj.getString(FlickrService.LABEL_KEY);

				if (FlickrService.SIZE_SMALL.equals(size)) {
					FlickrPhotoSize thumbSize = new FlickrPhotoSize();
					thumbSize.url = obj.getString(FlickrService.SOURCE_KEY);
					thumbSize.width = obj.getInt(FlickrService.WIDTH_KEY);
					thumbSize.height = obj.getInt(FlickrService.HEIGHT_KEY);
					photo.sizes.put(FlickrService.SIZE_SMALL, thumbSize);
				}

				if (FlickrService.SIZE_LARGE.equals(size)) {
					FlickrPhotoSize originalSize = new FlickrPhotoSize();
					originalSize.url = obj.getString(FlickrService.SOURCE_KEY);
					originalSize.width = obj.getInt(FlickrService.WIDTH_KEY);
					originalSize.height = obj.getInt(FlickrService.HEIGHT_KEY);
					photo.sizes.put(FlickrService.SIZE_LARGE, originalSize);
				}
			}
			return photo;
		}
		catch (JSONException e) {
			throw new ArtAroundException("Cannot parse json from Flickr", e);
		}
	}

	public static class FlickrPhoto {
		public Map<String, FlickrPhotoSize> sizes;
	}

	public static class FlickrPhotoSize {
		public String size, url;
		public int width, height;
	}
}
