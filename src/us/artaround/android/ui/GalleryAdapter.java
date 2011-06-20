package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GalleryAdapter extends BaseAdapter {

	private final Context context;
	private final ArrayList<PhotoWrapper> photos;
	private final int size;
	private final boolean[] showLoaders;

	public GalleryAdapter(Context context, ArrayList<PhotoWrapper> photos) {
		this.context = context;
		this.photos = photos;

		size = photos.size();
		showLoaders = new boolean[size];
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
		return 1;
	}

	public void addItem(PhotoWrapper wrapper) {
		photos.add(wrapper);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(context).inflate(R.layout.gallery_thumb, parent, false);
		}

		if (!showLoaders[position]) {
			ImageView imgView = (ImageView) view.findViewById(R.id.img_add_photo);

			PhotoWrapper photo = photos.get(position);
			if (photo != null) {

				if (photo.drawable != null) {
					imgView.setImageDrawable(photo.drawable);
				}
				else if (photo.uri != null && photo.id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {
					imgView.setImageBitmap(Utils.decodeBitmap(photo.uri, imgView));
				}
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
		}

		return view;
	}

	public void setShowLoaders(boolean show, int position) {
		showLoaders[position] = show;
		notifyDataSetChanged();
	}

	public void addItems(ArrayList<PhotoWrapper> photos) {
		if (photos == null) return;
		for (int i = 0; i < photos.size(); i++) {
			addItem(photos.get(i));
		}
	}

	public void setShowLoaders(boolean show) {
		for (int i = 0; i < size; i++) {
			showLoaders[i] = show;
		}
	}
}