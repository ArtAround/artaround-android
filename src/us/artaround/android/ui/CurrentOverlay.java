package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class CurrentOverlay extends ItemizedOverlay<OverlayItem> {
	private final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

	private final Drawable marker;
	private OverlayItem inDrag;
	private ImageView dragImage;
	private TextView tvCoordinates;
	private final boolean draggable;

	private int xDragImageOffset = 0;
	private int yDragImageOffset = 0;
	private int xDragTouchOffset = 0;
	private int yDragTouchOffset = 0;

	private final Activity context;

	public CurrentOverlay(Activity context, Drawable defaultMarker, GeoPoint geo, boolean draggable) {
		super(boundCenterBottom(defaultMarker));
		this.marker = defaultMarker;
		this.draggable = draggable;
		this.context = context;

		items.add(new OverlayItem(geo, context.getString(R.string.current_location), ""));
		populate(); //do populate after constructor so it doen't crash when empty

		if (draggable) {
			dragImage = (ImageView) context.findViewById(R.id.drag);
			xDragImageOffset = dragImage.getDrawable().getIntrinsicWidth() / 2;
			yDragImageOffset = dragImage.getDrawable().getIntrinsicHeight();

			tvCoordinates = (TextView) context.findViewById(R.id.footer);
		}
	}


	@Override
	protected OverlayItem createItem(int i) {
		return items.get(i);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		boundCenterBottom(marker);
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		if (!draggable) {
			return super.onTouchEvent(event, mapView);
		}

		final int action = event.getAction();
		final int x = (int) event.getX();
		final int y = (int) event.getY();
		boolean result = false;

		if (action == MotionEvent.ACTION_DOWN) {
			for (OverlayItem item : items) {
				Point p = new Point(0, 0);

				mapView.getProjection().toPixels(item.getPoint(), p);

				if (hitTest(item, marker, x - p.x, y - p.y)) {
					result = true;
					inDrag = item;
					items.remove(inDrag);
					populate();

					xDragTouchOffset = 0;
					yDragTouchOffset = 0;

					setDragImagePosition(p.x, p.y);
					dragImage.setVisibility(View.VISIBLE);

					xDragTouchOffset = x - p.x;
					yDragTouchOffset = y - p.y;

					break;
				}
			}
		}
		else if (action == MotionEvent.ACTION_MOVE && inDrag != null) {
			setDragImagePosition(x, y);
			result = true;
		}
		else if (action == MotionEvent.ACTION_UP && inDrag != null) {
			dragImage.setVisibility(View.GONE);

			GeoPoint pt = mapView.getProjection().fromPixels(x - xDragTouchOffset, y - yDragTouchOffset);
			OverlayItem toDrop = new OverlayItem(pt, inDrag.getTitle(), inDrag.getSnippet());

			items.add(toDrop);
			populate();
			updateCoordinatesView();

			inDrag = null;
			result = true;
		}

		return (result || super.onTouchEvent(event, mapView));
	}

	private void setDragImagePosition(int x, int y) {
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dragImage.getLayoutParams();

		lp.setMargins(x - xDragImageOffset - xDragTouchOffset, y - yDragImageOffset - yDragTouchOffset, 0, 0);
		dragImage.setLayoutParams(lp);
	}

	private void updateCoordinatesView() {
		GeoPoint point = items.get(0).getPoint();
		String lat = Utils.coordinateFormatter.format(point.getLatitudeE6() / 1E6);
		String longi = Utils.coordinateFormatter.format(point.getLongitudeE6() / 1E6);
		tvCoordinates.setText(context.getString(R.string.reposition_pin) + ": " + lat + ", " + longi);
	}

	public double getCurrentLatitude() {
		return items.get(0).getPoint().getLatitudeE6() / 1E6;
	}

	public double getCurrentLongitude() {
		return items.get(0).getPoint().getLongitudeE6() / 1E6;
	}
}
