package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.Utils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import com.google.android.maps.MapView;

public class ArtScrollView extends ScrollView {
	private static int NONE = -1;
	private static int SCROLLVIEW = 0;
	private static int MAPVIEW = 1;

	private float lastX;
	private float lastY;
	private float firstX;
	private float firstY;
	private float lastRawX;
	private float lastRawY;
	private int gestureRecipient = NONE;
	private boolean doneClick = true;
	private boolean firstClick = true;
	private ArtMapView mapView = null;

	public ArtScrollView(Context context) {
		super(context);
	}

	public ArtScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ArtScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		boolean result = true;

		//Utils.d(Utils.TAG, ev.getX() + " " + ev.getY());
		//Utils.d(Utils.TAG, ev.getRawX() + " " + ev.getRawY());

		// fresh click
		if (doneClick) {
			firstX = ev.getX();
			firstY = ev.getY();
			lastX = firstX;
			lastY = firstY;
			lastRawX = ev.getRawX();
			lastRawY = ev.getRawY();
			doneClick = false;
			firstClick = true;
		}
		if ((gestureRecipient == NONE) && (lastX != ev.getX() || lastY != ev.getY())) {
			if (touchInsideMinimap((int) ev.getX(), (int) ev.getY())) {
				gestureRecipient = MAPVIEW;
				//Utils.d(Utils.TAG, "1");
			}
			else {
				gestureRecipient = SCROLLVIEW;
				//Utils.d(Utils.TAG, "1");
			}
		}
		if (gestureRecipient == MAPVIEW) {
//			if (!firstClick) {
//				float offsetX = (((ev.getX() - lastX) - (ev.getRawX() - lastRawX)) * -1);
//				float offsetY = (((ev.getY() - lastY) - (ev.getRawY() - lastRawY)) * -1);
//				Utils.d(Utils.TAG, offsetX + " - " + offsetY);
//				ev.offsetLocation(offsetX, offsetY);
//			}
			if (touchInsideMinimap((int) ev.getX(), (int) ev.getY())) {
				//Utils.d(Utils.TAG, "3");
			result = getMap().onTouchEvent(ev);
			}
			else {
				gestureRecipient = SCROLLVIEW;
				super.onTouchEvent(ev);
			}
			firstClick = false;
		}
		else if (gestureRecipient == SCROLLVIEW) {
			result = super.onTouchEvent(ev);
			//Utils.d(Utils.TAG, "4");
		}
		if (ev.getAction() == MotionEvent.ACTION_UP) {
			if (((firstX == ev.getX()) && (firstY == ev.getY()))) {
				//no movement, it's a click
				super.onTouchEvent(ev);
				//Utils.d(Utils.TAG, "5");
			}
			doneClick = true;
			gestureRecipient = NONE;
			Utils.d(Utils.TAG, "6");
		}
		lastX = ev.getX();
		lastY = ev.getY();
		lastRawX = ev.getRawX();
		lastRawY = ev.getRawY();
		return result;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		getMap().onTouchEvent(ev);
		return super.onInterceptTouchEvent(ev);
	}

	private ArtMapView getMap() {
		if (mapView == null) {
			mapView = (ArtMapView) this.findViewById(R.id.minimap);
		}
		return mapView;
	}

	private boolean touchInsideMinimap(int x, int y) {
		int[] pos = new int[2];
		MapView mv = getMap();
		mv.getLocationOnScreen(pos);
		return (x >= pos[0] && x < pos[0] + mv.getWidth()) && (y >= pos[1] && y < pos[1] + mv.getHeight());
	}
}
