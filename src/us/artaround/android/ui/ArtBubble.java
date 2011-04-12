package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.ImageDownloader;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.ArtAroundAsyncTask;
import us.artaround.android.common.task.ArtAroundAsyncTask.ArtAroundAsyncTaskListener;
import us.artaround.android.common.task.LoadFlickrPhotosCommand;
import us.artaround.android.services.FlickrService;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
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
	private final LinearLayout layout;
	private final TextView title;
	private final TextView author;
	private final TextView category;
	private final TextView description;
	private final ProgressBar progress;
	private final ImageView imgView;
	private final Context ctx;

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
		category = (TextView) rootView.findViewById(R.id.bubble_category);
		description = (TextView) rootView.findViewById(R.id.bubble_description);
		progress = (ProgressBar) rootView.findViewById(R.id.progress);
		imgView = (ImageView) rootView.findViewById(R.id.img_add_photo);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(layout, params);

	}

	public void setData(ArtOverlayItem item) {
		layout.setVisibility(VISIBLE);
		Art art = item.art;

		if (art.title != null) {
			title.setText(art.title);
			title.setVisibility(VISIBLE);
		}

		if (art.artist != null && !TextUtils.isEmpty(art.artist.name)) {
			StringBuilder bld = new StringBuilder(art.artist.name);
			if (art.year > 0) {
				bld.append(" - ").append(art.year);
			}
			author.setText(bld);
			author.setVisibility(View.VISIBLE);
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
			args.putBoolean(ImageDownloader.EXTRA_EXTRACT_THUMB, true);
			args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_ORIGINAL);
			args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
			args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));

			new ArtAroundAsyncTask(new LoadFlickrPhotosCommand(0, id, args), this).execute();
		}
	}

	public void clearData() {
		progress.setVisibility(View.VISIBLE);
		imgView.setVisibility(View.GONE);
		imgView.setImageURI(null);
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		progress.setVisibility(View.GONE);

		if (exception == null) {
			Uri uri = (Uri) result;
			if (uri != null) {
				imgView.setScaleType(ScaleType.FIT_XY);
				imgView.setImageURI(uri);
				imgView.setVisibility(View.VISIBLE);

			}
			else {
				imgView.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onPublishProgress(ArtAroundAsyncCommand command, Object progress) {
		// TODO Auto-generated method stub

	}
}
