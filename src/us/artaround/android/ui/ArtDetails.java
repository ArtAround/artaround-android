package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ArtDetails extends Activity {

	Art art = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_details);

		this.art = getArt(getIntent().getStringExtra(Utils.KEY_ART_ID));
		setupUi();
	}

	private void setupUi() {
		TextView nameView = (TextView) findViewById(R.id.txt_name);
		nameView.setText(art.title);
	}

	private Art getArt(String artId) {
		Log.e(Utils.TAG, "artId: " + artId);
		if (artId == null) {
			return null;
		}
		// TODO Auto-generated method stub
		Art a = new Art();
		a.title = "The Court of Neptune";
		return a;
	}

}
