package us.artaround.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.text.TextUtils;
import android.util.Log;

public class ArtParser extends Parser {
	public static final String EXC_MSG = "JSON parsing exception";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";

	public static final String PARAM_OBJECT = "arts";

	public static final String PARAM_SLUG = "slug";
	public static final String PARAM_TITLE = "title";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_CREATED_AT = "created_at";
	public static final String PARAM_UPDATED_AT = "updated_at";
	public static final String PARAM_WARD = "ward";
	public static final String PARAM_LOCATION = "location";
	public static final String PARAM_LOCATION_DESC = "location_description";
	public static final String PARAM_NEIGHBORHOOD = "neighborhood";
	public static final String PARAM_ARTIST = "artist";
	public static final String PARAM_PHOTOS = "flickr_ids";

	public static Art parse(JSONObject json) throws JSONException, ParseException {
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		
		Art art = new Art();
		art.slug = prop(json, PARAM_SLUG);
		art.category = prop(json, PARAM_CATEGORY);
		art.title = prop(json, PARAM_TITLE);
		art.createdAt = df.parse((prop(json, PARAM_CREATED_AT)));
		art.updatedAt = df.parse(prop(json, PARAM_UPDATED_AT));

		String ward = prop(json, PARAM_WARD);
		if (TextUtils.isEmpty(ward)) {
			art.ward = 0;
		} else {
			art.ward = Integer.parseInt(ward);
		}

		JSONArray arr = propA(json, PARAM_LOCATION);
		art.latitude = (float) arr.optDouble(0);
		art.longitude = (float) arr.optDouble(1);

		art.locationDesc = prop(json, PARAM_LOCATION_DESC);
		art.neighborhood = prop(json, PARAM_NEIGHBORHOOD);
		
		arr = propA(json, PARAM_PHOTOS);
		String[] photoIds = new String[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			photoIds[i] = arr.getString(i);
		}
		art.photoIds = photoIds;
		
		// art.artist = ArtistParser.parse(propO(json, PARAM_ARTIST));
		art.artist = prop(json, PARAM_ARTIST);
		return art;
	}

	public static Art parse(String json) throws ArtAroundException {
		try {
			Log.i(Utils.TAG, "ArtParser received JSON: \n" + json);
			return parse(new JSONObject(json));
		} catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		} catch (ParseException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
	}

	public static ArrayList<Art> parseA(String json) throws ArtAroundException {
		Log.i(Utils.TAG, "ArtParser received JSON: \n" + json);

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
		} catch (ParseException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
		return results;
	}
}
