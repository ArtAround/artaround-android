package us.artaround.android.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import us.artaround.R;
import us.artaround.android.commons.Utils;
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
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

public class ArtBalloonsOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	private OverlayTapListener context;

	private MapView mapView;
	private BalloonOverlayView balloonView;
	private View clickRegion;
	private int viewOffset;
	final MapController mc;

	public ArtBalloonsOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));

		this.mapView = mapView;
		viewOffset = 0;
		mc = mapView.getController();

		this.doPopulate(); //do populate after constructor so it doen't crash when empty
	}

	public ArtBalloonsOverlay(Drawable defaultMarker, OverlayTapListener context, MapView mapView) {
		this(defaultMarker, mapView);
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

	public void addOverlay(OverlayItem overlay) {
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

	/**
	 * Set the horizontal distance between the marker and the bottom of the
	 * information balloon. The default is 0 which works well for center bounded
	 * markers. If your marker is center-bottom bounded, call this before adding
	 * overlay items to ensure the balloon hovers exactly above the marker.
	 * 
	 * @param pixels
	 *            - The padding between the center point and the bottom of the
	 *            information balloon.
	 */
	public void setBalloonBottomOffset(int pixels) {
		viewOffset = pixels;
	}

	/**
	 * Override this method to handle a "tap" on a balloon. By default, does
	 * nothing and returns false.
	 * 
	 * @param index
	 *            - The index of the item whose balloon is tapped.
	 * @return true if you handled the tap, otherwise false.
	 */
	protected boolean onBalloonTap(int index) {
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

		boolean isRecycled;
		final int thisIndex;
		GeoPoint point;

		thisIndex = index;
		point = createItem(index).getPoint();

		if (balloonView == null) {
			balloonView = new BalloonOverlayView(mapView.getContext(), viewOffset);
			clickRegion = balloonView.findViewById(R.id.balloon_inner_layout);
			isRecycled = false;
		}
		else {
			isRecycled = true;
		}

		balloonView.setVisibility(View.GONE);

		List<Overlay> mapOverlays = mapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBalloons(mapOverlays);
		}

		balloonView.setData(createItem(index));

		MapView.LayoutParams params = new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				point, MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;

		setBalloonTouchListener(thisIndex);

		balloonView.setVisibility(View.VISIBLE);

		if (isRecycled) {
			balloonView.setLayoutParams(params);
		}
		else {
			mapView.addView(balloonView, params);
		}

		mc.animateTo(point);

		return true;
	}

	/**
	 * Sets the visibility of this overlay's balloon view to GONE.
	 */
	private void hideBalloon() {
		if (balloonView != null) {
			balloonView.setVisibility(View.GONE);
		}
	}

	/**
	 * Hides the balloon view for any other BalloonItemizedOverlay instances
	 * that might be present on the MapView.
	 * 
	 * @param overlays
	 *            - list of overlays (including this) on the MapView.
	 */
	private void hideOtherBalloons(List<Overlay> overlays) {

		for (Overlay overlay : overlays) {
			if (overlay instanceof ArtBalloonsOverlay && overlay != this) {
				((ArtBalloonsOverlay) overlay).hideBalloon();
			}
		}

	}

	/**
	 * Sets the onTouchListener for the balloon being displayed, calling the
	 * overridden onBalloonTap if implemented.
	 * 
	 * @param thisIndex
	 *            - The index of the item whose balloon is tapped.
	 */
	private void setBalloonTouchListener(final int thisIndex) {

		try {
			@SuppressWarnings("unused")
			Method m = this.getClass().getDeclaredMethod("onBalloonTap", int.class);

			clickRegion.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {

					View l = ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
					Drawable d = l.getBackground();

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
						onBalloonTap(thisIndex);
						return true;
					}
					else {
						return false;
					}

				}
			});

		}
		catch (SecurityException e) {
			Utils.w(Utils.TAG, "setBalloonTouchListener reflection SecurityException", e);
			return;
		}
		catch (NoSuchMethodException e) {
			// method not overridden - do nothing
			return;
		}

	}
}
