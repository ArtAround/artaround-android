package us.artaround.android.ui;

import java.util.List;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;
import us.artaround.models.Comment;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class ArtComments extends ListActivity {
	private String slug;
	private List<Comment> comments;
	private LoadCommentsTask loadTask;
	private LoadingButton btnRefresh;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_comments);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		slug = getIntent().getStringExtra("slug");
		setupActionBarUi();
		restoreState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupActionBarUi();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Holder h = new Holder();
		h.comments = comments;
		h.task = loadTask;
		return h;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (loadTask != null) {
			loadTask.detach();
		}
	}

	private void setupActionBarUi() {
		getParent().findViewById(R.id.btn_edit).setVisibility(View.GONE);

		btnRefresh = (LoadingButton) getParent().findViewById(R.id.btn_refresh);
		btnRefresh.setVisibility(View.VISIBLE);
		btnRefresh.setImageResource(R.drawable.ic_btn_refresh);
		btnRefresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadComments();
			}
		});
	}

	private void showLoading(boolean isLoading) {
		btnRefresh.showLoading(isLoading);
	}

	private void loadComments() {
		loadTask = (LoadCommentsTask) new LoadCommentsTask(this).execute();
	}

	private void restoreState() {
		Holder h = (Holder) getLastNonConfigurationInstance();
		if (h != null) {
			loadTask = h.task;
			comments = h.comments;

			if (loadTask != null) loadTask.attach(this);
		}
		else
			loadComments();
	}

	private static class LoadCommentsTask extends AsyncTask<Void, Void, List<Comment>> {
		ArtComments context;

		public LoadCommentsTask(ArtComments context) {
			this.context = context;
		}

		public void attach(ArtComments context) {
			this.context = context;
		}

		public void detach() {
			this.context = null;
		}

		@Override
		protected void onPreExecute() {
			context.showLoading(true);
		}

		@Override
		protected List<Comment> doInBackground(Void... params) {
			try {
				return ServiceFactory.getArtService().getComments(context.slug);
			}
			catch (ArtAroundException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<Comment> result) {
			context.showLoading(false);

			if (result == null) {
				Toast.makeText(context, R.string.load_data_failure, Toast.LENGTH_SHORT).show();
			}
			else {
				context.comments = result;
				Utils.d(Utils.TAG, "Loaded comments finished.");
			}
		}
	}

	private static class Holder {
		LoadCommentsTask task;
		List<Comment> comments;
	}

}
