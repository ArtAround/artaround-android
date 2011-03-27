package us.artaround.android.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import us.artaround.R;
import us.artaround.android.ui.ArtMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

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

	public static final String KEY_CITY_CODE = "city_code";
	public static final String KEY_LAST_UPDATE = "last_update";
	public static final String KEY_UPDATE_INTERVAL = "update_interval";
	public static final String KEY_CLEARED_CACHE = "cleared_cache";
	public static final String KEY_SEND_CRASH_ONLINE = "send_crash_online";

	public static final String DATE_FORMAT = "yy-MM-dd'T'HH:mm:ss'Z'";
	public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(Utils.DATE_FORMAT);
	public static final SimpleDateFormat titleDateFormatter = new SimpleDateFormat("yyMMdd'_'HHmmss");

	public static final NumberFormat coordinateFormatter = NumberFormat.getInstance();
	{
		coordinateFormatter.setMaximumFractionDigits(4);
	}

	public static final int TIMEOUT = 30000; // 30 seconds

	public static final boolean DEBUG_MODE = true;

	public static String appVersion;

	//	private static final int OUTPUT_X = 800;
	//	private static final int OUTPUT_Y = 600;
	private static final int ASPECT_X = 1;
	private static final int ASPECT_Y = 1;
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

	public static String formatDate(Date date) {
		if (date == null) {
			return null;
		}
		return dateFormatter.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		if (TextUtils.isEmpty(date)) {
			return new Date();
		}
		return dateFormatter.parse(date);
	}

	public static Animation getRoateAnim(Context context) {
		Animation rotate = new RotateAnimation(0, 360, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(800);
		rotate.setInterpolator(new LinearInterpolator());
		rotate.setRepeatCount(Animation.INFINITE);
		return rotate;
	}

	public static void showToast(Context context, int msgId) {
		Toast.makeText(context, msgId, Toast.LENGTH_LONG).show();
	}

	public static void showToast(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public static void d(String tag, String message) {
		if (DEBUG_MODE) {
			Log.d(tag, message);
		}
	}

	public static void w(String tag, String message) {
		if (DEBUG_MODE) {
			Log.w(tag, message);
		}
	}

	public static void w(String tag, String message, Throwable e) {
		if (DEBUG_MODE) {
			Log.w(tag, message, e);
		}
	}

	public static void i(String tag, String message) {
		if (DEBUG_MODE) {
			Log.i(tag, message);
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
		//TODO enable online crash reporting
		//final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		//final boolean onlineDump = prefs.getBoolean(KEY_SEND_CRASH_ONLINE, true);

		if (!DEBUG_MODE) {
			return;
		}

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
		//intent.putExtra("outputX", OUTPUT_X);
		//intent.putExtra("outputY", OUTPUT_Y);
		intent.putExtra("scale", SCALE);
		intent.putExtra("noFaceDetection", !FACE_DETECTION);
		intent.putExtra("outputFormat", OUTPUT_FORMAT);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
		return intent;
	}

	public static Intent getStreetViewIntent(double latitude, double longitude) {
		// 1
		// yaw - direction you look
		// pitch - degrees from level where up is negative
		// zoom - is a zoom multiplier
		// mz - map zoom
		return new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("google.streetview:cbll=" + latitude + ","
				+ longitude + "&cbp=1,99.56,,1,-5.27&mz=21"));
	}

	public static Uri getNewPhotoUri() {
		String photoName = Utils.APP_NAME + "_" + Utils.titleDateFormatter.format(new Date()) + ".jpg";
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

	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also remove
	 * tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 * 
	 * @param text
	 *            The text, with the tokens, to adjust.
	 * @param token
	 *            The token string; there should be at least two instances of
	 *            token in text.
	 * @param cs
	 *            The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise the
	 *            second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 * 
	 * @see http
	 *      ://developer.android.com/reference/android/text/style/CharacterStyle
	 *      .html
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs) {
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1) {
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

}
