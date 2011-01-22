package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import android.app.Activity;
import android.os.Bundle;

public class ArtGallery extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_gallery);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		setupUi();
	}

	private void setupUi() {
		// TODO Auto-generated method stub

	}


}
