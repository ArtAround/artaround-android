package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.artaround.R;
import us.artaround.android.commons.ImageDownloader;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.tasks.LoadArtPhotoTask;
import us.artaround.android.commons.tasks.LoadArtPhotoTask.LoadPhotoCallback;
import us.artaround.services.Flickr;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher.ViewFactory;

public class ArtGallery extends Activity implements ViewFactory, LoadPhotoCallback {
	private ImageSwitcher imageSwitcher;
	private Gallery gallery;
	private ImageAdapter adapter;

	private List<Uri> photoUris;
	private String[] photoIds;

	private Map<String, LoadArtPhotoTask> tasks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_gallery);

		setupVars();
		setupUi();

		if (photoUris.isEmpty()) {
			loadImages();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!tasks.isEmpty()) {
			for (LoadArtPhotoTask task : tasks.values()) {
				task.detach();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder holder = new Holder();
		holder.tasks = tasks;
		holder.photoUris = photoUris;
		return holder;
	}

	private void setupVars() {
		photoIds = getIntent().getStringArrayExtra("photoIds");

		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			tasks = holder.tasks;

			for (LoadArtPhotoTask task : tasks.values()) {
				task.attach(this);
			}
			photoUris = holder.photoUris;
		}
		else {
			tasks = new HashMap<String, LoadArtPhotoTask>();
			photoUris = new ArrayList<Uri>();
		}
	}

	private void loadImages() {
		int length = photoIds.length;
		Log.d(Utils.TAG, "Starting to load " + length + " images from Flickr...");

		for (int i = 0; i < length; i++) {
			String id = photoIds[i];
			Uri photoUri = ImageDownloader.quickGetImage(this, id + Flickr.IMAGE_FORMAT, Flickr.SIZE_LARGE);

			if (photoUri != null) {
				photoUris.add(photoUri);
			}
			else {
				tasks.put(id, (LoadArtPhotoTask) new LoadArtPhotoTask(this, id).execute(Flickr.SIZE_LARGE));
			}
		}
	}

	private void setupUi() {
		imageSwitcher = (ImageSwitcher) findViewById(R.id.image_switcher);
		imageSwitcher.setFactory(this);
		adapter = new ImageAdapter(this);

		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(adapter);
		gallery.setEmptyView(findViewById(R.id.image_loading));
		gallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				imageSwitcher.setImageURI(photoUris.get(position));
			}
		});
	}

	@Override
	public View makeView() {
		ImageView imageView = new ImageView(this);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		return imageView;
	}

	private class ImageAdapter extends BaseAdapter {
		private Context context;
		private int itemBackground;

		public ImageAdapter(Context context) {
			this.context = context;
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
			imageView.setImageURI(photoUris.get(position));
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setLayoutParams(new Gallery.LayoutParams(Flickr.THUMB_WIDTH, Flickr.THUMB_HEIGHT));
			imageView.setBackgroundResource(itemBackground);
			return imageView;
		}

	}

	@Override
	public void onLoadPhoto(String id, Uri photoUri, LoadArtPhotoTask task) {
		Log.d(Utils.TAG, "Added photo uri " + photoUri);
		photoUris.add(photoUri);
		adapter.notifyDataSetChanged();
		tasks.remove(id);
	}

	private static class Holder {
		Map<String, LoadArtPhotoTask> tasks;
		List<Uri> photoUris;
	}
}
