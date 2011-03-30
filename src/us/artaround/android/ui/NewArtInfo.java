package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import us.artaround.R;
import us.artaround.android.common.BackgroundCommand;
import us.artaround.android.common.LoadCategoriesCommand;
import us.artaround.android.common.LoadNeighborhoodsCommand;
import us.artaround.android.common.LoadingTask;
import us.artaround.android.common.LoadingTask.LoadingTaskCallback;
import us.artaround.android.common.LocationUpdater;
import us.artaround.android.common.LocationUpdater.LocationUpdaterCallback;
import us.artaround.android.common.NotifyingAsyncQueryHandler;
import us.artaround.android.common.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.common.SubmitArtCommand;
import us.artaround.android.common.UploadPhotoCommand;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Artist;
import us.artaround.models.City;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class NewArtInfo extends MapActivity implements NotifyingAsyncQueryListener, LoadingTaskCallback,
		LocationUpdaterCallback {

	protected static final int QUERY_CATEGORY = 0;
	protected static final int QUERY_NEIGHBORHOOD = 1;
	protected static final int QUERY_ARTIST = 2;

	protected static final int REQUEST_CODE_CAMERA = 0;
	protected static final int REQUEST_CODE_GALLERY = 1;
	protected static final int REQUEST_CODE_CROP_FROM_CAMERA = 2;

	protected static final int SUBMIT_ART_TOKEN = 100;
	protected static final int UPLOAD_PHOTO_TOKEN = 101;
	protected static final int LOAD_CATEGORIES_TOKEN = 102;
	protected static final int LOAD_NEIGHBORHOODS_TOKEN = 103;
	protected static final int LOAD_COMMENTS_TOKEN = 104;

	protected static final int DIALOG_PROGRESS = 0;

	protected static final int SWIPE_MIN_DISTANCE = 120;
	protected static final int SWIPE_MAX_OFF_PATH = 250;
	protected static final int SWIPE_THRESHOLD_VELOCITY = 200;

	//protected GestureDetector gestureDetector;
	protected Animation slideLeftIn;
	protected Animation slideLeftOut;
	protected Animation slideRightIn;
	protected Animation slideRightOut;
	protected ViewFlipper viewFlipper;

	protected ArtField[] fields;
	protected Animation rotateAnim;
	protected ImageView imgRefresh;
	protected ImageButton btnPrev, btnNext;
	protected Button btnSubmit, btnEdit, btnAdd;
	protected TextView tvAppTitle, tvFooter;
	protected MapView miniMap;
	protected CurrentOverlay currentOverlay;
	protected Gallery gallery;
	protected ImageView galleryDetail;

	protected NotifyingAsyncQueryHandler queryHandler;
	protected LocationUpdater locationUpdater;
	protected Location currentLocation;
	protected GeoPoint currentGeo;

	protected LoadingTask loadCategoriesTask, loadNeighborhoodsTask;
	protected LoadingTask submitArtTask;
	protected HashMap<String, LoadingTask> uploadPhotoTasks;
	protected String[] categories, neighborhoods, artists;
	protected City city;
	protected Art art;
	protected boolean isModeEditing;
	protected int currentPage = 0, pageCount = 3;

	protected Uri uploadTempPhotoUri;
	protected ArrayList<String> allUris, uploadPhotoUris;
	protected AtomicInteger leftToUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_info);

		// --- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		setupVars();
		setupUi();
		setupState();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder holder = new Holder();
		holder.loadCategoriesTask = loadCategoriesTask;
		holder.loadNeighborhoodsTask = loadCategoriesTask;
		holder.uploadPhotoTasks = uploadPhotoTasks;
		holder.submitArtTask = submitArtTask;
		holder.categories = categories;
		holder.neighborhoods = neighborhoods;
		holder.artists = artists;
		holder.isModeEditing = isModeEditing;
		holder.currentPage = currentPage;
		holder.currentLocation = currentLocation;
		holder.currentGeo = currentGeo;
		holder.leftToUpload = leftToUpload;
		holder.uploadPhotoUris = uploadPhotoUris;
		holder.allUris = allUris;
		return holder;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.new_art_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_photo:
			addPhoto();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getString(R.string.loading));
			dialog.setIndeterminate(true);
			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	protected void setupState() {
		Holder holder = (Holder) getLastNonConfigurationInstance();
		boolean isLoadingCategories = false, isLoadingNeighborhoods = false;

		if (holder != null) {
			loadCategoriesTask = holder.loadCategoriesTask;
			loadNeighborhoodsTask = holder.loadNeighborhoodsTask;
			uploadPhotoTasks = holder.uploadPhotoTasks;
			submitArtTask = holder.submitArtTask;
			categories = holder.categories;
			neighborhoods = holder.neighborhoods;
			artists = holder.artists;
			isModeEditing = holder.isModeEditing;
			currentPage = holder.currentPage;
			currentLocation = holder.currentLocation;
			currentGeo = holder.currentGeo;
			leftToUpload = holder.leftToUpload;
			uploadPhotoUris.addAll(holder.uploadPhotoUris);
			allUris.addAll(holder.allUris);

			setGallerySelection(0);

			if (loadCategoriesTask != null) {
				loadCategoriesTask.attachCallback(this);
				isLoadingCategories = true;
			}
			if (loadNeighborhoodsTask != null) {
				loadNeighborhoodsTask.attachCallback(this);
				isLoadingNeighborhoods = true;
			}

			if (uploadPhotoTasks != null) {
				for (String id : uploadPhotoTasks.keySet()) {
					uploadPhotoTasks.get(id).attachCallback(this);
				}
			}

			if (submitArtTask != null) {
				submitArtTask.attachCallback(this);
			}
		}
		else {
			leftToUpload = new AtomicInteger(0);
			uploadPhotoTasks = new HashMap<String, LoadingTask>();
		}

		if (!isLoadingCategories) {
			if (categories == null || categories.length == 0) {
				queryHandler.startQuery(QUERY_CATEGORY, null, Categories.CONTENT_URI,
						ArtAroundDatabase.CATEGORIES_PROJECTION, null, null, null);
			}
			else {
				fields[2].setAdapterItems(categories);
			}
		}

		if (!isLoadingNeighborhoods) {
			if (neighborhoods == null || neighborhoods.length == 0) {
				queryHandler.startQuery(QUERY_NEIGHBORHOOD, null, Neighborhoods.CONTENT_URI,
						ArtAroundDatabase.NEIGHBORHOODS_PROJECTION, null, null, null);
			}
			else {
				fields[3].setAdapterItems(neighborhoods);
			}
		}

		queryHandler.startQuery(QUERY_ARTIST, null, Artists.CONTENT_URI, ArtAroundDatabase.ARTISTS_PROJECTION, null,
				null, null);

		setModeEditing();
		setCurrentPage();

		if (currentLocation == null && currentGeo == null) {
			startLocationUpdate();
		}
	}

	protected void setupUi() {
		tvAppTitle = (TextView) findViewById(R.id.app_label);
		tvFooter = (TextView) findViewById(R.id.footer);

		imgRefresh = (ImageView) findViewById(R.id.img_refresh);
		rotateAnim = Utils.getRoateAnim(this);

		btnPrev = (ImageButton) findViewById(R.id.btn_prev);
		btnPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPrevPage();
			}
		});

		btnNext = (ImageButton) findViewById(R.id.btn_next);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showNextPage();
			}
		});

		btnEdit = (Button) findViewById(R.id.btn_edit);
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isModeEditing = !isModeEditing;
				setModeEditing();
			}
		});
		btnSubmit = (Button) findViewById(R.id.btn_submit);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitArt();
			}
		});

		btnAdd = (Button) findViewById(R.id.btn_add);
		btnAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addPhoto();
			}
		});

		viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
		slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
		slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
		slideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
		slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
		//gestureDetector = new GestureDetector(new MyGestureDetector());

		setupArtFields();
		setupMiniMap();
		setupGallery();
	}

	protected void setupMiniMap() {
		miniMap = (MapView) findViewById(R.id.mini_map);
		miniMap.setBuiltInZoomControls(false);
		miniMap.getController().setZoom(miniMap.getMaxZoomLevel() - 2);
	}

	protected void setupGallery() {
		galleryDetail = (ImageView) findViewById(R.id.gallery_imageView);
		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new GalleryAdapter(this, allUris));
		gallery.setEmptyView(findViewById(R.id.empty));
		gallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				galleryDetail.setImageURI(Uri.parse(allUris.get(position)));
			}
		});
		registerForContextMenu(gallery);
	}

	protected void setGallerySelection(int pos) {
		if (allUris.isEmpty()) return;
		((GalleryAdapter) gallery.getAdapter()).notifyDataSetChanged();
		galleryDetail.setImageURI(Uri.parse(allUris.get(pos)));
		gallery.setSelection(pos, true);
	}

	protected void changePageTitle(int page) {
		switch (page) {
		default:
		case 0:
			if (!TextUtils.isEmpty(art.title))
				tvAppTitle.setText(art.title);
			else
				tvAppTitle.setText(R.string.art_add_new);
			break;
		case 1:
			tvAppTitle.setText(R.string.art_minimap);
			break;
		case 2:
			tvAppTitle.setText(R.string.art_gallery);
			break;
		}
	}

	protected void setCurrentPage() {
		viewFlipper.setDisplayedChild(currentPage);
		changePageTitle(currentPage);
	}

	protected void switchPage() {
		currentPage = viewFlipper.indexOfChild(viewFlipper.getCurrentView());
		changePageTitle(currentPage);
	}

	protected void setupArtFields() {
		ArtField title = (ArtField) findViewById(R.id.title);
		title.setLabelText(getString(R.string.label_title));

		ArtField artist = (ArtField) findViewById(R.id.artist);
		artist.setLabelText(getString(R.string.label_artist));
		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) artist.setValueText(art.artist.name);

		ArtField category = (ArtField) findViewById(R.id.category);
		category.setLabelText(getString(R.string.label_category));
		if (!TextUtils.isEmpty(art.category)) category.setValueText(art.category);

		ArtField neighborhood = (ArtField) findViewById(R.id.neighborhood);
		neighborhood.setLabelText(getString(R.string.label_neighborhood));
		if (!TextUtils.isEmpty(art.neighborhood)) neighborhood.setValueText(art.neighborhood);

		ArtField year = (ArtField) findViewById(R.id.year);
		year.setLabelText(getString(R.string.label_year));
		if (art.year > 0) year.setValueText("" + art.year);

		ArtField ward = (ArtField) findViewById(R.id.ward);
		ward.setLabelText(getString(R.string.label_ward));
		if (art.ward > 0) ward.setValueText("" + art.ward);

		ArtField locationDesc = (ArtField) findViewById(R.id.location_description);
		locationDesc.setLabelText(getString(R.string.label_location_desc));
		if (!TextUtils.isEmpty(art.locationDesc)) locationDesc.setValueText(art.locationDesc);

		ArtField artDesc = (ArtField) findViewById(R.id.art_description);
		artDesc.setLabelText(getString(R.string.label_art_description));
		if (!TextUtils.isEmpty(art.description)) artDesc.setValueText(art.description);

		// FIXME cannot hide soft-keyboard if editing mode is true first?!
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(title.getWindowToken(), 0);

		fields = new ArtField[] { title, artist, category, neighborhood, year, ward, locationDesc, artDesc };
	}

	protected void setModeEditing() {
		for (int i = 0; i < fields.length; i++) {
			fields[i].setEditing(isModeEditing);
		}
		if (isModeEditing) {
			btnEdit.setText(R.string.cancel);
			btnSubmit.setVisibility(View.VISIBLE);
			btnAdd.setVisibility(View.VISIBLE);
		}
		else {
			btnEdit.setText(R.string.edit);
			btnSubmit.setVisibility(View.GONE);
			btnAdd.setVisibility(View.GONE);
		}
	}

	protected void addPhoto() {
		CharSequence[] items = new CharSequence[] { getString(R.string.media_source_camera),
				getString(R.string.media_source_gallery) };

		AlertDialog.Builder dialog = new AlertDialog.Builder(NewArtInfo.this)
				.setTitle(getString(R.string.media_source_title));
		dialog.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				uploadTempPhotoUri = Utils.getNewPhotoUri();
				if (uploadTempPhotoUri == null) {
					return;
				}

				switch (which) {
				case 0:
					final Intent iCamera = new Intent("android.media.action.IMAGE_CAPTURE");
					iCamera.putExtra(MediaStore.EXTRA_OUTPUT, uploadTempPhotoUri);
					startActivityForResult(iCamera, REQUEST_CODE_CAMERA);
					break;
				case 1:
					final Intent iGallery = new Intent(Intent.ACTION_PICK);
					iGallery.setType("image/*");
					iGallery.putExtra(MediaStore.EXTRA_OUTPUT, uploadTempPhotoUri);
					Utils.getCropImageIntent(iGallery, uploadTempPhotoUri);
					startActivityForResult(iGallery, REQUEST_CODE_GALLERY);
					break;
				}
			}
		});
		dialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			Utils.d(Utils.TAG, "Could not take/select photo! Activity result code is " + resultCode);
			return;
		}

		switch (requestCode) {
		case REQUEST_CODE_CAMERA:
			Intent intent = new Intent("com.android.camera.action.CROP");
			Uri uri = null;
			if (data != null && (uri = data.getData()) != null) {
				Utils.d(Utils.TAG, "Uri is " + uri.toString());
			}
			else {
				uri = uploadTempPhotoUri;
			}
			intent.setDataAndType(uri, "image/*");

			// start crop image activity
			Utils.getCropImageIntent(intent, uri);
			startActivityForResult(intent, REQUEST_CODE_CROP_FROM_CAMERA);
			break;
		case REQUEST_CODE_GALLERY:
		case REQUEST_CODE_CROP_FROM_CAMERA:
			// delete file in the end
			uploadPhotoUris.add(uploadTempPhotoUri.toString());
			allUris.add(uploadTempPhotoUri.toString());
			setGallerySelection(allUris.size() - 1);
			break;
		}
	}

	protected void updateArtFromFields() {
		// title, artist, category, neighborhood, year, ward, locationDesc, artDesc
		for (int i = 0; i < fields.length; i++) {
			String str = fields[i].getValue().getText().toString();
			if (str != null) {
				str = str.trim();
			}
			if (TextUtils.isEmpty(str)) continue;

			switch (i) {
			case 0:
				art.title = str;
				break;
			case 1:
				art.artist = new Artist(str, str);
				break;
			case 2:
				art.category = str;
				break;
			case 3:
				art.neighborhood = str;
				break;
			case 4:
				art.year = Integer.parseInt(str);
				break;
			case 5:
				art.ward = Integer.parseInt(str);
				break;
			case 6:
				art.locationDesc = str;
				break;
			case 7:
				art.description = str;
				break;
			}
		}
		art.latitude = currentOverlay.getCurrentLatitude();
		art.longitude = currentOverlay.getCurrentLongitude();
		art.city = city.name;
	}

	protected void submitArt() {
		updateArtFromFields();
		submitArtTask = (LoadingTask) new LoadingTask(this, new SubmitArtCommand(SUBMIT_ART_TOKEN, art)).execute();
	}

	protected void setupVars() {
		uploadPhotoUris = new ArrayList<String>();
		allUris = new ArrayList<String>();
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
		locationUpdater = new LocationUpdater(this);
		city = ServiceFactory.getCurrentCity();

		art = new Art();
		isModeEditing = true;

		Intent intent = getIntent();
		if (intent.hasExtra("location")) {
			currentLocation = intent.getParcelableExtra("location");
			if (currentLocation != null) {
				currentGeo = Utils.geo(currentLocation);
			}
		}
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		switch (token) {
		case QUERY_CATEGORY:
			List<String> result = ArtAroundDatabase.categoriesFromCursor(cursor);
			if (result.isEmpty()) {
				loadCategoriesTask = (LoadingTask) new LoadingTask(this, new LoadCategoriesCommand(
						LOAD_CATEGORIES_TOKEN)).execute();
			}
			else {
				categories = result.toArray(new String[result.size()]);
				fields[2].setAdapterItems(categories);
			}
			break;
		case QUERY_NEIGHBORHOOD:
			result = ArtAroundDatabase.neighborhoodsFromCursor(cursor);
			if (result.isEmpty()) {
				loadNeighborhoodsTask = (LoadingTask) new LoadingTask(this, new LoadNeighborhoodsCommand(
						LOAD_NEIGHBORHOODS_TOKEN)).execute();
			}
			else {
				neighborhoods = result.toArray(new String[result.size()]);
				fields[3].setAdapterItems(neighborhoods);
			}
			break;
		case QUERY_ARTIST:
			result = ArtAroundDatabase.artistsFromCursor(cursor);
			artists = result.toArray(new String[result.size()]);
			fields[1].setAdapterItems(artists);
			break;
		}
		if (cursor != null) {
			cursor.close();
		}
	}

	@Override
	public void beforeLoadingTask(BackgroundCommand command) {
		switch (command.getToken()) {
		case UPLOAD_PHOTO_TOKEN:
		case SUBMIT_ART_TOKEN:
			showDialog(DIALOG_PROGRESS);
			break;
		}
	}

	@Override
	public void afterLoadingTask(BackgroundCommand command, Object result) {
		switch (command.getToken()) {
		case LOAD_CATEGORIES_TOKEN:
			queryHandler.startQuery(QUERY_CATEGORY, null, Categories.CONTENT_URI,
					ArtAroundDatabase.CATEGORIES_PROJECTION, null, null, null);
			loadCategoriesTask = null;
			break;
		case LOAD_NEIGHBORHOODS_TOKEN:
			queryHandler.startQuery(QUERY_NEIGHBORHOOD, null, Neighborhoods.CONTENT_URI,
					ArtAroundDatabase.NEIGHBORHOODS_PROJECTION, null, null, null);
			loadNeighborhoodsTask = null;
			break;
		case UPLOAD_PHOTO_TOKEN:
			int left = leftToUpload.decrementAndGet();
			if (left == 0) {
				uploadPhotoUris.clear();
				dismissDialog(DIALOG_PROGRESS);
				Utils.showToast(this, R.string.submit_successful);
				finish();
			}
			else {
				uploadPhotoTasks.remove(command.getId());
				String filePath = Uri.parse(uploadPhotoUris.get(left - 1)).getPath();
				LoadingTask newTask = (LoadingTask) new LoadingTask(this, new UploadPhotoCommand(UPLOAD_PHOTO_TOKEN,
						filePath, art.slug, filePath)).execute();
				uploadPhotoTasks.put(filePath, newTask);
			}
			break;
		case SUBMIT_ART_TOKEN:
			submitArtTask = null;
			art.slug = (String) result;

			int size = uploadPhotoUris.size();
			leftToUpload.set(size);

			if (size > 0) {
				String filePath = Uri.parse(uploadPhotoUris.get(size - 1)).getPath();
				LoadingTask newTask = (LoadingTask) new LoadingTask(this, new UploadPhotoCommand(UPLOAD_PHOTO_TOKEN,
						filePath, art.slug, filePath)).execute();
				uploadPhotoTasks.put(filePath, newTask);
			}
			else {
				dismissDialog(DIALOG_PROGRESS);
				Utils.showToast(this, R.string.submit_successful);
				doFinish();
			}
			break;
		}
	}

	@Override
	public void onLoadingTaskError(BackgroundCommand command, ArtAroundException exception) {
		switch (command.getToken()) {
		case LOAD_CATEGORIES_TOKEN:
		case LOAD_NEIGHBORHOODS_TOKEN:
			Utils.d(Utils.TAG, "load categories and neighborhoods failure");
			break;
		case SUBMIT_ART_TOKEN:
		case UPLOAD_PHOTO_TOKEN:
			dismissDialog(DIALOG_PROGRESS);
			break;
		}
		// Utils.showToast(this, getString(R.string.load_data_failure));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationUpdater.removeUpdates();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	protected void showLoading(boolean loading) {
		if (loading) {
			imgRefresh.setVisibility(View.VISIBLE);
			imgRefresh.startAnimation(rotateAnim);
		}
		else {
			imgRefresh.clearAnimation();
			imgRefresh.setVisibility(View.INVISIBLE);
		}
	}

	protected void startLocationUpdate() {
		tvFooter.setText(R.string.waiting_location);
		locationUpdater.updateLocation();
		btnSubmit.setEnabled(false);
		showLoading(true);
	}

	@Override
	public void onSuggestLocationSettings() {
		this.tvFooter.setText(R.string.location_update_failure);
		currentGeo = city.center;
		addDraggablePin(currentGeo);
		showLoading(false);
	}

	@Override
	public void onLocationUpdate(Location location) {
		this.currentLocation = location;
		this.currentGeo = Utils.geo(location);
		updateCoordinates(location);
		addDraggablePin(currentGeo);
		btnSubmit.setEnabled(true);
		showLoading(false);
	}

	@Override
	public void onLocationUpdateError() {
		this.tvFooter.setText(getString(R.string.location_update_failure) + Utils.NL
				+ getString(R.string.reposition_pin));
		currentGeo = city.center;
		addDraggablePin(currentGeo);
		btnSubmit.setEnabled(true);
		showLoading(false);
	}

	protected void addDraggablePin(GeoPoint geo) {
		currentOverlay = new CurrentOverlay(this, R.drawable.ic_pin, geo, R.id.minimap_drag);
		miniMap.getOverlays().add(currentOverlay);
		miniMap.getController().animateTo(geo);
		miniMap.invalidate();
	}

	private void updateCoordinates(Location location) {
		String lat = Utils.coordinateFormatter.format(location.getLatitude());
		String longi = Utils.coordinateFormatter.format(location.getLongitude());
		tvFooter.setText(getString(R.string.reposition_pin) + ": " + lat + ", " + longi);
	}

	// FIXME make the 'fling' gesture detectable only on the action bar.
	// @Override
	// public boolean dispatchTouchEvent(MotionEvent ev) {
	// if (gestureDetector != null) {
	// gestureDetector.onTouchEvent(ev);
	// }
	// return super.dispatchTouchEvent(ev);
	// }

	//	@Override
	//	public boolean onTouchEvent(MotionEvent event) {
	//		if (gestureDetector.onTouchEvent(event))
	//			return true;
	//		else
	//			return false;
	//	}

	protected void showNextPage() {
		viewFlipper.setInAnimation(slideLeftIn);
		viewFlipper.setOutAnimation(slideLeftOut);
		viewFlipper.showNext();
		switchPage();
	}

	protected void showPrevPage() {
		viewFlipper.setInAnimation(slideRightIn);
		viewFlipper.setOutAnimation(slideRightOut);
		viewFlipper.showPrevious();
		switchPage();
	}

	//	protected class MyGestureDetector extends SimpleOnGestureListener {
	//		@Override
	//		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	//			try {
	//				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) return false;
	//				// right to left swipe
	//				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	//					viewFlipper.setInAnimation(slideLeftIn);
	//					viewFlipper.setOutAnimation(slideLeftOut);
	//					viewFlipper.showNext();
	//					switchPage();
	//				}
	//				else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	//					viewFlipper.setInAnimation(slideRightIn);
	//					viewFlipper.setOutAnimation(slideRightOut);
	//					viewFlipper.showPrevious();
	//					switchPage();
	//				}
	//			}
	//			catch (Exception e) {
	//				// nothing
	//			}
	//			return false;
	//		}
	//	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			doFinish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		doFinish();
		return;
	}

	private void doFinish() {
		Intent iHome = Utils.getHomeIntent(this);
		iHome.putExtras(getIntent()); // saved things from ArtMap
		startActivity(iHome);
		finish();
	}

	protected static class Holder {
		ArrayList<String> uploadPhotoUris, allUris;
		AtomicInteger leftToUpload;
		HashMap<String, LoadingTask> uploadPhotoTasks;
		LoadingTask submitArtTask;
		LoadingTask loadCategoriesTask, loadNeighborhoodsTask;
		String[] categories, neighborhoods, artists;
		Location currentLocation;
		GeoPoint currentGeo;
		int currentPage;
		boolean isModeEditing;
	}
}