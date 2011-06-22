package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Iterator;

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
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.Toast;

public class MiniGalleryFragment extends Fragment implements LoaderCallbacks<LoaderPayload> {
	private static final String TAG = "MiniGallery";

	private static final int REQUEST_CODE_CAMERA = 0;
	private static final int REQUEST_CODE_GALLERY = 1;
	private static final int REQUEST_CODE_CROP_FROM_CAMERA = 2;

	private static final int LOADER_PHOTO = 600;
	private static final String DIALOG_ADD_PHOTO = "dlg_add_photo";

	private Gallery miniGallery;
	private MiniGalleryAdapter adapter;

	private String artTitle;
	private boolean isEditMode;

	private ArrayList<PhotoWrapper> wrappers;
	private ArrayList<String> newPhotoUris;
	private Uri tempPhotoUri;
	private int toLoadCount;
	private int loadedCount;
	private String idToLoad;

	public MiniGalleryFragment() {}

	public MiniGalleryFragment(String artTitle, ArrayList<String> photoIds, boolean isEditMode) {
		this.artTitle = artTitle;
		this.isEditMode = isEditMode;

		wrappers = new ArrayList<PhotoWrapper>();
		newPhotoUris = new ArrayList<String>();

		if (photoIds != null) {
			int size = photoIds.size();

			for (int i = 0; i < size; ++i) {
				wrappers.add(new PhotoWrapper(photoIds.get(i)));
				toLoadCount++;
			}

			idToLoad = photoIds.get(0);
		}

		if (isEditMode) {
			wrappers.add(null);
		}

		Utils.d(TAG, "toLoadCount=", toLoadCount, "idToLoad=", idToLoad);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.mini_gallery_fragment, container, false);
		miniGallery = (Gallery) view.findViewById(R.id.mini_gallery);

		if (wrappers != null && wrappers.size() > 0) {
			registerForContextMenu(miniGallery);

			miniGallery.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					if (isEditMode && (position == wrappers.size() - 1)) {
						addNewPhoto();
					}
					else if (!isEditMode) {
						gotoGallery();
					}
				}
			});
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupState();
	}

	private void setupState() {
		if (adapter == null) {
			adapter = new MiniGalleryAdapter(getActivity(), isEditMode, wrappers);
		}
		else {
			adapter.setContext(getActivity());
		}
		miniGallery.setAdapter(adapter);
		miniGallery.setSelection(wrappers.size() > 1 ? 1 : 0);

		Utils.d(TAG, "setupState(): toLoadCount=", toLoadCount, "loadedCount=", loadedCount, "idToLoad=", idToLoad);

		if (loadedCount < toLoadCount && idToLoad != null) {
			adapter.setShowLoaders(true);

			Resources res = getResources();
			Bundle args = new Bundle();
			args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_SMALL);
			args.putFloat(ImageDownloader.EXTRA_DENSITY, res.getDisplayMetrics().density);
			args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
			args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));
			getLoaderManager().restartLoader(LOADER_PHOTO, args, this);
		}
	}

	protected void gotoGallery() {
		Intent iGallery = new Intent(getActivity(), ArtGallery.class);
		iGallery.putExtra(ArtGallery.EXTRA_WRAPPERS, wrappers);
		iGallery.putExtra(ArtGallery.EXTRA_TITLE, artTitle);
		iGallery.putExtra(ArtGallery.EXTRA_IS_EDIT_MODE, isEditMode);
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
	public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
		if (LOADER_PHOTO != id) return null;

		return new AsyncLoader<LoaderPayload>(getActivity()) {
			@Override
			public LoaderPayload loadInBackground() {
				try {
					Utils.d(TAG, "onCreateLoader(): loading photo with id=", idToLoad);

					Uri uri = ImageDownloader.quickGetImageUri(idToLoad);
					if (uri == null) {
						FlickrService srv = FlickrService.getInstance(getActivity());
						FlickrPhoto photo = srv.parsePhoto(srv.getPhotoJson(idToLoad),
								args.getString(ImageDownloader.EXTRA_PHOTO_SIZE));
						if (photo != null) {
							args.putString(ImageDownloader.EXTRA_PHOTO_ID, idToLoad);
							args.putString(ImageDownloader.EXTRA_PHOTO_URL, photo.url);
							uri = ImageDownloader.getImageUri(args);
						}
					}

					if (uri != null) {
						int size = wrappers.size();
						for (int i = 0; i < size; ++i) {
							if (wrappers.get(i) != null && idToLoad.equals(wrappers.get(i).id)) {
								wrappers.get(i).thumbUri = uri.toString();
								Utils.d(TAG, "loadInBackground(): found uri=", uri, "for photo with id=", idToLoad);
							}
						}
					}
					return new LoaderPayload(LoaderPayload.STATUS_OK, null, args);
				}
				catch (ArtAroundException e) {
					Toast.makeText(getActivity(), R.string.load_data_failure, Toast.LENGTH_SHORT).show();
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
				idToLoad = wrappers.get(loadedCount).id;
				getLoaderManager().restartLoader(LOADER_PHOTO, payload.getArgs(), this);
			}
		}
		else {
			adapter.setShowLoader(false, loadedCount - 1);
			Utils.d(TAG, "onLoadFinished(): could not load photo with id=", idToLoad, payload.getResult());
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<LoaderPayload> loader) {
		Utils.d(TAG, "onLoaderReset()");
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != FragmentActivity.RESULT_OK) {
			Utils.d(TAG, "onActivityResult(): could not take/select photo, result code is ", resultCode);
			return;
		}

		switch (requestCode) {
		case REQUEST_CODE_CAMERA:
			Intent intent = new Intent("com.android.camera.action.CROP");
			Uri uri = null;
			if (data != null && (uri = data.getData()) != null) {
				Utils.d(TAG, "onActivityResult(): uri=", uri.toString());
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
			String uriStr = tempPhotoUri.toString().replace("file://", "");
			newPhotoUris.add(uriStr);

			PhotoWrapper wrapper = new PhotoWrapper(newPhotoUri(uriStr));
			wrapper.thumbUri = uriStr;

			int size = wrappers.size();
			wrappers.add(null);
			wrappers.set(size - 1, wrapper);
			wrappers.set(size, wrappers.get(size));
			adapter.notifyDataSetChanged();

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
			if (id != null && id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {
				getActivity().getMenuInflater().inflate(R.menu.mini_gallery_menu, menu);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.context_remove_photo:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			if (info != null) {
				String id = (String) info.targetView.getTag();
				if (id != null && id.indexOf(MiniGalleryAdapter.NEW_PHOTO) > -1) {

					Iterator<PhotoWrapper> it = wrappers.iterator();
					while (it.hasNext()) {
						PhotoWrapper wrapper = it.next();
						if (wrapper != null && id.equals(wrapper.id)) {
							newPhotoUris.remove(wrapper.thumbUri);
							it.remove();
							adapter.notifyDataSetChanged();
							break;
						}
					}
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

	public ArrayList<PhotoWrapper> getwrappers() {
		return wrappers;
	}

	public void setwrappers(ArrayList<PhotoWrapper> wrappers) {
		this.wrappers = wrappers;
	}
}
