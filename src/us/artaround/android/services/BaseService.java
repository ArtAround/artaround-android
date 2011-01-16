package us.artaround.android.services;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import us.artaround.android.commons.Utils;
import us.artaround.android.parsers.StreamData;
import us.artaround.models.ArtAroundException;
import android.net.http.AndroidHttpClient;

public class BaseService {
	protected final String TAG = "ArtAround.BaseService";

	protected final char PARAM_START = '?';
	protected final char PARAM_ADD = '&';

	protected String addParams(String uri, String... params) {
		final StringBuilder uriBuilder = new StringBuilder(uri);

		if (params != null) {
			boolean isFirst = true;
			if (uri.indexOf(PARAM_START) != -1) {
				isFirst = false;
			}
			for (final String param : params) {
				if (param != null) {
					if (isFirst) {
						uriBuilder.append(PARAM_START);
						isFirst = false;
					}
					else {
						uriBuilder.append(PARAM_ADD);
					}
					uriBuilder.append(param);
				}
			}
		}
		return uriBuilder.toString();
	}

	protected void getMethod(StreamData data, String uri) throws ArtAroundException {
		final AndroidHttpClient client = getNewHttpClient();

		final HttpGet getRequest = new HttpGet(uri);
		//AndroidHttpClient.modifyRequestToAcceptGzipResponse(getRequest);

		try {
			HttpResponse response = client.execute(getRequest);
			parseHttpResponse(data, uri, response);
		}
		catch (IOException e) {
			throw new ArtAroundException(e);
		}
		finally {
			if (getRequest != null) {
				getRequest.abort();
			}
			if (client != null) {
				client.close();
			}
		}
	}

	protected AndroidHttpClient getNewHttpClient() {
		final AndroidHttpClient client = AndroidHttpClient.newInstance(Utils.USER_AGENT);

		HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(client.getParams(), HTTP.UTF_8);
		HttpProtocolParams.setUseExpectContinue(client.getParams(), false);

		client.getParams().setParameter("Accept", "application/json");
		client.getParams().setParameter("Content-type", "application/json");

		return client;
	}

	protected void parseHttpResponse(StreamData data, String uri, HttpResponse response) throws ArtAroundException {
		int statusCode = response.getStatusLine().getStatusCode();
		Utils.d(TAG, "parseHttpResponse(): status code is " + statusCode);

		try {
			switch (statusCode) {
			case HttpStatus.SC_OK:
				data.setHttpData(EntityUtils.toString(response.getEntity()));
				break;
			case HttpStatus.SC_NOT_FOUND:
				throw new ArtAroundException.NotFound("404 - Not Found for " + uri);
			default:
				throw new ArtAroundException("Bad status code " + statusCode + " for " + uri);
			}
		}
		catch (Exception e) {
			throw new ArtAroundException(e);
		}
	}
}
