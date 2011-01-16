package us.artaround.android.ui;

import java.util.List;

import us.artaround.R;
import us.artaround.android.services.FlickrService;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
	private Context context;
	private int itemBackground;
	private List<Uri> photoUris;

	public ImageAdapter(Context context, List<Uri> photoUris) {
		this.context = context;
		this.photoUris = photoUris;

		TypedArray a = context.obtainStyledAttributes(R.styleable.ArtGallery);
		itemBackground = a.getResourceId(R.styleable.ArtGallery_android_galleryItemBackground, 0);
		a.recycle();
	}

	@Override
	public int getCount() {
		return photoUris.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(context);

		Uri realUri = photoUris.get(position);
		if (null != realUri) {
			imageView.setImageURI(realUri);
		}

		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setLayoutParams(new Gallery.LayoutParams(FlickrService.THUMB_WIDTH, FlickrService.THUMB_HEIGHT));
		imageView.setBackgroundResource(itemBackground);
		return imageView;
	}

}