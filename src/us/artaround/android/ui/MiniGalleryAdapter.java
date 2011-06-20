package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
		return 1;
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
		if (view == null) {
			view = LayoutInflater.from(context).inflate(R.layout.mini_gallery_thumb, parent, false);
		}
		ImageView imgView = (ImageView) view.findViewById(R.id.img_add_photo);
		View progress = view.findViewById(R.id.progress);
		View text = view.findViewById(R.id.txt_add_photo);

		if (editMode) {
			if (position == 1) {
				imgView.setImageResource(R.drawable.img_camera);
				LayoutParams params = imgView.getLayoutParams();
				params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
				params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
				imgView.setLayoutParams(params);

				progress.setVisibility(View.GONE);
				text.setVisibility(View.VISIBLE);
			}
		}
		if (!showLoaders) {
			PhotoWrapper wrapper = photos.get(position);
			if (wrapper != null) {
				if (wrapper.uri != null) {
					imgView.setImageBitmap(Utils.decodeBitmap(wrapper.uri, imgView));
					//imgView.setImageURI(Uri.parse(wrapper.uri));
				}
				view.setTag(wrapper.id);
			}
			progress.setVisibility(View.GONE);
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