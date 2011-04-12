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
import android.media.ThumbnailUtils;
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

	public static final String EXTRA_EXTRACT_THUMB = "extract_thumb";
	public static final String EXTRA_EXTRACT_PREVIEW = "extract_preview";

	public static final String EXTRA_WIDTH = "thumb_width";
	public static final String EXTRA_HEIGHT = "thumb_height";

	public static final String THUMB_SUFFIX = "_thumb";
	public static final String PREVIEW_SUFFIX = "_preview";
	public final static String IMAGE_FORMAT = ".png";

	public static int THUMB_WIDTH = 200;
	public static int THUMB_HEIGHT = 150;

	public static int PREVIEW_WIDTH = 100;
	public static int PREVIEW_HEIGHT = 100;

	// should be called from within a background task, it performs a network call
	public static Uri getImage(Bundle args) {
		String photoId = args.getString(EXTRA_PHOTO_ID);
		String url = args.getString(EXTRA_PHOTO_URL);

		Uri uri = quickGetImage(photoId);
		if (uri == null) {
			downloadAndCacheImage(photoId, url, args);
			uri = quickGetImage(photoId);
			Utils.i(TAG, "Fetched image " + args + " from server uri=" + uri);
		}
		else {
			Utils.i(TAG, "Fetched image " + args + " from cache uri=" + uri);
		}
		return uri;
	}

	// will not make a network call, if file exists on disk
	public static Uri quickGetImage(String photoId) {
		File imageFile = getFile(photoId);
		if (imageFile == null || !imageFile.exists())
			return null;
		else
			return Uri.parse(imageFile.getAbsolutePath());
	}

	private static File getFile(String photoId) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
				|| Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			Utils.w(TAG, "Could not cache the image on the sdcard! Check sdcard status.");
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

	public static void downloadAndCacheImage(String photoId, String url, Bundle args) {
		if (TextUtils.isEmpty(url)) {
			Utils.w(TAG, "Could not download and/or cache image! Url is null.");
			return;
		}

		BitmapDrawable drawable = null;
		String originalId = photoId;

		// if it's a thumbnail, verify if the full-size picture exists already
		if (originalId.indexOf(THUMB_SUFFIX) > -1) {
			originalId = originalId.replace(THUMB_SUFFIX, "");
		}
		if (originalId.indexOf(PREVIEW_SUFFIX) > -1) {
			originalId = originalId.replace(PREVIEW_SUFFIX, "");
		}

		Uri originalUri = quickGetImage(photoId);
		if (originalUri != null) {
			drawable = (BitmapDrawable) Drawable.createFromPath(originalUri.toString());
		}
		else {
			// download the file
			AndroidHttpClient client = AndroidHttpClient.newInstance(Utils.USER_AGENT);
			HttpGet request = null;
			InputStream is = null;
			HttpEntity entity = null;

			try {
				request = new HttpGet((new URI(url)).toASCIIString());
				HttpResponse response = client.execute(request);

				final int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					Utils.w(TAG, "Could not download image! Status code " + statusCode
							+ " while retrieving bitmap from " + url);
					return;
				}

				entity = response.getEntity();
				if (entity != null) {
					is = entity.getContent();
					drawable = (BitmapDrawable) Drawable.createFromStream(new FlushedInputStream(is), null);
				}
			}
			catch (Throwable e) {
				if (request != null) {
					request.abort();
				}
				Utils.w(TAG, "Could not download and/or cache image!", e);
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
					Utils.w(TAG, "Could not download and/or cache image!", e);
				}
			}

			extractExtra(drawable, originalId, args, EXTRA_EXTRACT_PREVIEW);
			extractExtra(drawable, originalId, args, EXTRA_EXTRACT_THUMB);
		}
	}

	private static void extractExtra(BitmapDrawable drawable, String originalId, Bundle args, String type) {
		// type is THUMB or PREVIEW
		boolean extract = args.getBoolean(type, false);
		String suffix = type.equalsIgnoreCase(EXTRA_EXTRACT_THUMB) ? THUMB_SUFFIX : PREVIEW_SUFFIX;
		int width = args.getInt(EXTRA_WIDTH);
		int height = args.getInt(EXTRA_HEIGHT);

		if (extract && width > 0 && height > 0) {
			if (width > 0 && height > 0) {
				Bitmap bitmap = ThumbnailUtils.extractThumbnail(drawable.getBitmap(), width, height);
				cacheImage(originalId + suffix, bitmap);

				// save the original image when extracting a thumbnail
				if (type.equalsIgnoreCase(EXTRA_EXTRACT_THUMB)) {
					cacheImage(originalId, drawable.getBitmap());
				}
			}
			else {
				Utils.w(TAG, "Could not extract extra " + suffix + " from image!");
			}
		}
	}

	public static void cacheImage(String photoId, Bitmap bitmap) {
		try {
			File file = getFile(photoId);
			final FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.close();

			Utils.d(TAG, "Cached image with id " + photoId);
		}
		catch (final Throwable e) {
			Utils.w(TAG, "Could not download and/or cache image!", e);
		}
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
