package us.artaround.android.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import us.artaround.android.services.ServiceFactory;
import android.net.http.AndroidHttpClient;
import android.os.Environment;

public class ArtAroundExceptionHandler implements UncaughtExceptionHandler {

	private final static String TAG = "ArtAround.ExceptionHandler";

	private final static String DUMP_DIR = "/dump";
	private final static String DUMP_EXT = ".txt";

	private final String POST_URL = "crash/log.json";
	private final String PARAM_DUMP = "dump";
	private final String PARAM_TIMESTAMP = "timestamp";
	private final String PARAM_APP_VERSION = "app_version";
	private final String PARAM_DEVICE = "device_name";
	private final String PARAM_OS_VERSION = "os_version";

	private static ArtAroundExceptionHandler instance;
	private Thread.UncaughtExceptionHandler previousHandler;

	private String filePath;
	private String appVersion;
	private final String osVersion;
	private final String deviceName;
	private boolean onlineDump;
	private boolean cardDump;

	public ArtAroundExceptionHandler(boolean cardDump, boolean onlineDump, String appVersion) {
		this.cardDump = cardDump;
		this.onlineDump = onlineDump;
		this.appVersion = appVersion;

		this.previousHandler = Thread.getDefaultUncaughtExceptionHandler();

		String dumpPath = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			dumpPath = Environment.getExternalStorageDirectory().getAbsolutePath();
			File dir = new File(dumpPath, Utils.APP_DIR + DUMP_DIR);
			dir.mkdirs();
			if (dir != null) {
				dumpPath = dir.getAbsolutePath();
			}
		}

		this.filePath = dumpPath;
		this.osVersion = android.os.Build.VERSION.RELEASE;
		this.deviceName = android.os.Build.MODEL;
	}

	public static ArtAroundExceptionHandler getInstance(boolean cardDump, boolean onlineDump, String appVersion) {
		if (instance == null) {
			instance = new ArtAroundExceptionHandler(cardDump, onlineDump, appVersion);
		}
		else {
			instance.cardDump = cardDump;
			instance.onlineDump = onlineDump;
			instance.appVersion = appVersion;
		}
		return instance;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Writer wr = new StringWriter();
		PrintWriter err = new PrintWriter(wr);
		ex.printStackTrace(err);

		final String stacktrace = wr.toString();
		err.close();

		final String timestamp = Utils.titleDateFormatter.format(new Date());

		if (cardDump && filePath != null) {
			dumpOnCard(timestamp, stacktrace);
		}

		if (onlineDump) {
			new Thread() {
				@Override
				public void run() {
					dumpOnServer(timestamp, stacktrace);
				}
			}.start();
		}

		previousHandler.uncaughtException(thread, ex);
	}

	private void dumpOnCard(final String timestamp, final String stacktrace) {
		String fileName = timestamp + DUMP_EXT;
		File file = new File(filePath + "/" + fileName);

		try {
			file.createNewFile();
			FileWriter filewriter = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(filewriter);
			bw.write("App Version:" + appVersion + "\n");
			bw.write("Device:" + deviceName + "\n");
			bw.write("Android OS ver:" + osVersion + "\n");
			bw.write(stacktrace);
			bw.flush();
			bw.close();
			Utils.d(TAG, "Writing crash file " + fileName);
		}
		catch (final IOException e) {
			Utils.w(TAG, TAG, e);
		}
	}

	private void dumpOnServer(String timestamp, String stacktrace) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_DUMP, stacktrace));
		params.add(new BasicNameValuePair(PARAM_TIMESTAMP, timestamp));
		params.add(new BasicNameValuePair(PARAM_APP_VERSION, appVersion));
		params.add(new BasicNameValuePair(PARAM_DEVICE, deviceName));
		params.add(new BasicNameValuePair(PARAM_OS_VERSION, osVersion));

		AndroidHttpClient client = AndroidHttpClient.newInstance(Utils.USER_AGENT);
		HttpPost postRequest = new HttpPost(ServiceFactory.getCurrentCity().serverUrl + POST_URL);

		try {
			postRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			client.execute(postRequest);
		}
		catch (UnsupportedEncodingException e) {
			Utils.w(TAG, TAG, e);
		}
		catch (IOException e) {
			Utils.w(TAG, TAG, e);
		}
		catch (OutOfMemoryError e) {
			Utils.w(TAG, TAG, e);
		}
		finally {
			if (postRequest != null) {
				postRequest.abort();
			}
			if (client != null) {
				client.close();
			}
		}
	}

}
