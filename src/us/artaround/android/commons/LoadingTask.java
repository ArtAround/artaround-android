package us.artaround.android.commons;

import us.artaround.models.ArtAroundException;
import android.os.AsyncTask;

public class LoadingTask extends AsyncTask<Void, Void, Object> {

	public static interface LoadingTaskCallback {
		void beforeLoadingTask(BackgroundCommand command);

		void afterLoadingTask(BackgroundCommand command, Object result);

		void onLoadingTaskError(BackgroundCommand command, ArtAroundException exception);
	}

	private LoadingTaskCallback callback;
	private final BackgroundCommand command;
	private ArtAroundException exception;

	public LoadingTask(LoadingTaskCallback callback, BackgroundCommand command) {
		this.callback = callback;
		this.command = command;
	}

	public void attachCallback(LoadingTaskCallback callback) {
		this.callback = callback;
	}

	public void detachCallback() {
		this.callback = null;
	}

	@Override
	protected void onPreExecute() {
		callback.beforeLoadingTask(command);
	}

	@Override
	protected Object doInBackground(Void... params) {
		try {
			return command.execute();
		}
		catch (ArtAroundException e) {
			exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(Object result) {
		if (callback == null) return;

		if (exception != null) {
			callback.onLoadingTaskError(command, exception);
		}
		else {
			callback.afterLoadingTask(command, result);
		}
	}

}
