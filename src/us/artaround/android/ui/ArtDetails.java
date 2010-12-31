package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.List;

import us.artaround.R;
import us.artaround.android.commons.ImageDownloader;
import us.artaround.android.commons.Utils;
import us.artaround.android.commons.tasks.LoadArtPhotoTask;
import us.artaround.android.commons.tasks.LoadArtPhotoTask.LoadPhotoCallback;
import us.artaround.models.Art;
import us.artaround.models.Artist;
import us.artaround.services.ArtService;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;

public class ArtDetails extends Activity implements LoadPhotoCallback {
	private EditText[] editableFields;
	private ImageButton btnAdd, btnEdit, btnFavorite;
	private Gallery miniGallery;

	private Art art;
	private boolean isEditing = false;
	private int colorTxtEditing, colorTxtNormal;
	private int colorBgEditing, colorBgNormal;

	private ImageAdapter galleryAdapter;
	private List<LoadArtPhotoTask> loadPhotoTasks;
	private List<Uri> photoUris;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_details);

		art = (Art) getIntent().getExtras().getSerializable("art");
		Log.d(Utils.TAG, "Showing details about art " + art);

		loadPhotoTasks = new ArrayList<LoadArtPhotoTask>();
		photoUris = new ArrayList<Uri>();

		ArtService.init(getApplicationContext());

		setupUi();
		restoreLastKnownConfig();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (!loadPhotoTasks.isEmpty()) {
			for (LoadArtPhotoTask task : loadPhotoTasks) {
				task.detach();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder holder = new Holder();
		holder.tasks = loadPhotoTasks;
		holder.photoUris = photoUris;
		return holder;
	}

	private void restoreLastKnownConfig() {
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			loadPhotoTasks = holder.tasks;
			photoUris = holder.photoUris;

			for (LoadArtPhotoTask task : loadPhotoTasks) {
				task.attach(this);
			}
		}
		else {
			loadImages();
		}
	}

	private void setupUi() {
		setupActionBarUi();

		Resources res = getResources();
		colorTxtNormal = res.getColor(R.color.info_value);
		colorTxtEditing = res.getColor(R.color.info_value_editing_txt);

		colorBgNormal = res.getColor(R.color.info_bg);
		colorBgEditing = res.getColor(R.color.info_value_editing_bg);

		EditText artField = (EditText) findViewById(R.id.value_art);
		if (!TextUtils.isEmpty(art.title)) artField.setText(art.title);

		EditText artistField = (EditText) findViewById(R.id.value_artist);
		Artist artist = art.artist;
		if (artist != null && !TextUtils.isEmpty(artist.name)) artistField.setText(artist.name);

		EditText yearField = (EditText) findViewById(R.id.value_year);
		if (art.createdAt != null) yearField.setText("" + (art.createdAt.getYear() + 1900));

		EditText locationField = (EditText) findViewById(R.id.value_location);
		if (art.locationDesc != null) locationField.setText(art.locationDesc);

		editableFields = new EditText[] { artField, artistField, yearField, locationField };

		int size = editableFields.length;
		for (int i = 0; i < size; i++)
			editableFields[i].setFocusable(false);

		miniGallery = (Gallery) findViewById(R.id.mini_gallery);
		galleryAdapter = new ImageAdapter(this, photoUris);
		miniGallery.setAdapter(galleryAdapter);
		miniGallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (art.photoIds != null && art.photoIds.length > 0) {
					Intent iGallery = new Intent(ArtDetails.this, ArtGallery.class);
					iGallery.putExtra("photoIds", art.photoIds);
					startActivity(iGallery);
				}
			}
		});
	}

	private void loadImages() {
		String[] photoIds = art.photoIds;
		if (photoIds == null || photoIds.length == 0) {
			return;
		}

		int length = photoIds.length;
		Log.d(Utils.TAG, "Loading " + length + " images...");

		for (int i = 0; i < length; i++) {
			String id = photoIds[i];
			Log.d(Utils.TAG, "Photo id = " + id);

			String smallSize = ArtService.getImageSmallSize();
			Uri photoUri = ImageDownloader.quickGetImage(this, ArtService.getImageName(id), smallSize);

			if (photoUri != null) {
				addPhotoToGallery(photoUri);
			}
			else {
				loadPhotoTasks.add((LoadArtPhotoTask) new LoadArtPhotoTask(this, id).execute(smallSize));
			}
		}
	}

	private void setupActionBarUi() {
		btnAdd = (ImageButton) findViewById(R.id.btn_1);
		btnAdd.setImageResource(R.drawable.ic_btn_add);
		btnAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doAddArt();
			}
		});
		btnEdit = (ImageButton) findViewById(R.id.btn_2);
		btnEdit.setImageResource(R.drawable.ic_btn_edit);
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doEditArt();
			}
		});
		btnFavorite = (ImageButton) findViewById(R.id.btn_3);
		btnFavorite.setImageResource(R.drawable.ic_btn_star_normal);
		btnFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doFavoriteArt();
			}
		});
	}

	protected void doFavoriteArt() {
		// TODO Auto-generated method stub

	}

	protected void doEditArt() {
		isEditing = !isEditing;

		btnEdit.setImageResource(isEditing ? R.drawable.ic_btn_save : R.drawable.ic_btn_edit);

		int size = editableFields.length;
		for (int i = 0; i < size; i++) {
			EditText field = editableFields[i];
			if (isEditing) {
				field.setFocusableInTouchMode(true);
			}
			else {
				field.setFocusable(false);
			}
			field.setCursorVisible(isEditing);
			field.setBackgroundColor(isEditing ? colorBgEditing : colorBgNormal);
			field.setTextColor(isEditing ? colorTxtEditing : colorTxtNormal);
		}
	}

	protected void doAddArt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoadPhoto(String id, Uri photoUri, LoadArtPhotoTask task) {
		Log.d(Utils.TAG, "Added photo uri " + photoUri);
		addPhotoToGallery(photoUri);
		loadPhotoTasks.remove(task);
	}

	private void addPhotoToGallery(Uri photoUri) {
		photoUris.add(photoUri);
		galleryAdapter.notifyDataSetChanged();
	}

	private static class Holder {
		List<LoadArtPhotoTask> tasks;
		List<Uri> photoUris;
	}
}
