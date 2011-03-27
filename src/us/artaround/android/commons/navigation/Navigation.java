package us.artaround.android.commons.navigation;

import java.net.HttpURLConnection;
import java.net.URL;

import us.artaround.android.commons.Utils;
import android.os.AsyncTask;

import com.google.android.maps.GeoPoint;

public class Navigation {

	public static final String TAG = "ArtAround.Navigation";

	public static final int TYPE_WALKING = 0;
	public static final int TYPE_DRIVING = 1;

	public static interface NavigationListener {
		void onNavigationAvailable(int type, Route route);

		void onNavigationUnavailable(GeoPoint startPoint, GeoPoint endPoint);
	}

	private static final GoogleKMLParser kmlParser = new GoogleKMLParser();

	private NavigationListener listener;
	private int navigationType;
	private boolean useMetric; // default is imperial (miles)

	public Navigation() {}

	public Navigation(boolean useMetric) {
		this.useMetric = useMetric;
	}

	public void navigateTo(GeoPoint startPoint, GeoPoint endPoint, int navigationType, NavigationListener listener) {
		if (startPoint == null || endPoint == null || listener == null) {
			throw new IllegalArgumentException("startPoint, endPoint and listener args can't be null!");
		}
		this.navigationType = navigationType;
		this.listener = listener;

		startNavigation(startPoint, endPoint, navigationType, listener);
	}

	private void startNavigation(GeoPoint startPoint, GeoPoint endPoint, int navigationType, NavigationListener listener) {
		new LoadRouteTask(startPoint, endPoint).execute(navigationType);
	}

	private void onNavigationAvailable(Route route) {
		if (listener != null) {
			listener.onNavigationAvailable(navigationType, route);
		}
	}

	private void onNavigationUnavailable(GeoPoint startPoint, GeoPoint endPoint) {
		if (listener != null) {
			listener.onNavigationUnavailable(endPoint, endPoint);
		}
	}

	private class LoadRouteTask extends AsyncTask<Integer, Void, Route> {
		private static final String MAPS_API_URL = "http://maps.google.com/maps?f=d&hl=en";

		private final GeoPoint startPoint;
		private final GeoPoint endPoint;

		public LoadRouteTask(GeoPoint startPoint, GeoPoint endPoint) {
			Utils.d(TAG, "Getting directions from " + startPoint + " to " + endPoint);
			this.startPoint = startPoint;
			this.endPoint = endPoint;
		}

		@Override
		protected Route doInBackground(Integer... params) {
			if (params == null || params.length == 0) {
				throw new IllegalArgumentException("You must provide a navigation type parameter!");
			}

			StringBuilder urlString = new StringBuilder();
			urlString.append(MAPS_API_URL).append("&saddr=").append(startPoint.getLatitudeE6() / 1E6).append(",")
					.append(startPoint.getLongitudeE6() / 1E6).append("&daddr=").append(endPoint.getLatitudeE6() / 1E6)
					.append(",").append(endPoint.getLongitudeE6() / 1E6).append("&ie=UTF8&0&om=0&output=kml");

			if (params[0] == TYPE_WALKING) {
				urlString.append("&dirflg=w");
			}
			if (useMetric) {
				urlString.append("&doflg=ptk");
			}
			else {
				urlString.append("&doflg=ptm");
			}

			Utils.d(TAG, "url=" + urlString.toString());

			Route route = null;
			try {
				URL url = new URL(urlString.toString());
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.connect();

				route = kmlParser.parseRoute(connection.getInputStream());
			}
			catch (Exception e) {
				route = null;
			}
			return route;
		}

		@Override
		protected void onPostExecute(Route route) {
			if (route != null) {
				onNavigationAvailable(route);
			}
			else {
				onNavigationUnavailable(startPoint, endPoint);
			}
		}
	}
}
