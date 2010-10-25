package us.artaround.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.google.android.maps.MapView;

public class ArtMapView extends MapView implements OnZoomListener {

	private int zoom;
	private ZoomListener zoomListener;

	public ArtMapView(Context context, String apiKey) {
		super(context, apiKey);
	}
	
	public ArtMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ArtMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setZoomLevel(int zoom) {
		this.zoom = zoom;
		this.getController().setZoom(zoom);
	}

	public ZoomListener getZoomListener() {
		return zoomListener;
	}

	public void setZoomListener(ZoomListener listener) {
		this.zoomListener = listener;
		this.getZoomButtonsController().setOnZoomListener(this);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		if(event.getAction() == MotionEvent.ACTION_UP){
			checkForZoomEvent();
		}
		return result;
	}

	private void checkForZoomEvent() {
		if(this.zoomListener!=null){
			int newZoom = this.getZoomLevel();
			if(newZoom!=zoom){
				this.zoomListener.onZoom(zoom, newZoom);
			}
			zoom = newZoom;
		}
	}

	@Override
	public void onVisibilityChanged(boolean visible) {}

	@Override
	public void onZoom(boolean zoomIn) {
		int oldZoom = this.zoom;
		this.setZoomLevel(oldZoom + (zoomIn?1:-1));
		if(this.zoomListener!=null){
			this.zoomListener.onZoom(oldZoom, this.zoom);
		}
	}

}
