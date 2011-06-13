package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Iterator;
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;

//FIXME why this fragment doesn't save its own state?
public class MiniGalleryFragment extends Fragment implements LoaderCallbacks<PhotoWrapper> {
	private static final String TAG = "MiniGallery";

	public static final String ARG_EDIT_MODE = "edit_mode";
	public static final String ARG_TITLE = "title";
	public static final String ARG_PHOTOS = "photos";
	public static final String ARG_PHOTO = "photo";

	private static final String SAVE_PHOTOS = "photos";
	private static final String SAVE_NEW_PHOTO_URIS = "new_photo_uris";
	private static final String SAVE_LOADED_PHOTOS_COUNT = "loaded_photos_count";

	private static final int REQUEST_CODE_CAMERA = 0;
	private static final int REQUEST_CODE_GALLERY = 1;
	private static final int REQUEST_CODE_CROP_FROM_CAMERA = 2;

	private static final String DIALOG_ADD_PHOTO = "add_photo";

	private static final int TIMEOUT = 20000;

	private Gallery miniGallery;
	private MiniGalleryAdapter adapter;

	private String artTitle;
	private boolean isEditMode;
	private ArrayList<String> photoIds;

	private ArrayList<PhotoWrapper> photos;
	private ArrayList<String> newPhotoUris;
	private Uri tempPhotoUri;
	private AtomicInteger loadedPhotosCount;

	private MiniGallerySaver gallerySaver;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setRetainInstance(true);

		photoIds = getArguments().getStringArrayList(ARG_PHOTOS);
		isEditMode = getArguments().getBoolean(ARG_EDIT_MODE);
		artTitle = getArguments().getString(ARG_TITLE);

		if (activity instanceof MiniGallerySaver) {
			gallerySaver = (MiniGallerySaver) activity;
		}

