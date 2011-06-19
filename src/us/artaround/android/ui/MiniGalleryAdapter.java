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
		return editMode ? 2 : 1;
	}

	//FIXME find a better way of "filling-in" the gallery from center
	public void addItem(PhotoWrapper wrapper) {
		if (editMode) {
			// 0, _, 2, 3,...
			if (photos.get(0) == null) {
				photos.set(0, wrapper);
			}
			else if (photos.get(2) == null) {
				photos.set(2, wrapper);
			}
			else {
				photos.add(wrapper);
			}
		}
		else {
			// 1, 0, 2, 3...
			if (photos.get(1) == null) {
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
		View view = convertView;

		if (editMode) {
			if (view == null) {
				view = LayoutInflater.from(context).inflate(R.layout.mini_gallery_add_thumb, parent, false);
			}

			if (position == 1) {
				view.findViewById(R.id.img_add_photo).setVisibility(View.VISIBLE);
				view.findViewById(R.id.progress).setVisibility(View.GONE);
				view.findViewById(R.id.txt_add_photo).setVisibility(View.VISIBLE);
			}
			else if (!showLoaders) {
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
		}
		else {
			if (view == null) {
				view = LayoutInflater.from(context).inflate(R.layout.mini_gallery_thumb, parent, false);
			}
			ImageView imgView = (ImageView) view;

			PhotoWrapper wrapper = photos.get(position);
			if (wrapper != null) {
				if (wrapper.uri != null) {
					imgView.setImageURI(Uri.parse(wrapper.uri));
				}
				imgView.setTag(wrapper.id);
			}
			imgView.setVisibility(View.VISIBLE);
		}
		return view;
	}

	public void setShowLoaders(boolean show) {
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