package us.artaround.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;

public class ArtParser extends Parser {
	public static final String EXC_MSG = "JSON parsing exception";

	public static final String PARAM_ARTS = "arts";
	public static final String PARAM_TOTAL_COUNT = "total_count";
	public static final String PARAM_PER_PAGE = "per_page";
	public static final String PARAM_PAGE = "page";
	public static final String PARAM_COUNT = "count";

	public static final String PARAM_ID = "slug";
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

	public static int parseTotalCount(JSONObject json) throws JSONException {
		return json.getInt(PARAM_TOTAL_COUNT);
	}

	private static int[] parsePage(JSONObject json) throws JSONException {
		int results[] = new int[3];
		results[0] = json.getInt(PARAM_COUNT);
		results[1] = json.getInt(PARAM_PAGE);
		results[2] = json.getInt(PARAM_PER_PAGE);
		return results;
	}

	public static Art parse(JSONObject json) throws JSONException, ParseException {
		Art art = new Art();
		art.id = prop(json, PARAM_ID);
		art.category = prop(json, PARAM_CATEGORY);
		art.title = prop(json, PARAM_TITLE);
		art.createdAt = Utils.parseDate(prop(json, PARAM_CREATED_AT));
		art.updatedAt = Utils.parseDate(prop(json, PARAM_UPDATED_AT));
		art.ward = json.optInt(PARAM_WARD, 0);
		art.locationDesc = prop(json, PARAM_LOCATION_DESC);
		art.neighborhood = prop(json, PARAM_NEIGHBORHOOD);

		JSONArray arr = propA(json, PARAM_LOCATION);
		art.latitude = (float) arr.optDouble(0);
		art.longitude = (float) arr.optDouble(1);

		arr = propA(json, PARAM_PHOTOS);
		if (arr != null) {
			String[] photoIds = new String[arr.length()];
			for (int i = 0; i < arr.length(); i++) {
				photoIds[i] = arr.getString(i);
			}
			art.photoIds = photoIds;
		}
		
		String artistName = prop(json, PARAM_ARTIST);
		art.artist = ArtistParser.parseArtist(artistName);

		return art;
	}

	public static Art parse(String json) throws ArtAroundException {
		try {
			//Log.d(Utils.TAG, "ArtParser received JSON: \n" + json);
			return parse(new JSONObject(json));
		} catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		} catch (ParseException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
	}

	public static ParseResult parseArts(String json) throws ArtAroundException {
		//Log.d(Utils.TAG, "ArtParser received JSON: \n" + json);
		ParseResult result = new ParseResult();

		try {
			JSONObject obj = new JSONObject(json);

			int totalCount = parseTotalCount(obj);

			int[] pageInfo = parsePage(obj.getJSONObject(PARAM_PAGE));

			JSONArray arr = obj.getJSONArray(PARAM_ARTS);
			List<Art> art = new ArrayList<Art>();
			for (int i = 0; i < arr.length(); i++) {
				art.add(parse(arr.getJSONObject(i)));
			}

			result.art = art;
			result.totalCount = totalCount;
			result.count = pageInfo[0];
			result.page = pageInfo[1];
			result.perPage = pageInfo[2];

			//Log.d(Utils.TAG, "ParseResult art: " + result.art);

		} catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		} catch (RuntimeException e) {
			throw new ArtAroundException(EXC_MSG, e);
		} catch (ParseException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
		return result;
	}

	public static List<String> parseStringArray(String json) throws ArtAroundException {
		try {
			List<String> result = new ArrayList<String>();
			JSONArray obj = new JSONArray(json);
			int size = obj.length();
			for (int i = 0; i < size; i++) {
				result.add(obj.getString(i));
			}
			return result;
		}
		catch (JSONException e) {
			throw new ArtAroundException(EXC_MSG, e);
		}
	}
}
