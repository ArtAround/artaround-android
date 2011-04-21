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
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
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

public class MiniGalleryFragment extends Fragment implements LoaderCallbacks<PhotoWrapper> {

	private static final String TAG = "ArtAround.MiniGalleryFragment";

	public static final String ARG_TITLE = "title";
	public static final String ARG_PHOTOS = "photos";
	public static final String ARG_PHOTO = "photo";

	private static final int REQUEST_CODE_CAMERA = 0;
	private static final int REQUEST_CODE_GALLERY = 1;
	private static final int REQUEST_CODE_CROP_FROM_CAMERA = 2;

	private static final String DIALOG_ADD_PHOTO = "add_photo";

	private Gallery miniGallery;
	private MiniGalleryAdapter adapter;

	private String title;
	private ArrayList<String> photoIds;
	private ArrayList<PhotoWrapper> photos;
	private ArrayList<String> newPhotos;
	private Uri tempUri;
	private final AtomicInteger loadedCount = new AtomicInteger(0);

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Utils.d(TAG, "onAttach()");

		setRetainInstance(true);

		photoIds = getArguments().getStringArrayList(ARG_PHOTOS);
		title = getArguments().getString(ARG_TITLE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Utils.d(TAG, "onCreateView()");
		View view = inflater.inflate(R.layout.mini_gallery_fragment, container, false);
		miniGallery = (Gallery) view.findViewById(R.id.mini_gallery);
		registerForContextMenu(miniGallery);
		adapter = new MiniGalleryAdapter(getActivity(), true);
		miniGallery.setAdapter(adapter);
		miniGallery.setSelection(1);
		miniGallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (position == 1) {
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
		Utils.d(TAG, "onActivityCreated()");

		setupState();
	}

	private int getPhotoCount() {
		if (photoIds == null) return 0;
		return photoIds.size();
	}

	private void setupState() {
		int size = getPhotoCount();
		if (size == 0) {
			return;
		}
		photos = new ArrayList<PhotoWrapper>();
		newPhotos = new ArrayList<String>();

		// load photos from Flick or from cache (sdcard)
		LoaderManager lm = getLoaderManager();
		Resources res = getResources();
		Bundle args = new Bundle();
		args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_SMALL);
		args.putFloat(ImageDownloader.EXTRA_DENSITY, res.getDisplayMetrics().density);
		args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
		args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));

		for (int i = 0; i < size; ++i) {
			String id = photoIds.get(i);
			if (TextUtils.isEmpty(id)) {
				continue;
			}
			args.putString(ARG_PHOTO, id);

			int hashCode = id.hashCode();
			if (lm.getLoader(hashCode) == null) {
				lm.initLoader(hashCode, args, this);
			}
			else {
				lm.restartLoader(hashCode, args, this);
			}
		}
	}

	protected void gotoGallery() {
		Intent iGallery = new Intent(getActivity(), ArtGallery.class);
		iGallery.putExtra(ArtGallery.EXTRA_PHOTOS, photos);
		iGallery.putExtra(ArtGallery.EXTRA_TITLE, title);
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

		loadedCount.incrementAndGet();
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

		if (wrapper == null) {
			return;
		}

		if (loadedCount.get() == photoIds.size()) {
			adapter.hideLoaders();
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
		super.onActivityResult(requestCode, resultCode, data);

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
				uri = tempUri;
			}
			intent.setDataAndType(uri, "image/*");

			// start crop image activity
			Utils.getCropImageIntent(intent, uri);
			startActivityForResult(intent, REQUEST_CODE_CROP_FROM_CAMERA);
			break;
		case REQUEST_CODE_GALLERY:
		case REQUEST_CODE_CROP_FROM_CAMERA:
			// FIXME delete temp file after submitting
			String uriStr = tempUri.toString();
			newPhotos.add(uriStr);

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
					newPhotos.remove(newUri);
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
					f.tempUri = Utils.getNewPhotoUri();
					if (f.tempUri == null) {
						return;
					}

					switch (which) {
					case 0:
						final Intent iCamera = new Intent("android.media.action.IMAGE_CAPTURE");
						iCamera.putExtra(MediaStore.EXTRA_OUTPUT, f.tempUri);
						f.startActivityForResult(iCamera, REQUEST_CODE_CAMERA);
						break;
					case 1:
						final Intent iGallery = new Intent(Intent.ACTION_PICK);
						iGallery.setType("image/*");
						iGallery.putExtra(MediaStore.EXTRA_OUTPUT, f.tempUri);
						Utils.getCropImageIntent(iGallery, f.tempUri);
						f.startActivityForResult(iGallery, REQUEST_CODE_GALLERY);
						break;
					}
				}
			});
			return builder.create();
		}
	}
}
