package us.artaround.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Parser {

	public static String prop(JSONObject json, String prop) throws JSONException {
		return json.isNull(prop) ? null : json.getString(prop);
	}

	public static JSONArray propA(JSONObject json, String prop) throws JSONException {
		return json.isNull(prop) ? null : json.getJSONArray(prop);
	}

	public static JSONObject propO(JSONObject json, String prop) throws JSONException {
		return json.isNull(prop) ? null : json.getJSONObject(prop);
	}
}
