package us.artaround.android.ui;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;

import com.google.android.maps.OverlayItem;

public class ArtOverlayItem extends OverlayItem {

	public final Art art;

	public ArtOverlayItem(Art a) {
		super(Utils.geo(a.latitude, a.longitude), a.title, a.locationDesc);
		this.art = a;
	}

}
