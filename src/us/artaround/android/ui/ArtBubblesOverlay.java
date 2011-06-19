package us.artaround.android.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.Utils;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ArtBubblesOverlay extends ItemizedOverlay<ArtOverlayItem> {
	private final ArrayList<ArtOverlayItem> overlays = new ArrayList<ArtOverlayItem>();
	private OverlayTapListener context;

	private final MapView mapView;
	private final MapController mapController;

	private ArtBubble bubbleView;
	private View clickRegion;
	private int viewOffset;

	public ArtBubblesOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));

		this.mapView = mapView;
		//viewOffset = 0;
		setBubbleBottomOffset(defaultMarker.getIntrinsicHeight());
		mapController = mapView.getController();

		this.doPopulate(); //do populate after constructor so it doen't crash when empty
	}

	public ArtBubblesOverlay(Drawable defaultMarker, OverlayTapListener context, MapView mapView) {
		this(defaultMarker, mapView);
		this.context = context;
	}

	@Override
	protected ArtOverlayItem createItem(int i) {
		return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

	public void addOverlay(ArtOverlayItem overlay) {
		overlays.add(overlay);
	}

	public void addOverlay(Collection<OverlayItem> overlays) {
		overlays.addAll(overlays);
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

	public void setBubbleBottomOffset(int pixels) {
		viewOffset = pixels;
	}

	protected boolean onBubbleTap(int index) {
		context.onTap(overlays.get(index));
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.android.maps.ItemizedOverlay#onTap(int)
	 */
	@Override
	protected final boolean onTap(int index) {
		boolean recycled;
		final int thisIndex;
		GeoPoint point;

		thisIndex = index;
		point = createItem(index).getPoint();

		if (bubbleView == null) {
			bubbleView = new ArtBubble(mapView.getContext(), viewOffset);
			clickRegion = bubbleView.findViewById(R.id.bubble_inner);
			recycled = false;
		}
		else {
			recycled = true;
		}
		bubbleView.setVisibility(View.GONE);

		List<Overlay> mapOverlays = mapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBubbles(mapOverlays);
		}

		bubbleView.clearData();
		bubbleView.setData(createItem(index));

		MapView.LayoutParams params = new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				point, MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;

		setBubbleTouchListener(thisIndex);

		bubbleView.setVisibility(View.VISIBLE);

		if (recycled) {
			bubbleView.setLayoutParams(params);
		}
		else {
			mapView.addView(bubbleView, params);
		}

		mapController.animateTo(point);
		return true;
	}

	public boolean hideBubble() {
		if (bubbleView != null && bubbleView.getVisibility() == View.VISIBLE) {
			bubbleView.setVisibility(View.GONE);
			return true;
		}
		return false;
	}

	private void hideOtherBubbles(List<Overlay> overlays) {
		for (Overlay overlay : overlays) {
			if (overlay instanceof ArtBubblesOverlay && overlay != this) {
				((ArtBubblesOverlay) overlay).hideBubble();
			}
		}
	}

	private void setBubbleTouchListener(final int thisIndex) {

		try {
			@SuppressWarnings("unused")
			Method m = this.getClass().getDeclaredMethod("onBubbleTap", int.class);

			clickRegion.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {

					View view = ((View) v.getParent()).findViewById(R.id.bubble_outer);
					Drawable d = view.getBackground();

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int[] states = { android.R.attr.state_pressed };
						if (d.setState(states)) {
							d.invalidateSelf();
						}
						return true;
					}
					else if (event.getAction() == MotionEvent.ACTION_UP) {
						int newStates[] = {};
						if (d.setState(newStates)) {
							d.invalidateSelf();
						}
						// call overridden method
						onBubbleTap(thisIndex);
						return true;
					}
					else {
						return false;
					}

				}
			});

		}
		catch (SecurityException e) {
			Utils.d(Utils.TAG, "setBubbleTouchListener reflection SecurityException", e);
			return;
		}
		catch (NoSuchMethodException e) {
			Utils.d(Utils.TAG, "setBubbleTouchListener does not exist", e);
			// method not overridden - do nothing
			return;
		}
	}
}
