package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GalleryAdapter extends BaseAdapter {
	public static final Uri PLACEHOLDER = Uri.EMPTY;

	private final Context context;
	private final ArrayList<Uri> photoUris;
	private boolean hideLoaders;

	public GalleryAdapter(Context context, boolean showLoaders) {
		this.context = context;
		this.hideLoaders = !showLoaders;

		photoUris = new ArrayList<Uri>();
		photoUris.add(PLACEHOLDER);
		photoUris.add(PLACEHOLDER);
		photoUris.add(PLACEHOLDER);
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
	public int getViewTypeCount() {
		return 2;
	}

	public void addItem(Uri uri) {
		if (uri == null) {
			return;
		}
		if (photoUris.get(0).equals(PLACEHOLDER)) {
			photoUris.set(0, uri);
		}
		else if (photoUris.get(2).equals(PLACEHOLDER)) {
			photoUris.set(2, uri);
		}
		else {
			photoUris.add(uri);
		}
		notifyDataSetChanged();
	}

	public void removeItem(int position) {
		if (position == 2 && !photoUris.get(2).equals(PLACEHOLDER)) {
			photoUris.set(2, PLACEHOLDER);
		}
		else if (position == 0 && !photoUris.get(0).equals(PLACEHOLDER)) {
			photoUris.set(0, PLACEHOLDER);
		}
		else {
			photoUris.remove(position);
		}
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if ((position == 0 && photoUris.get(0).equals(PLACEHOLDER)) || position == 1
				|| (position == 2 && photoUris.get(2).equals(PLACEHOLDER))) {

			View view = LayoutInflater.from(context).inflate(R.layout.gallery_add_thumb, parent, false);

			if (position == 1) {
				view.findViewById(R.id.img_add_photo).setVisibility(View.VISIBLE);
				view.findViewById(R.id.txt_add_photo).setVisibility(View.VISIBLE);
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
			else if (hideLoaders) {
				view.findViewById(R.id.progress).setVisibility(View.GONE);
			}
			return view;
		}
		else {
			ImageView imageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.gallery_thumb, parent,
					false);
			Uri uri = photoUris.get(position);
			if (uri != null) {
				imageView.setImageURI(uri);
			}
			return imageView;
		}
	}

	public void hideLoaders() {
		hideLoaders = true;
		notifyDataSetChanged();
	}
}