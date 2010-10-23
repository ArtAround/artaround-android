package us.artaround.android.services;

import org.json.JSONException;
import org.json.JSONObject;

import us.artaround.android.models.Artist;

public class ArtistParser extends Parser {

	public static final String PARAM_NAME = "name";

	public static Artist parse(JSONObject json) throws JSONException {
		Artist artist = new Artist();
		artist.name = prop(json, PARAM_NAME);
		return artist;
	}

}
