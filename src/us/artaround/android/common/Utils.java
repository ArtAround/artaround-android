package us.artaround.android.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import us.artaround.R;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.ui.ArtMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class Utils {
	public static final String TAG = "ArtAround";
	public static final String STR_SEP = ",";
	public static final String NL = "\n";
	public static final String DNL = "\n\n";
	public static final String USER_AGENT = "us.artaround";

	public static final String APP_DIR = "Android/data/us.artaround";
	public static final String APP_DIR_CACHE = "/cache";
	public static final String APP_NAME = "ArtAround";

	public static final long DEFAULT_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // one day
	public static final int DEFAULT_CITY_CODE = 0; // Washington, DC

	public static final int MINIMAP_ZOOM = 17;

	public static final String KEY_CITY_CODE = "city_code";
	public static final String KEY_LAST_UPDATE = "last_update";
	public static final String KEY_UPDATE_INTERVAL = "update_interval";
	public static final String KEY_CLEARED_CACHE = "cleared_cache";
	public static final String KEY_SEND_CRASH_ONLINE = "send_crash_online";
	public static final String KEY_CHECK_WIFI = "check_wifi";
	public static final String KEY_CHECK_LOCATION_PREFS = "check_location_prefs";
	public static final String KEY_SHOW_CHANGELOG = "show_changelog";
	public static final String KEY_SHOW_WELCOME = "show_welcome";
	public static final String KEY_VERSION = "version";

	public static final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yy-MM-dd'T'HH:mm:ss'Z'");
	public static final SimpleDateFormat utilDateFormatter = new SimpleDateFormat("yyMMdd'_'HHmmss");
	public static final SimpleDateFormat textDateFormatter = new SimpleDateFormat("MM.dd.yy");

	public static final DecimalFormat coordinateFormatter = new DecimalFormat();
	static {
		coordinateFormatter.setMinimumFractionDigits(4);
	}

	public static final int TIMEOUT = 30000; // 30 seconds

	public static final boolean DEBUG_MODE = false;

	public static String appVersion;

	public static final int THEME_DEFAULT = 0;
	private static int theme = THEME_DEFAULT;

	private static final int OUTPUT_X = 800;
	private static final int OUTPUT_Y = 600;
	private static final int ASPECT_X = 0;
	private static final int ASPECT_Y = 0;
	private static final boolean SCALE = true;
	private static final boolean FACE_DETECTION = true;
	private static final String OUTPUT_FORMAT = Bitmap.CompressFormat.JPEG.toString();

	public static int floatE6(float f) {
		return (int) ((int) f * 1E6);
	}

	public static GeoPoint geo(Location location) {
		return geo(location.getLatitude(), location.getLongitude());
	}

	public static GeoPoint geo(float latitude, float longitude) {
		return new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
	}

	public static GeoPoint geo(double latitude, double longitude) {
		return geo((float) latitude, (float) longitude);
	}

	public static Animation getRoateAnim(Context context) {
		Animation rotate = new RotateAnimation(0, 360, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(800);
		rotate.setInterpolator(new LinearInterpolator());
		rotate.setRepeatCount(Animation.INFINITE);
		return rotate;
	}

	private static String constructLog(Object... message) {
		if (message == null) return "";

		int size = message.length;
		StringBuilder bld = new StringBuilder();
		for (int i = 0; i < size; ++i)
			bld.append(message[i]).append(" ");

		return bld.toString();
	}

	public static void d(String tag, Object... message) {
		if (DEBUG_MODE) {
			Log.d(tag, constructLog(message));
		}
	}

	public static Intent getHomeIntent(Context context) {
		return new Intent(context, ArtMap.class);
	}

	public static InputStream getConnection(String url) {
		InputStream is = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			is = conn.getInputStream();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}

	public static boolean isCacheOutdated(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		Date lastUpdate = getLastCacheUpdate(prefs);
		if (lastUpdate == null) {
			return true;
		}
		return (new Date().getTime() - lastUpdate.getTime()) > prefs.getLong(Utils.KEY_UPDATE_INTERVAL,
				Utils.DEFAULT_UPDATE_INTERVAL);
	}

	public static Date getLastCacheUpdate(SharedPreferences prefs) {
		long time = prefs.getLong(Utils.KEY_LAST_UPDATE, 0);
		if (time == 0) {
			return null;
		}
		return new Date(time);
	}

	public static void setLastCacheUpdate(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		Editor editor = prefs.edit();
		editor.putLong(Utils.KEY_LAST_UPDATE, new Date().getTime());
		SharedPreferencesCompat.apply(editor);
	}

	public static AlertDialog getLocationSettingsDialog(final Activity context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.location_suggest_settings_title));
		builder.setMessage(context.getString(R.string.location_suggest_settings_msg));
		builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		});
		builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
	}

	public static void enableDump(Context context) {
		if (appVersion == null) {
			try {
				PackageManager pm = context.getPackageManager();
				appVersion = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
			}
			catch (final NameNotFoundException e) {
				appVersion = "0.0";
			}
		}
		Thread.setDefaultUncaughtExceptionHandler(ArtAroundExceptionHandler.getInstance(true, false, appVersion));
	}

	public static File getCacheFolder() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();

			File cacheFolder = new File(storagePath, APP_DIR + APP_DIR_CACHE);
			cacheFolder.mkdirs();
			return cacheFolder;
		}
		else
			return null;
	}

	public static Intent getCropImageIntent(Intent intent, Uri output) {
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", ASPECT_X);
		intent.putExtra("aspectY", ASPECT_Y);
		intent.putExtra("outputX", OUTPUT_X);
		intent.putExtra("outputY", OUTPUT_Y);
		intent.putExtra("scale", SCALE);
		intent.putExtra("noFaceDetection", !FACE_DETECTION);
		intent.putExtra("outputFormat", OUTPUT_FORMAT);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
		return intent;
	}

	public static Intent getStreetViewIntent(double latitude, double longitude) {
		//FIXME find better values for the params
		return new Intent(Intent.ACTION_VIEW, Uri.parse("google.streetview:cbll=" + latitude + "," + longitude
				+ "&cbp=11,0,0,0,0"));
	}

	public static Uri getNewPhotoUri() {
		String photoName = Utils.APP_NAME + "_" + Utils.utilDateFormatter.format(new Date()) + ".jpg";
		File cacheFolder = Utils.getCacheFolder();
		if (cacheFolder != null) {
			return Uri.fromFile(new File(cacheFolder, photoName));
		}
		return null;
	}

	public static Uri getCroppedPhotoUri() {
		String photoName = Utils.APP_NAME + "_cropped.jpg";
		File cacheFolder = Utils.getCacheFolder();
		if (cacheFolder != null) {
			return Uri.fromFile(new File(cacheFolder, photoName));
		}
		return null;
	}

	public static void setTheme(Activity activity) {
		switch (theme) {
		default:
		case THEME_DEFAULT:
			activity.getWindow().setFormat(PixelFormat.RGBA_8888);
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
			activity.setTheme(R.style.ArtAround_Default);
			break;
		}
	}

	public static void closeCursor(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	public static AlertDialog.Builder locationSettingsDialog(final Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(activity.getString(R.string.location_suggest_settings_title));
		builder.setMessage(activity.getString(R.string.location_suggest_settings_msg));
		builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		});
		builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder;
	}

	public static AlertDialog.Builder wifiSettingsDialog(final Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.connection_failure_title);
		builder.setMessage(R.string.connection_failure_msg);
		builder.setPositiveButton(R.string.connection_failure_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				activity.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
			}
		});
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder;
	}

	public static String formatCoords(Location location) {
		return formatCoords(location.getLatitude(), location.getLongitude());
	}

	public static String formatCoords(double latitude, double longitude) {
		return coordinateFormatter.format(latitude) + ", " + coordinateFormatter.format(longitude);
	}

	public static int dip(Context context, int dim) {
		return (int) (context.getResources().getDisplayMetrics().density * dim + 0.5f);
	}

	public static void setHintSpan(TextView textView, CharSequence hint) {
		SpannableString hintSpan = new SpannableString(textView.getHint());
		hintSpan.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, hintSpan.length(), 0);
		textView.setHint(hintSpan);
		textView.setHintTextColor(textView.getContext().getResources().getColor(R.color.HintColor));
	}

	// should be called on a background thread only
	public static void clearCache() {
		int count = ArtAroundProvider.contentResolver.delete(Arts.CONTENT_URI, null, null);
		Utils.d(Utils.TAG, "-> deleted arts=", count);

		count = ArtAroundProvider.contentResolver.delete(Artists.CONTENT_URI, null, null);
		Utils.d(Utils.TAG, "-> deleted artists=", count);

		count = ArtAroundProvider.contentResolver.delete(Categories.CONTENT_URI, null, null);
		Utils.d(Utils.TAG, "-> deleted categories=", count);

		count = ArtAroundProvider.contentResolver.delete(Neighborhoods.CONTENT_URI, null, null);
		Utils.d(Utils.TAG, "-> deleted neighborhoods=", count);
	}

	// should be called on a background thread only
	public static void deleteCachedFiles() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File storage = Environment.getExternalStorageDirectory();
			File dir = new File(storage.getAbsolutePath(), Utils.APP_DIR + Utils.APP_DIR_CACHE);
			dir.mkdirs();

			String[] fileNames = dir.list();
			if (fileNames != null) {
				int size = fileNames.length;

				if (size == 0) {
					Utils.d(Utils.TAG, "deleteCacheFiles(): no files to delete.");
					return;
				}

				for (int i = 0; i < size; ++i) {
					String fn = dir.getAbsolutePath() + "/" + fileNames[i];
					final File currentFile = new File(fn);
					if (currentFile != null) {
						try {
							boolean ok = currentFile.delete();
							Utils.d(Utils.TAG, "deleteCacheFiles(): deleted", fn, ok);
						}
						catch (final SecurityException e) {
							e.printStackTrace();
						}
					}
				}
			}
			else {
				Utils.d(Utils.TAG, "deleteCacheFiles(): no files to delete.");
			}
		}
	}

	// this is because of OutOfMemoryException with large images from sdcard
	public static Bitmap decodeBitmap(String path, ImageView imgView) {
		//imgView is passed to set it null to remove it from external memory
		imgView = null;

		try {
			InputStream stream = new FileInputStream(path);
			Bitmap bitmap = BitmapFactory.decodeStream(stream, null, null);
			stream.close();
			stream = null;
			return bitmap;
		}
		catch (Exception e) {
			return null;
		}
	}

}
