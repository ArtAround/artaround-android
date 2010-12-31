package us.artaround.services;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import us.artaround.android.commons.Utils;
import us.artaround.models.ArtAroundException;
import android.text.TextUtils;

public abstract class BaseService {
	public static final String FORMAT = "json";
	public static final String BASE_URL = "http://theartaround.us/api/v1/";

	public static String extraHeaderKey = null;
	public static String extraHeaderValue = null;

	protected static String formUrl(String method, String query) {
		StringBuilder builder = new StringBuilder(BASE_URL).append(method).append(".").append(FORMAT);
		if (!TextUtils.isEmpty(query)) {
			builder.append("?").append(query);
		}
		return builder.toString();
	}

	protected static String getMethod(String url) throws ArtAroundException {
		HttpGet request = new HttpGet(url);
		request.addHeader("User-Agent", Utils.USER_AGENT);

		if (extraHeaderKey != null && extraHeaderValue != null) {
			request.addHeader(extraHeaderKey, extraHeaderValue);
		}

		try {
			HttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();

			switch (statusCode) {
			case HttpStatus.SC_OK:
				return EntityUtils.toString(response.getEntity());
			case HttpStatus.SC_NOT_FOUND:
				throw new ArtAroundException.NotFound("404 - Not Found from " + url);
			default:
				throw new ArtAroundException("Bad status code " + statusCode + " on fetching JSON from " + url);
			}
		} catch (ClientProtocolException e) {
			throw new ArtAroundException("Problem fetching JSON from " + url, e);
		} catch (IOException e) {
			throw new ArtAroundException("Problem fetching JSON from " + url, e);
		}
	}
}
