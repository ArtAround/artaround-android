package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.PhotoWrapper;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MiniGalleryAdapter extends BaseAdapter {
	public static final String NEW_PHOTO = "new_photo";

	private Context context;
	private final ArrayList<PhotoWrapper> wrappers;
	private final ArrayList<Boolean> showLoaders;
	private final boolean editMode;

	public MiniGalleryAdapter(Context context, boolean isEditMode, ArrayList<PhotoWrapper> wrappers) {
		this.context = context;
		this.editMode = isEditMode;
		this.wrappers = wrappers;

		this.showLoaders = new ArrayList<Boolean>();
		int size = wrappers.size();

		for (int i = 0; i < size; ++i) {
			showLoaders.add(true);
		}

		showLoaders.set(size - 1, false);
	}

	@Override
	public int getCount() {
		int count = wrappers.size();
		if (showLoaders.size() < count) {
			showLoaders.add(false);
		}
		return count;
	}

	@Override
	public Object getItem(int position) {
		return wrappers.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	public void removeItem(String photoId) {
		int position = -1;
		int size = wrappers.size();

		for (int i = 0; i < size; ++i) {
			PhotoWrapper wrapper = wrappers.get(i);
			if (wrapper != null && photoId.equals(wrapper.id)) {
				position = i;
				break;
			}
		}
		if (editMode) {
			if (position != size - 1) {
				wrappers.remove(position);
				showLoaders.remove(position);
			}
		}
		else {
			wrappers.remove(position);
			showLoaders.remove(position);
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

		if (editMode && position == wrappers.size() - 1) {
			LayoutParams params = imgView.getLayoutParams();
			params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
			params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			imgView.setLayoutParams(params);
			imgView.setImageResource(R.drawable.img_camera);

			progress.setVisibility(View.GONE);
			text.setVisibility(View.VISIBLE);
		}
		if (!showLoaders.get(position)) {
			PhotoWrapper wrapper = wrappers.get(position);
			if (wrapper != null && wrapper.thumbUri != null) {
				//imgView.setImageBitmap(Utils.decodeBitmap(wrapper.thumbUri, imgView));
				imgView.setImageDrawable(new BitmapDrawable(wrapper.thumbUri));
				view.setTag(wrapper.id);
			}
			progress.setVisibility(View.GONE);
		}
		else {
			progress.setVisibility(View.VISIBLE);
		}
		return view;
	}

	public void setShowLoader(boolean show, int position) {
		showLoaders.set(position, show);
		notifyDataSetChanged();
	}

	public void setShowLoaders(boolean show) {
		int size = showLoaders.size();
		for (int i = 0; i < size; ++i) {
			showLoaders.set(i, show);
		}
		if (editMode) {
			showLoaders.set(size - 1, false);
		}
		notifyDataSetChanged();
	}

	public void setContext(Context context) {
		this.context = context;
	}
}