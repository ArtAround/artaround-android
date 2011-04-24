package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MiniGalleryAdapter extends BaseAdapter {
	public static final String NEW_PHOTO = "new_photo";

	private final Context context;
	private final ArrayList<PhotoWrapper> photos;
	private final boolean editMode;
	private boolean showLoaders;

	public MiniGalleryAdapter(Context context, boolean editMode) {
		this.context = context;
		this.editMode = editMode;

		this.photos = new ArrayList<PhotoWrapper>();
		photos.add(null); // empty placeholder
		photos.add(null); // add image
		photos.add(null); // empty placeholder
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

	//FIXME find a better way of "filling-in" the gallery from center
	public void addItem(PhotoWrapper wrapper) {
		if (!editMode && photos.get(1) == null) {
			photos.set(1, wrapper);
		}

		else if (photos.get(0) == null) {
			photos.set(0, wrapper);
		}

		else if (photos.get(2) == null) {
			photos.set(2, wrapper);
		}
		else {
			photos.add(wrapper);
		}
		notifyDataSetChanged();
	}

	public void removeItem(String photoId) {
		int position = -1;
		int size = photos.size();
		for (int i = 0; i < size; i++) {
			PhotoWrapper phr = photos.get(i);
			if (phr != null && phr.id.equals(photoId)) {
				position = i;
				break;
			}
		}
		if (position == 0 || position == 2) {
			photos.set(position, null);
		}
		else {
			photos.remove(position);
		}
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (editMode
				&& (position == 0 && photos.get(0) == null || position == 1 || (position == 2 && photos.get(2) == null))) {
			View view = null;
			if (convertView instanceof LinearLayout) {
				view = convertView;
			}
			if (view == null) {
				view = LayoutInflater.from(context).inflate(R.layout.mini_gallery_add_thumb, parent, false);
			}

			if (position == 1) {
				view.findViewById(R.id.img_add_photo).setVisibility(View.VISIBLE);
				view.findViewById(R.id.txt_add_photo).setVisibility(View.VISIBLE);
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
			else if (!showLoaders) {
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
			return view;
		}
		else {
			ImageView imageView = null;
			if (convertView instanceof ImageView) {
				imageView = (ImageView) convertView;
			}
			if (imageView == null) {
				imageView = (ImageView) LayoutInflater.from(context)
						.inflate(R.layout.mini_gallery_thumb, parent, false);
			}
			PhotoWrapper wrapper = photos.get(position);
			if (wrapper != null) {
				if (wrapper.uri != null) {
					imageView.setImageURI(Uri.parse(wrapper.uri));
				}
				imageView.setTag(wrapper.id);
			}
			return imageView;
		}
	}

	public void toggleLoaders(boolean show) {
		showLoaders = show;
		notifyDataSetChanged();
	}

	public void addItems(ArrayList<PhotoWrapper> photos) {
		if (photos == null) return;
		for (int i = 0; i < photos.size(); i++) {
			addItem(photos.get(i));
		}
	}
}