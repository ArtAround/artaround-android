package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GalleryAdapter extends BaseAdapter {

	private final Context context;
	private final ArrayList<PhotoWrapper> photos;

	public GalleryAdapter(Context context, ArrayList<PhotoWrapper> photos) {
		this.context = context;
		this.photos = photos;
	}

	@Override
	public int getCount() {
		return photos.size();
	}

	@Override
	public Object getItem(int position) {
		return photos.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	public void addItem(String id, String uri, Drawable drawable) {
		PhotoWrapper pw = new PhotoWrapper(id, uri, drawable);
		photos.add(pw);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		PhotoWrapper photo = photos.get(position);
		if (photo != null) {
			ImageView imageView = (ImageView) convertView;
			if (imageView == null) {
				imageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.gallery_thumb, parent, false);
			}

			if (photo.drawable != null) {
				imageView.setImageDrawable(photo.drawable);
			}
			else if (photo.uri != null && photo.id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {
				imageView.setImageURI(Uri.parse(photo.uri));
			}
			return imageView;
		}
		return null;
	}

	public void hideLoaders() {
		notifyDataSetChanged();
	}
}