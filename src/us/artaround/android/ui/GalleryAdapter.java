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

	private Context context;
	private final ArrayList<PhotoWrapper> wrappers;
	private final int count;
	private final boolean[] showLoader;

	public GalleryAdapter(Context context, ArrayList<PhotoWrapper> wrappers, boolean isEditMode) {
		this.context = context;
		this.wrappers = wrappers;

		count = isEditMode ? wrappers.size() - 1 : wrappers.size();
		showLoader = new boolean[count];
	}

	@Override
	public int getCount() {
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(context).inflate(R.layout.gallery_thumb, parent, false);
		}

		if (!showLoader[position]) {
			if (wrappers.get(position) != null) {
				ImageView imgView = (ImageView) view.findViewById(R.id.img_add_photo);
				imgView.setImageBitmap(Utils.decodeBitmap(wrappers.get(position).thumbUri, imgView));
			}
			view.findViewById(R.id.progress).setVisibility(View.GONE);
		}

		return view;
	}

	public void setShowLoader(boolean show, int position) {
		showLoader[position] = show;
		notifyDataSetChanged();
	}

	public void setShowLoaders(boolean show) {
		for (int i = 0; i < count; i++) {
			showLoader[i] = show;
		}
		notifyDataSetChanged();
	}

	public void setContext(Context context) {
		this.context = context;
	}
}