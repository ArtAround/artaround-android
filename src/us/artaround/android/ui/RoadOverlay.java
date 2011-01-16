package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.android.commons.navigation.Road;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class RoadOverlay extends Overlay {
	Road road;
	ArrayList<GeoPoint> points;
	GeoPoint moveTo;

	public RoadOverlay(Road road, MapView mv) {
		this.road = road;
		computeMiddle();
	}
	
	private void computeMiddle() {
		if (road.route.length > 0) {
			points = new ArrayList<GeoPoint>();
			for (int i = 0; i < road.route.length; i++) {
				points.add(new GeoPoint((int) (road.route[i][1] * 1000000), (int) (road.route[i][0] * 1000000)));
			}
			//int moveToLat = (points.get(0).getLatitudeE6() + (points.get(points.size() - 1).getLatitudeE6() - points
			//.get(0).getLatitudeE6()) / 2);
			//int moveToLong = (points.get(0).getLongitudeE6() + (points.get(points.size() - 1).getLongitudeE6() - points
			//.get(0).getLongitudeE6()) / 2);

			int moveToLat = points.get(points.size() - 1).getLatitudeE6();
			int moveToLong = points.get(points.size() - 1).getLongitudeE6();
			moveTo = new GeoPoint(moveToLat, moveToLong);
		}
	}
	
	public GeoPoint getMoveTo() {
		return moveTo;
	}
	

	@Override
	public boolean draw(Canvas canvas, MapView mv, boolean shadow, long when) {
		super.draw(canvas, mv, shadow);
		drawPath(mv, canvas);
		return true;
	}

	public void drawPath(MapView mv, Canvas canvas) {
		int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(8);

		paint.setPathEffect(new DashPathEffect(new float[] { 12, 6 }, 18));
		for (int i = 0; i < points.size(); i++) {
			Point point = new Point();
			mv.getProjection().toPixels(points.get(i), point);
			x2 = point.x;
			y2 = point.y;
			if (i > 0) {
				canvas.drawLine(x1, y1, x2, y2, paint);
			}
			x1 = x2;
			y1 = y2;
		}
	}
}
