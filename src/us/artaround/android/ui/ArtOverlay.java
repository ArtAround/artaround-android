package us.artaround.android.ui;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class ArtOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private OverlayTapListener context;

	public ArtOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		this.doPopulate(); //do populate after constructor so it doen't crash when empty
	}

	public ArtOverlay(Drawable defaultMarker, OverlayTapListener context) {
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

	public void removeOverlay(OverlayItem overay) {
		overlays.remove(overay);
	}

	public void doPopulate() {
		setLastFocusedIndex(-1);
		populate();
	}

	public void doClear() {
		overlays.clear();
	}

	public static interface OverlayTapListener {
		void onTap(OverlayItem item);
	}
}
