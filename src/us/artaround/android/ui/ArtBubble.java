package us.artaround.android.ui;

import us.artaround.R;
import us.artaround.android.common.AsyncLoader;
import us.artaround.android.common.ImageDownloader;
import us.artaround.android.common.LoaderPayload;
import us.artaround.android.common.Utils;
import us.artaround.android.services.FlickrService;
import us.artaround.android.services.FlickrService.FlickrPhoto;
import us.artaround.models.Art;
import us.artaround.models.ArtAroundException;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Html;
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

public class ArtBubble extends FrameLayout implements LoaderCallbacks<LoaderPayload> {
	//private final static String TAG = "ArtBubble";

	private final static int TIMEOUT = 30000;

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
		imgView = (ImageView) rootView.findViewById(R.id.preview);

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

		StringBuilder str = new StringBuilder();
		if (art.artist != null && !TextUtils.isEmpty(art.artist)) {
			str.append(art.artist);

			if (art.year > 0) {
				str.append(" - ");
			}
		}

		if (art.year > 0) {
			str.append("<b>").append(art.year).append("</b>");
		}
		if (str.length() > 0) {
			author.setText(Html.fromHtml(str.toString()));
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
			args.putString(ImageDownloader.EXTRA_PHOTO_SIZE, FlickrService.SIZE_SMALL);
			args.putFloat(ImageDownloader.EXTRA_DENSITY, res.getDisplayMetrics().density);
			args.putInt(ImageDownloader.EXTRA_WIDTH, res.getDimensionPixelSize(R.dimen.GalleryItemWidth));
			args.putInt(ImageDownloader.EXTRA_HEIGHT, res.getDimensionPixelSize(R.dimen.GalleryItemHeight));

			((FragmentActivity) getContext()).getSupportLoaderManager().restartLoader(id.hashCode(), args, this);
			new CountDownTimer(TIMEOUT, 1) {
				@Override
				public void onTick(long millisUntilFinished) {}

				@Override
				public void onFinish() {
					progress.setVisibility(View.GONE);
				}
			}.start();
		}
	}

	public void clearData() {
		Utils.d(Utils.TAG, "clearData()");
		progress.setVisibility(View.VISIBLE);
		imgView.setVisibility(View.GONE);
		imgView.setImageURI(null);
	}

	@Override
	public Loader<LoaderPayload> onCreateLoader(int id, final Bundle args) {
		return new AsyncLoader<LoaderPayload>(getContext()) {

			@Override
			public LoaderPayload loadInBackground() {
				LoaderPayload payload = null;

				try {
					FlickrService srv = FlickrService.getInstance();
					FlickrPhoto photo = srv.parsePhoto(
							srv.getPhotoJson(args.getString(ImageDownloader.EXTRA_PHOTO_ID)),
							args.getString(ImageDownloader.EXTRA_PHOTO_SIZE));
					if (photo != null) {
						args.putString(ImageDownloader.EXTRA_PHOTO_URL, photo.url);
						payload = new LoaderPayload(LoaderPayload.STATUS_OK, ImageDownloader.getImageUri(args));
					}
				}
				catch (ArtAroundException e) {
					payload = new LoaderPayload(LoaderPayload.STATUS_ERROR, e.getMessage());
				}
				payload.setArgs(args);
				return payload;
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
		if (payload.getStatus() == LoaderPayload.STATUS_OK) {
			Uri uri = (Uri) payload.getResult();
			// if we show another bubble while the first one is not loaded yet
			// we need to show only the last task result
			if (loader.getId() == payload.getArgs().getString(ImageDownloader.EXTRA_PHOTO_ID).hashCode()) {
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
		else {
			//TODO error message
		}
		loader.stopLoading();
	}

	@Override
	public void onLoaderReset(Loader<LoaderPayload> loader) {}
}
