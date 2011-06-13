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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class GalleryFragment extends Fragment implements LoaderCallbacks<Boolean> {
	private static final String TAG = "ArtAround.GalleryFragment";

	public static final String ARG_PHOTOS = "photos";
	public static final String ARG_PHOTO = "photo";
	public static final String ARG_TITLE = "title";

	private Gallery gallery;
	private GalleryAdapter adapter;

	private Animation rotateAnim;
	private ImageView imgLoader;
	private ImageButton btnComment;
	private ImageButton btnAddImg;
	private TextView tvTitle;

	private String title;
	private ArrayList<PhotoWrapper> photos;
	private int toLoadCount;
	private final AtomicInteger loadedCount = new AtomicInteger(0);

	@SuppressWarnings("unchecked")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Utils.d(TAG, "onAttach()");

		setRetainInstance(true);

		photos = (ArrayList<PhotoWrapper>) getArguments().getSerializable(ARG_PHOTOS);
		title = getArguments().getString(ARG_TITLE);
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

		btnComment = (ImageButton) view.findViewById(R.id.btn_comment);
		btnComment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

			}
		});

		btnAddImg = (ImageButton) view.findViewById(R.id.btn_add_image);
		btnAddImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

			}

		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Utils.d(TAG, "onActivityCreated()");

		setupState();
	}

	private void setupState() {
		if (photos == null || photos.isEmpty()) return;

		Bundle args = new Bundle();

		int size = photos.size();
		for (int i = 0; i < size; i++) {
			String id = photos.get(i).id;
			if (TextUtils.isEmpty(id) || id.contains(MiniGalleryAdapter.NEW_PHOTO)) continue;

			toLoadCount++;
			args.putString(ARG_PHOTO, id);
			int hashCode = id.hashCode();
			getLoaderManager().restartLoader(hashCode, args, this);
		}
	}

	@Override
	public Loader<Boolean> onCreateLoader(int id, final Bundle args) {
		Utils.d(TAG, "onCreateLoader(): id=" + id + ", args=" + args);

		loadedCount.incrementAndGet();
		toggleLoading(true);

		return new AsyncLoader<Boolean>(getActivity()) {
			@Override
			public Boolean loadInBackground() {
				FlickrService srv = FlickrService.getInstance();
				String photoId = args.getString(ARG_PHOTO);
				FlickrPhoto photo;
				try {
					photo = srv.parsePhoto(srv.getPhotoJson(photoId), FlickrService.SIZE_ORIGINAL);
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
					Utils.w(TAG, "loadInBackground(): exc=" + e);
				}
				return false;
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<Boolean> loader, Boolean result) {
		Utils.d(TAG, "onLoadFinished(): id=" + loader.getId());
		if (result == null || result == false) return;
		adapter.notifyDataSetChanged();

		if (loadedCount.get() == toLoadCount) {
			toggleLoading(false);
		}
	}

	@Override
	public void onLoaderReset(Loader<Boolean> loader) {
		Utils.d(TAG, "onLoaderReset(): id=" + loader.getId());
	}

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
