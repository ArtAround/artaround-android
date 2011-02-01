package us.artaround.android.ui;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.text.TextUtils;

import com.google.android.maps.OverlayItem;

public class ArtOverlayItem extends OverlayItem {
	public final Art art;

	public ArtOverlayItem(Art art) {
		super(Utils.geo(art.latitude, art.longitude), art.title, buildDesc(art).toString());
		this.art = art;
	}

	//TODO translation
	private static String buildDesc(Art art) {
		StringBuilder bld = new StringBuilder();
		if (!TextUtils.isEmpty(art.locationDesc)) {
			bld.append(art.locationDesc);
		}
		if (!TextUtils.isEmpty(art.category)) {
			bld.append(Utils.NL).append("Category: " + art.category);
		}
		if (!TextUtils.isEmpty(art.neighborhood)) {
			bld.append(Utils.NL).append("Neighborhood: " + art.neighborhood);
		}
		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) {
			bld.append(Utils.NL).append("Artist: " + art.artist.name);
		}
		return bld.toString();
	}

}
