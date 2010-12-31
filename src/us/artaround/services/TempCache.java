package us.artaround.services;

import java.util.HashMap;
import java.util.Map;

import us.artaround.models.Artist;

public class TempCache {
	public static final Map<String, Artist> artists = new HashMap<String, Artist>();

	public static void clear() {
		artists.clear();
	}
}
