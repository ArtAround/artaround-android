package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.ImageDownloader;
import us.artaround.android.common.Utils;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.ArtAroundAsyncTask;
import us.artaround.android.common.task.ArtAroundAsyncTask.ArtAroundAsyncTaskListener;
import us.artaround.android.common.task.LoadFlickrPhotoThumbCommand;
import us.artaround.android.services.FlickrService;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ArtBubble extends FrameLayout implements ArtAroundAsyncTaskListener {
	private final static String TAG = "ArtAround.ArtBubble";

	private final static int TIMEOUT = 30000;

	private final LinearLayout layout;
	private final TextView title;
	private final TextView year;
	private final TextView author;
	private final TextView category;
	private final TextView description;
	private final ProgressBar progress;
	private final ImageView imgView;
	private final Context ctx;

	private ArtAroundAsyncTask task;

	public ArtBubble(Context context, int bubbleBottomOffset) {
		super(context);
		ctx = context;

		setPadding(10, 0, 10, bubbleBottomOffset);
		layout = new LinearLayout(context);
		layout.setVisibility(VISIBLE);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rootView = inflater.inflate(R.layout.art_bubble, layout);
		title = (TextView) rootView.findViewById(R.id.bubble_title);
		author = (TextView) rootView.findViewById(R.id.bubble_author);
		year = (TextView) rootView.findViewById(R.id.bubble_year);
		category = (TextView) rootView.findViewById(R.id.bubble_category);
		description = (TextView) rootView.findViewById(R.id.bubble_description);
		progress = (ProgressBar) rootView.findViewById(R.id.progress);
		imgView = (ImageView) rootView.findViewById(R.id.preview);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(layout, params);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		task.cancel(true);
		super.onConfigurationChanged(newConfig);
	}

	public void setData(ArtOverlayItem item) {
		layout.setVisibility(VISIBLE);
		Art art = item.art;

		if (art.title != null) {
			title.setText(art.title);
			title.setVisibility(VISIBLE);
		}

		String str = null;
		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) {
			author.setText(art.artist.name);
			author.setVisibility(View.VISIBLE);
			str = " - ";
		}

		if (art.year > 0) {
			if (str != null) {
				str += art.year;
			}
			else {
				str = "" + art.year;
			}
			year.setText(str);
			year.setVisibility(View.VISIBLE);
		}

		if (!TextUtils.isEmpty(art.category)) {
			category.setText(art.category.toUpperCase());
			category.setVisibility(View.VISIBLE);
		}

		if (!TextUtils.isEmpty(art.locationDesc)) {
			description.setText(art.locationDesc);
			description.setVisibility(View.VISIBLE);
		}

		if (art.photoIds != null && art.photoIds.size() > 0) {
			String id = art.photoIds.get(0);
			Resources res = ctx.getResources();

			Bundle args = new Bundle();
			args.putString(ImageDownloader.EXTRA_PHOTO_ID, id);
			args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_SMALL);
			args.putFloat(ImageDownloader.EXTRA_DENSITY, res.getDisplayMetrics().density);
			args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
			args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));

			task = new ArtAroundAsyncTask(new LoadFlickrPhotoThumbCommand(0, id, args), this);
			task.execute();
			new CountDownTimer(TIMEOUT, 1) {
				@Override
				public void onTick(long millisUntilFinished) {}

				@Override
				public void onFinish() {
					progress.setVisibility(View.GONE);
				}
			}.start();
			Utils.d(TAG, "setData(): start task " + task.getCommandId());
		}
	}

	public void clearData() {
		Utils.d(TAG, "clearData()");
		progress.setVisibility(View.VISIBLE);
		imgView.setVisibility(View.GONE);
		imgView.setImageURI(null);
		if(task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
			task.cancel(true);
		}
	}


	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		if (exception == null) {
			Uri uri = (Uri) result;
			// if we show another bubble while the first one is not loaded yet
			// we need to show only the last task result
			if (task.getCommandId().equals(command.id)) {
				if (uri != null) {
					imgView.setScaleType(ScaleType.FIT_XY);
					imgView.setImageURI(uri);
					imgView.setVisibility(View.VISIBLE);
					progress.setVisibility(View.GONE);
				}
				else {
					imgView.setVisibility(View.GONE);
				}
			}
		}
		task = null;
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {}

	@Override
	public void onPublishProgress(ArtAroundAsyncCommand command, Object progress) {}
}
