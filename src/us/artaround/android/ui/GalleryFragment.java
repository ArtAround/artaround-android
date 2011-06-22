package us.artaround.android.ui;

import java.util.ArrayList;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.ImageDownloader;
import us.artaround.android.common.LoaderPayload;
import us.artaround.android.common.PhotoWrapper;
import us.artaround.android.common.Utils;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.ArtAroundException;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.TextView;

public class GalleryFragment extends Fragment implements LoaderCallbacks<LoaderPayload> {
	private static final String TAG = "GalleryFragment";

	private static final int LOADER_PHOTO = 500;

	private TextView tvTitle;
	private String artTitle;

	private Gallery gallery;
	private GalleryAdapter adapter;

	private ArrayList<PhotoWrapper> wrappers;
	private int photoCount;
	private int toLoadCount;
	private int loadedCount;
	private String idToLoad;
	private boolean isEditMode;

	public GalleryFragment() {}

	public GalleryFragment(ArrayList<PhotoWrapper> wrappers, String artTitle, boolean isEditMode) {
		this.isEditMode = isEditMode;
		this.artTitle = artTitle;
		this.wrappers = wrappers;
		photoCount = wrappers.size();

		// search all the photos to find how many are actual Flickr photos that need to be loaded
		// (not local photos temporarily added in the gallery from phone) and the first id of such a photo
		for (int i = 0; i < photoCount; ++i) {
			if (wrappers.get(i) == null) continue;

			String id = wrappers.get(i).id;
			if (!id.contains(MiniGalleryAdapter.NEW_PHOTO)) {
				toLoadCount++;
				if (idToLoad == null) {
					idToLoad = id;
				}
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setRetainInstance(true); // this makes all the magic happen!
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.gallery_fragment, container, false);
		gallery = (Gallery) view.findViewById(R.id.gallery);
		tvTitle = (TextView) view.findViewById(R.id.title);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupState();
	}

	private void setupState() {
		tvTitle.setText(artTitle);

		if (adapter == null) {
			adapter = new GalleryAdapter(getActivity(), wrappers, isEditMode);
		}
		else {
			adapter.setContext(getActivity());
		}
		gallery.setAdapter(adapter);

		Utils.d(TAG, "setupState(): toLoadCount=", toLoadCount, "loadedCount=", loadedCount, "idToLoad=", idToLoad);

		if (loadedCount < toLoadCount && idToLoad != null) {
			adapter.setShowLoaders(true);
			getLoaderManager().restartLoader(LOADER_PHOTO, new Bundle(), this);
		}
	}

	@Override
	public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
		if (LOADER_PHOTO != id) return null;

		return new AsyncLoader<LoaderPayload>(getActivity()) {
			@Override
			public LoaderPayload loadInBackground() {
				Utils.d(TAG, "loadInBackground(): loading photo with id=", idToLoad);

				try {
					Uri uri = ImageDownloader.quickGetImageUri(idToLoad);
					if (uri == null) {
						FlickrService srv = FlickrService.getInstance(getActivity());
						FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(idToLoad), FlickrService.SIZE_MEDIUM);
						if (photo != null) {
							args.putString(ImageDownloader.EXTRA_PHOTO_ID, idToLoad);
							args.putString(ImageDownloader.EXTRA_PHOTO_URL, photo.url);
							args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_MEDIUM);
							uri = ImageDownloader.getImageUri(args);
						}
					}

					if (uri != null) {
						for (int i = 0; i < photoCount; i++) {
							if (wrappers.get(i) != null && idToLoad.equals(wrappers.get(i).id)) {
								wrappers.get(i).thumbUri = uri.toString();
								Utils.d(TAG, "loadInBackground(): found uri=", uri, "for photo with id=", idToLoad);
							}
						}
					}
					return new LoaderPayload(LoaderPayload.STATUS_OK, null, args);
				}
				catch (ArtAroundException e) {
					Utils.d(TAG, "loadInBackground(): exc=", e);
					return new LoaderPayload(LoaderPayload.STATUS_ERROR, e, args);
				}
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
		if (loader == null || loader.getId() != LOADER_PHOTO) return;

		loadedCount++;
		Utils.d(TAG, "onLoadFinished(): loadedCount=", loadedCount);

		if (payload.getStatus() == LoaderPayload.STATUS_OK) {
			if (loadedCount == toLoadCount) {
				adapter.setShowLoaders(false);
				idToLoad = null;
				Utils.d(TAG, "onLoadFinished(): done loading all photos!");
			}
			else {
				adapter.setShowLoader(false, loadedCount - 1);
				idToLoad = getNextIdToLoad(loadedCount);
				getLoaderManager().restartLoader(LOADER_PHOTO, payload.getArgs(), this);
			}
		}
		else {
			adapter.setShowLoader(false, loadedCount - 1);
			Utils.d(TAG, "onLoadFinished(): could not load photo with id=", idToLoad, payload.getResult());
		}
		adapter.notifyDataSetChanged();
	}

	private String getNextIdToLoad(int start) {
		for (int i = start; i < photoCount; ++i) {
			String id = wrappers.get(i).id;
			if (!id.contains(MiniGalleryAdapter.NEW_PHOTO)) {
				return id;
			}
		}
		return null;
	}

	@Override
	public void onLoaderReset(Loader<LoaderPayload> loader) {
		Utils.d(TAG, "onLoaderReset()");
	}

	@Override
	public void onDestroy() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Utils.deleteCachedFiles();
			}
		}).start();
		super.onDestroy();
	}
}
