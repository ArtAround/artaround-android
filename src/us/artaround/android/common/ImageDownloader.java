package us.artaround.android.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

public class ImageDownloader {
	private static final String TAG = "ArtAround.ImageDownloader";

	public static final String EXTRA_PHOTO_ID = "photo_id";
	public static final String EXTRA_PHOTO_URL = "photo_url";
	public static final String EXTRA_PHOTO_SIZE = "photo_size";

	public static final String EXTRA_DENSITY = "density";
	public static final String EXTRA_WIDTH = "width";
	public static final String EXTRA_HEIGHT = "height";

	public final static String IMAGE_FORMAT = ".png";

	public static int THUMB_WIDTH = 200;
	public static int THUMB_HEIGHT = 150;

	public static int PREVIEW_WIDTH = 100;
	public static int PREVIEW_HEIGHT = 100;

	public static Drawable getImageDrawable(String url) {
		return downloadImage(url);
	}

	// should be called from within a background task, it performs a network call
	public static Uri getImageUri(Bundle args) {
		String photoId = args.getString(EXTRA_PHOTO_ID);

		Uri uri = quickGetImageUri(photoId);
		if (uri == null) {
			downloadAndCacheImage(args);
			uri = quickGetImageUri(photoId);
			Utils.d(TAG, "Fetched image", photoId, " from server.");
		}
		else {
			Utils.d(TAG, "Fetched image", photoId, "from cache.");
		}
		return uri;
	}

	// will not make a network call, if file exists on disk
	private static Uri quickGetImageUri(String photoId) {
		File imageFile = getFile(photoId);
		if (imageFile == null || !imageFile.exists())
			return null;
		else
			return Uri.parse(imageFile.getAbsolutePath());
	}

	private static File getFile(String photoId) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
				|| Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			Utils.d(TAG, "Could not cache the image on the sdcard! Check sdcard status.");
			return null;
		}

		// TODO
		// split the photos into several directories (named using the first 3 chars in the photoId)
		// in order to avoid having too many nodes in a single directory
		File storage = Environment.getExternalStorageDirectory();
		File dir = new File(storage.getAbsolutePath(), Utils.APP_DIR + Utils.APP_DIR_CACHE);
		dir.mkdirs();

		return new File(dir.getAbsolutePath(), photoId + IMAGE_FORMAT);
	}

	private static void downloadAndCacheImage(Bundle args) {
		String photoId = args.getString(EXTRA_PHOTO_ID);
		String url = args.getString(EXTRA_PHOTO_URL);

		if (TextUtils.isEmpty(url)) {
			Utils.d(TAG, "Could not download and/or cache image! Url is null.");
			return;
		}

		BitmapDrawable photo = null;
		float density = args.getFloat(EXTRA_DENSITY);
		int width = args.getInt(EXTRA_WIDTH);
		int height = args.getInt(EXTRA_HEIGHT);

		AndroidHttpClient client = AndroidHttpClient.newInstance(Utils.USER_AGENT);
		HttpGet request = null;
		InputStream is = null;
		HttpEntity entity = null;

		try {
			request = new HttpGet((new URI(url)).toASCIIString());
			HttpResponse response = client.execute(request);

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Utils.d(TAG, "Could not download image! Status code", statusCode, "while retrieving bitmap from", url);
				return;
			}

			entity = response.getEntity();
			if (entity != null) {
				is = entity.getContent();
				photo = (BitmapDrawable) Drawable.createFromStream(new FlushedInputStream(is), null);

				if (photo != null) {
					int myWidth = (int) (density * density * photo.getIntrinsicWidth() + 0.5f);
					int myHeight = (int) (density * density * photo.getIntrinsicHeight() + 0.5f);

					if (myWidth > width || myHeight > height) {
						final float ratio = (float) myWidth / (float) myHeight;

						if (ratio > 1) {
							myWidth = width;
							myHeight = (int) (height / ratio);
						}
						else {
							myWidth = (int) (width * ratio);
							myHeight = height;
						}
					}
					photo.setBounds(0, 0, myWidth, myHeight);
					cacheImage(photoId, photo.getBitmap());
				}
			}
		}
		catch (Throwable e) {
			if (request != null) {
				request.abort();
			}
			Utils.d(TAG, "Could not download and/or cache image!", e);
		}
		finally {
			if (client != null) {
				client.close();
			}

			try {
				if (is != null) {
					is.close();
				}

				if (entity != null) {
					entity.consumeContent();
				}
			}
			catch (IOException e) {
				Utils.d(TAG, "Could not download and/or cache image!", e);
			}
		}
	}

	private static void cacheImage(String photoId, Bitmap bitmap) {
		try {
			File file = getFile(photoId);
			final FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.close();

			Utils.d(TAG, "Cached image with id ", photoId);
		}
		catch (final Throwable e) {
			Utils.d(TAG, "Could not download and/or cache image!", e);
		}
	}

	private static Drawable downloadImage(String url) {
		if (TextUtils.isEmpty(url)) {
			Utils.d(TAG, "Could not download image! Url is null.");
			return null;
		}

		AndroidHttpClient client = AndroidHttpClient.newInstance(Utils.USER_AGENT);
		HttpGet request = null;
		InputStream is = null;
		HttpEntity entity = null;

		try {
			request = new HttpGet((new URI(url)).toASCIIString());
			HttpResponse response = client.execute(request);

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Utils.d(TAG, "Could not download image! Status code", statusCode, "while retrieving bitmap from", url);
				return null;
			}

			entity = response.getEntity();
			if (entity != null) {
				is = entity.getContent();
				return Drawable.createFromStream(new FlushedInputStream(is), null);
			}
		}
		catch (Throwable e) {
			if (request != null) {
				request.abort();
			}
			Utils.d(TAG, "Could not download image!", e);
		}
		finally {
			if (client != null) {
				client.close();
			}

			try {
				if (is != null) {
					is.close();
				}

				if (entity != null) {
					entity.consumeContent();
				}
			}
			catch (IOException e) {
				Utils.d(TAG, "Could not download image!", e);
			}
		}
		return null;
	}

	// A bug in the previous versions of BitmapFactory.decodeStream may prevent it from working over a slow
	// connection. 
	//Decode a new FlushedInputStream(inputStream) instead to fix the problem.
	private static class FlushedInputStream extends FilterInputStream {
		private FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;

			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);

				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break; // we reached EOF
					}
					else {
						bytesSkipped = 1; // we read one byte
					}

					break;
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}
}
