package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class ArtComments extends ListActivity {
	private String artSlug;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_comments);

		artSlug = getIntent().getStringExtra("art_slug");
		setupVars();
		setupUi();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupState();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void setupState() {

			Utils.d(Utils.TAG, "Comments cursor is empty");

			new AsyncTask<Void, Void, Boolean>() {

				@Override
				protected Boolean doInBackground(Void... params) {
					try {
						ServiceFactory.getArtService().getComments(artSlug);
						return true;
					}
					catch (ArtAroundException e) {
						return false;
					}
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (!result) {
						Toast.makeText(ArtComments.this, R.string.load_data_failure, Toast.LENGTH_SHORT).show();
					}
					else {
						Utils.d(Utils.TAG, "Loaded comments finished.");
					}
				}
			}.execute();

	}

	private void setupVars() {

	}

	private void setupUi() {

	}
}
