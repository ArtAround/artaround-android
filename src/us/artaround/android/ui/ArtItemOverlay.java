package us.artaround.android.ui;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class ArtItemOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private OverlayTapListener context;

	public ArtItemOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public ArtItemOverlay(Drawable defaultMarker, OverlayTapListener context) {
		this(defaultMarker);
		this.context = context;
	}

	@Override
	protected OverlayItem createItem(int i) {
		return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		context.onTap(overlays.get(index));
		return true;
	}

	public void addOverlay(OverlayItem overlay) {
		overlays.add(overlay);
	}

	public void doPopulate() {
		populate();
	}

	public static interface OverlayTapListener {
		void onTap(OverlayItem item);
	}
}
