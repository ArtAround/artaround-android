package us.artaround.android.services;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import us.artaround.android.models.Art;
import us.artaround.android.models.ArtAroundException;

public class ArtParser extends Parser {
	public static final String EXC_MSG = "JSON parsing exception";

	public static final String PARAM_OBJECT = "art";

	public static final String PARAM_ID = "_id";
	public static final String PARAM_SLUG = "slug";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_TITLE = "title";
	public static final String PARAM_CREATED_AT = "created_at";
	public static final String PARAM_UPDATED_AT = "updated_at";
	public static final String PARAM_PITCH = "pitch";
	public static final String PARAM_ZOOM = "zoom";
	public static final String PARAM_WARD = "ward";
	public static final String PARAM_APPROVED = "approved";
	public static final String PARAM_LATITUDE = "latitude";
	public static final String PARAM_LONGITUDE = "longitude";
	public static final String PARAM_LOCATION = "location";
	public static final String PARAM_LOCATION_DESC = "location_description";
	public static final String PARAM_NEIGHBORHOOD = "neighborhood";
	public static final String PARAM_ARTIST = "artist";
	public static final String PARAM_PHOTOS = "flickr_ids";



	public static Art parse(JSONObject json) throws JSONException {
		Art art = new Art();

		art.id = prop(json, PARAM_ID);
		art.slug = prop(json, PARAM_SLUG);
		art.category = prop(json, PARAM_CATEGORY);
		art.title = prop(json, PARAM_TITLE);
		art.createdAt = Long.parseLong(prop(json, PARAM_CREATED_AT));
		art.updatedAt = Long.parseLong(prop(json, PARAM_UPDATED_AT));
		art.pitch = Float.parseFloat(prop(json, PARAM_PITCH));
		art.zoom = Integer.parseInt(prop(json, PARAM_ZOOM));
		art.ward = Integer.parseInt(prop(json, PARAM_WARD));
		art.approved = Boolean.parseBoolean(prop(json, PARAM_APPROVED));
		
		JSONArray arr = propA(json, PARAM_LOCATION);
		art.latitude = arr.optLong(0);
		art.longitude = arr.optLong(1);

		art.locationDesc = prop(json, PARAM_LOCATION_DESC);
		art.neighborhood = prop(json, PARAM_NEIGHBORHOOD);
		
		arr = propA(json, PARAM_PHOTOS);
		String[] photoIds = new String[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			photoIds[i] = arr.getString(i);
		}
		art.photoIds = photoIds;
		
		art.artist = ArtistParser.parse(propO(json, PARAM_ARTIST));
		return art;
	}

	public static Art parse(String json) throws ArtAroundException {
		try {
			return parse(new JSONObject(json));
		} catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
	}

	public static ArrayList<Art> parseA(String json) throws ArtAroundException {
		ArrayList<Art> results = new ArrayList<Art>();
		try {
			JSONArray arr = new JSONObject(json).getJSONArray(PARAM_OBJECT);
			for (int i = 0; i < arr.length(); i++) {
				results.add(parse(arr.getJSONObject(i)));
			}
		} catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		} catch (RuntimeException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
		return results;
	}
}
