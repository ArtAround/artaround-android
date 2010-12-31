package us.artaround.android.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MyLocationOverlay extends Overlay {
	private int drawableId;
	private Context context;
	private GeoPoint geoPoint;
	private OverlayTapListener callback;
	private Paint paint;

	public MyLocationOverlay(Context context, int drawableId) {
		this.context = context;
		this.drawableId = drawableId;

		if (context instanceof OverlayTapListener) {
			callback = (OverlayTapListener) context;
		}
		paint = new Paint();
	}

	public void setDrawableId(int drawableId) {
		this.drawableId = drawableId;
	}

	public void setGeoPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}

	@Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		super.draw(canvas, mapView, shadow);

		if (geoPoint == null) {
			return false;
		}

		// Converts latitude /longitude to coordinates on the screen.
        Point myScreenCoords = new Point();
		mapView.getProjection().toPixels(geoPoint, myScreenCoords);

        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), drawableId);
		canvas.drawBitmap(bmp, myScreenCoords.x, myScreenCoords.y, paint);

        return true;
    }

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		if (callback != null) {
			callback.onTap(this);
			return false;
		}
		return super.onTap(p, mapView);
	}
}
