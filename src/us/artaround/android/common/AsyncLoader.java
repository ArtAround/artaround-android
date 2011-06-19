package us.artaround.android.common;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class AsyncLoader<D> extends AsyncTaskLoader<D> {
	private D data;

	public AsyncLoader(Context context) {
		super(context);
	}

	@Override
	public void deliverResult(D data) {
		if (isReset()) {
			// a query came in while the loader is stopped
			return;
		}

		this.data = data;

		super.deliverResult(data);
	}

	@Override
	protected void onStartLoading() {
		if (data != null) {
			deliverResult(data);
		}

		if (takeContentChanged() || data == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		// attempt to cancel the current load task if possible
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();

		// ensure the loader is stopped
		onStopLoading();

		data = null;
	}
}
