package us.artaround.android.commons;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public class ImageDownloader {
	public static final String SDCARD_CACHE = "Android/data/us.artaround/cache/";

	private static File getFile(String name, String size) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
				|| Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			Utils.w(Utils.TAG, "Could not cache the image on the sdcard! Check sdcard status.");
			return null;
		}

		File storage = Environment.getExternalStorageDirectory();
		File dir = new File(storage.getAbsolutePath(), SDCARD_CACHE + "/" + size);
		dir.mkdirs();

		return new File(dir.getAbsolutePath(), name);
	}

	// should be called from within a background task, it performs a network call
	public static Uri getImage(Context context, String name, String size, String url) {
		Uri uri = quickGetImage(context, name, size);
		if (uri == null) {
			cacheImage(context, url, name, size);
			uri = quickGetImage(context, name, size);
		}
		Utils.i(Utils.TAG, "Fetched image " + name + " from cache.");
		return uri;
	}

	// will not make a network call, if file exists on disk
	public static Uri quickGetImage(Context context, String name, String size) {
		File imageFile = getFile(name, size);
		if (imageFile == null || !imageFile.exists())
			return null;
		else
			return Uri.parse(imageFile.getAbsolutePath());
	}

	public static void cacheImage(Context context, String url, String name, String size) {
		downloadFile(url, getFile(name, size));
	}

	public static void downloadFile(String url, File file) {
		try {
			if (file == null) {
				Utils.w(Utils.TAG, "Cannot download image: the file is null!");
				return;
			}
			Utils.d(Utils.TAG, "Downloading photo with url " + url);

			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			int contentLength = conn.getContentLength();

			if (contentLength < 0) {
				Utils.w(Utils.TAG, "Could not download file, probably bad connection.");
				return;
			}

			InputStream is = u.openStream();
			BufferedInputStream stream = new BufferedInputStream(is);

			ByteArrayBuffer bf = new ByteArrayBuffer(50);
			int current = 0;
			while ((current = stream.read()) != -1) {
				bf.append((byte) current);
			}

			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bf.toByteArray());

			fos.flush();
			fos.close();
			is.close();
		}
		catch (FileNotFoundException e) {
			Utils.w(Utils.TAG, "Could not download and/or cache image!", e);
			return; // swallow a 404
		}
		catch (IOException e) {
			Utils.w(Utils.TAG, "Could not download and/or cache image!", e);
			return; // swallow a 404
		}
	}
}