		Utils.d(TAG, "onAttach()");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mini_gallery_fragment, container, false);

		adapter = new MiniGalleryAdapter(getActivity(), isEditMode);
		miniGallery = (Gallery) view.findViewById(R.id.mini_gallery);
		miniGallery.setAdapter(adapter);
		miniGallery.setSelection(1);
		registerForContextMenu(miniGallery);
		miniGallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (isEditMode && position == 1) {
					addNewPhoto();
				}
				else if (getPhotoCount() > 0) {
					gotoGallery();
				}
			}
		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Utils.d(TAG, "onActivityCreated(): savedInstanceState=" + savedInstanceState);
		//setupState(savedInstanceState);
		Bundle savedState = gallerySaver.restoreMiniGalleryState();
		setupState(savedState);
		Utils.d(TAG, "onActivityCreated(): savedState=" + savedState);
	}

	@Override
	public void onSaveInstanceState(Bundle args) {
		args.putSerializable(SAVE_PHOTOS, photos);
		args.putStringArrayList(SAVE_NEW_PHOTO_URIS, newPhotoUris);
		args.putInt(SAVE_LOADED_PHOTOS_COUNT, loadedPhotosCount.get());

		gallerySaver.saveMiniGalleryState(args);
		super.onSaveInstanceState(args);
	}

	private int getPhotoCount() {
		if (photoIds == null) return 0;
		return photoIds.size();
	}

	@SuppressWarnings("unchecked")
	private void setupState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			photos = (ArrayList<PhotoWrapper>) savedInstanceState.getSerializable(SAVE_PHOTOS);
			newPhotoUris = savedInstanceState.getStringArrayList(SAVE_NEW_PHOTO_URIS);
			loadedPhotosCount = new AtomicInteger(savedInstanceState.getInt(SAVE_LOADED_PHOTOS_COUNT, 0));

			adapter.addItems(photos);
		}
		else {
			photos = new ArrayList<PhotoWrapper>();
			newPhotoUris = new ArrayList<String>();
			loadedPhotosCount = new AtomicInteger(0);
		}

		int size = getPhotoCount();
		if (size == 0) {
			return;
		}

		// load photos from Flick or from cache (sdcard)
		adapter.toggleLoaders(true);
		new CountDownTimer(TIMEOUT, 1) {
			@Override
			public void onTick(long millisUntilFinished) {}

			@Override
			public void onFinish() {
				adapter.toggleLoaders(false);
			}
		}.start();

		Resources res = getResources();

		Bundle args = new Bundle();
		args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_SMALL);
		args.putFloat(ImageDownloader.EXTRA_DENSITY, res.getDisplayMetrics().density);
		args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
		args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));

		for (int i = 0; i < size; ++i) {
			String id = photoIds.get(i);
			if (TextUtils.isEmpty(id)) continue;

			args.putString(ARG_PHOTO, id);

			int hashCode = id.hashCode();
			getLoaderManager().restartLoader(hashCode, args, this);
		}
	}

	protected void gotoGallery() {
		Intent iGallery = new Intent(getActivity(), ArtGallery.class);
		iGallery.putExtra(ArtGallery.EXTRA_PHOTOS, photos);
		iGallery.putExtra(ArtGallery.EXTRA_TITLE, artTitle);
		startActivity(iGallery);
	}

	protected void addNewPhoto() {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prevDialog = null;
		if ((prevDialog = fm.findFragmentByTag(DIALOG_ADD_PHOTO)) != null) {
			ft.remove(prevDialog);
		}
		ft.addToBackStack(null);

		new AddPhotoDialog(this).show(ft, DIALOG_ADD_PHOTO);
	}

	@Override
	public Loader<PhotoWrapper> onCreateLoader(int id, final Bundle args) {
		Utils.d(TAG, "onCreateLoader(): id=" + id + ", args=" + args);

		loadedPhotosCount.incrementAndGet();
		miniGallery.setClickable(false);

		return new AsyncLoader<PhotoWrapper>(getActivity()) {
			@Override
			public PhotoWrapper loadInBackground() {
				FlickrService srv = FlickrService.getInstance();
				FlickrPhoto photo;
				try {
					String photoId = args.getString(ARG_PHOTO);
					photo = srv.parsePhoto(srv.getPhotoJson(photoId), args.getString(ImageDownloader.EXTRA_PHOTO_SIZE));

					if (photo != null) {
						args.putString(ImageDownloader.EXTRA_PHOTO_ID, photoId);
						args.putString(ImageDownloader.EXTRA_PHOTO_URL, photo.url);
						Uri uri = ImageDownloader.getImageUri(args);
						Utils.d(TAG, "onCreateLoader(): uri=" + uri.toString());
						return (uri != null) ? new PhotoWrapper(photoId, uri.toString()) : null;
					}
				}
				catch (ArtAroundException e) {
					Utils.w(TAG, "loadInBackground(): exc=" + e);
				}
				return null;
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<PhotoWrapper> loader, PhotoWrapper wrapper) {
		Utils.d(TAG, "onLoadFinished(): id=" + loader.getId());
		adapter.toggleLoaders(false);

		if (wrapper == null) {
			return;
		}

		if (loadedPhotosCount.get() == photoIds.size()) {
			miniGallery.setClickable(true);
		}
		if (wrapper.uri != null) {
			adapter.addItem(wrapper);
			photos.add(wrapper);
		}
	}

	@Override
	public void onLoaderReset(Loader<PhotoWrapper> loader) {
		Utils.d(TAG, "onLoaderReset(): id=" + loader.getId());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != FragmentActivity.RESULT_OK) {
			Utils.d(TAG, "Could not take/select photo! Activity result code is " + resultCode);
			return;
		}

		switch (requestCode) {
		case REQUEST_CODE_CAMERA:
			Intent intent = new Intent("com.android.camera.action.CROP");
			Uri uri = null;
			if (data != null && (uri = data.getData()) != null) {
				Utils.d(TAG, "Uri is " + uri.toString());
			}
			else {
				uri = tempPhotoUri;
			}
			intent.setDataAndType(uri, "image/*");

			// start crop image activity
			Utils.getCropImageIntent(intent, uri);
			startActivityForResult(intent, REQUEST_CODE_CROP_FROM_CAMERA);
			break;
		case REQUEST_CODE_GALLERY:
		case REQUEST_CODE_CROP_FROM_CAMERA:
			// FIXME delete temp file after submitting
			String uriStr = tempPhotoUri.toString();
			newPhotoUris.add(uriStr);

			PhotoWrapper wrapper = new PhotoWrapper(newPhotoUri(uriStr), uriStr);
			adapter.addItem(wrapper);
			photos.add(wrapper);
			break;
		}
	}

	private String newPhotoUri(String uri) {
		return uri + "_" + MiniGalleryAdapter.NEW_PHOTO;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		View view = miniGallery.getSelectedView();
		if (view != null) {
			String id = (String) view.getTag();
			if (!TextUtils.isEmpty(id) && id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.mini_gallery_menu, menu);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.context_remove_photo:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			if (info != null) {
				View view = info.targetView;
				String id = (String) view.getTag();
				if (!TextUtils.isEmpty(id) && id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {
					adapter.removeItem(id);

					String newUri = null;
					Iterator<PhotoWrapper> it = photos.iterator();
					while (it.hasNext()) {
						PhotoWrapper wrapper = it.next();
						if (id.equals(wrapper.id)) {
							newUri = wrapper.uri;
							it.remove();
						}
					}
					newPhotoUris.remove(newUri);
				}
			}
			break;
		}
		return super.onContextItemSelected(item);
	}

	private static class AddPhotoDialog extends DialogFragment {
		MiniGalleryFragment f;

		public AddPhotoDialog(MiniGalleryFragment f) {
			this.f = f;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			CharSequence[] items = new CharSequence[] { getString(R.string.art_edit_media_source_camera),
					getString(R.string.art_edit_media_source_gallery) };

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
					.setTitle(getString(R.string.art_edit_media_source));
			builder.setItems(items, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					f.tempPhotoUri = Utils.getNewPhotoUri();
					if (f.tempPhotoUri == null) {
						return;
					}

					switch (which) {
					case 0:
						final Intent iCamera = new Intent("android.media.action.IMAGE_CAPTURE");
						iCamera.putExtra(MediaStore.EXTRA_OUTPUT, f.tempPhotoUri);
						f.startActivityForResult(iCamera, REQUEST_CODE_CAMERA);
						break;
					case 1:
						final Intent iGallery = new Intent(Intent.ACTION_PICK);
						iGallery.setType("image/*");
						iGallery.putExtra(MediaStore.EXTRA_OUTPUT, f.tempPhotoUri);
						Utils.getCropImageIntent(iGallery, f.tempPhotoUri);
						f.startActivityForResult(iGallery, REQUEST_CODE_GALLERY);
						break;
					}
				}
			});
			return builder.create();
		}
	}

	public ArrayList<String> getNewPhotoUris() {
		return newPhotoUris;
	}

	public void setNewPhotoUris(ArrayList<String> newPhotoUris) {
		this.newPhotoUris = newPhotoUris;
	}

	public ArrayList<PhotoWrapper> getPhotos() {
		return photos;
	}

	public void setPhotos(ArrayList<PhotoWrapper> photos) {
		this.photos = photos;
	}

	public AtomicInteger getLoadedPhotosCount() {
		return loadedPhotosCount;
	}

	public void setLoadedPhotosCount(AtomicInteger loadedPhotosCount) {
		this.loadedPhotosCount = loadedPhotosCount;
	}
}
