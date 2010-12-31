package us.artaround.services;

import us.artaround.models.Artist;
import android.text.TextUtils;

public class ArtistParser {

	public static Artist parseArtist(String artistName) {
		if (!TextUtils.isEmpty(artistName)) {
			if (TempCache.artists.containsKey(artistName)) {
				return TempCache.artists.get(artistName);
			}
			else {
				Artist artist = new Artist(artistName);
				TempCache.artists.put(artistName, artist);
				return artist;
			}
		}
		return null;
	}
}
