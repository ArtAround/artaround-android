package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.ImageDownloader;
import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class GalleryFragment extends Fragment implements LoaderCallbacks<Boolean> {
	//private static final String TAG = "GalleryFragment";

	public static final String ARG_PHOTOS = "photos";
	public static final String ARG_PHOTO = "photo";
	public static final String ARG_TITLE = "title";

	private static final String SAVE_PHOTOS = "photos";
	private static final String SAVE_TO_LOAD_COUNT = "to_load_count";
	private static final String SAVE_LOADED_PHOTOS_COUNT = "loaded_photos_count";

	private static final int LOADER_PHOTO = 500;

	private Gallery gallery;
	private GalleryAdapter adapter;
	//private GallerySaver gallerySaver;

	private Animation rotateAnim;
	private ImageView imgLoader;
	private TextView tvTitle;

	private String title;
	private ArrayList<PhotoWrapper> photos;
	private int toLoadCount;
	private AtomicInteger loadedCount = new AtomicInteger(0);

	@SuppressWarnings("unchecked")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Utils.d(Utils.TAG, "onAttach()");

		//		if (activity instanceof GallerySaver) {
		//			gallerySaver = (GallerySaver) activity;
		//		}

		photos = (ArrayList<PhotoWrapper>) getArguments().getSerializable(ARG_PHOTOS);
		title = getArguments().getString(ARG_TITLE);
		Utils.d(Utils.TAG, "onAttach(): photos=", photos);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.gallery_fragment, container, false);
		gallery = (Gallery) view.findViewById(R.id.gallery);
		adapter = new GalleryAdapter(getActivity(), photos);
		gallery.setAdapter(adapter);

		tvTitle = (TextView) view.findViewById(R.id.title);
		if (!TextUtils.isEmpty(title)) {
			tvTitle.setText(title);
		}

		rotateAnim = Utils.getRoateAnim(getActivity());
		imgLoader = (ImageView) view.findViewById(R.id.img_loader);

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle args) {
		args.putSerializable(SAVE_PHOTOS, photos);
		args.putInt(SAVE_TO_LOAD_COUNT, toLoadCount);
		args.putInt(SAVE_LOADED_PHOTOS_COUNT, loadedCount.get());

		//gallerySaver.saveGalleryState(args);
		super.onSaveInstanceState(args);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Utils.d(Utils.TAG, "onActivityCreated()");

		//Bundle savedState = gallerySaver.restoreGalleryState();
		setupState(null);
	}

	@SuppressWarnings("unchecked")
	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			photos = (ArrayList<PhotoWrapper>) savedInstanceState.getSerializable(SAVE_PHOTOS);
			toLoadCount = savedInstanceState.getInt(SAVE_TO_LOAD_COUNT);
			loadedCount = new AtomicInteger(savedInstanceState.getInt(SAVE_LOADED_PHOTOS_COUNT, 0));
			adapter.addItems(photos);
			return;
		}

		if (photos == null || photos.isEmpty()) return;

		int size = photos.size();
		String firstId = null;
		for (int i = 0; i < size; i++) {
			String id = photos.get(i).id;
			if (!TextUtils.isEmpty(id) && !id.contains(MiniGalleryAdapter.NEW_PHOTO)) {
				toLoadCount++;
				if (firstId == null) {
					firstId = id;
				}
			}
		}

		Utils.d(Utils.TAG, "There are", toLoadCount, "photos to load");

		if (firstId != null) {
			toggleLoading(true);
			adapter.setShowLoaders(true);

			Bundle args = new Bundle();
			args.putString(ARG_PHOTO, firstId);
			getLoaderManager().restartLoader(LOADER_PHOTO, args, this);
		}
	}

	private String findNextIdToLoad(int start) {
		int size = photos.size();

		for (int i = start; i < size; i++) {
			String id = photos.get(i).id;
			if (!TextUtils.isEmpty(id) && !id.contains(MiniGalleryAdapter.NEW_PHOTO)) {
				return id;
			}
		}
		return null;
	}

	@Override
	public Loader<Boolean> onCreateLoader(int id, final Bundle args) {

		return new AsyncLoader<Boolean>(getActivity()) {
			@Override
			public Boolean loadInBackground() {
				FlickrService srv = FlickrService.getInstance();
				String photoId = args.getString(ARG_PHOTO);

				try {
					FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(photoId), FlickrService.SIZE_ORIGINAL);
					if (photo != null) {
						Drawable drawable = ImageDownloader.getImageDrawable(photo.url);
						for (int i = 0; i < photos.size(); i++) {
							if (photoId.equals(photos.get(i).id)) {
								photos.get(i).drawable = drawable;
								return true;
							}
						}
					}
				}
				catch (ArtAroundException e) {
					Utils.d(Utils.TAG, "loadInBackground(): exc=", e);
				}
				return false;
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<Boolean> loader, Boolean result) {
		if (result == null || result == false) return;
		adapter.notifyDataSetChanged();

		if (loadedCount.getAndIncrement() == toLoadCount - 1) {
			toggleLoading(false);
			adapter.setShowLoaders(false);

			Utils.d(Utils.TAG, "onLoadFinished(): done loading all pictures!");
		}
		else {
			adapter.setShowLoaders(false, loadedCount.get() - 1);

			String id = findNextIdToLoad(loadedCount.get());
			Utils.d(Utils.TAG, "onLoadFinished(): loading next picture=", id);
			if (id != null) {
				Bundle args = new Bundle();
				args.putString(ARG_PHOTO, id);
				getLoaderManager().restartLoader(LOADER_PHOTO, args, this);
			}
		}
		loader.stopLoading();
	}

	@Override
	public void onLoaderReset(Loader<Boolean> loader) {}

	private void toggleLoading(boolean show) {
		if (show) {
			imgLoader.setVisibility(View.VISIBLE);
			imgLoader.startAnimation(rotateAnim);
		}
		else {
			imgLoader.clearAnimation();
			imgLoader.setVisibility(View.INVISIBLE);
		}
	}
}
