package us.artaround.android.commons;

import us.artaround.models.ArtAroundException;
import android.os.AsyncTask;

public class LoadingTask<Result> extends AsyncTask<Void, Void, Result> {

	public static interface LoadingTaskCallback<Result> {
		void beforeLoadingTask(int token);

		void afterLoadingTask(int token, Result result);

		void onLoadingTaskError(int token, ArtAroundException exception);
	}

	private LoadingTaskCallback<Result> callback;
	private final ServerCallCommand command;
	private ArtAroundException exception;

	public LoadingTask(LoadingTaskCallback<Result> callback, ServerCallCommand command) {
		this.callback = callback;
		this.command = command;
	}

	public void attachCallback(LoadingTaskCallback<Result> callback) {
		this.callback = callback;
	}

	public void detachCallback() {
		this.callback = null;
	}

	@Override
	protected void onPreExecute() {
		callback.beforeLoadingTask(command.getToken());
	}

	@Override
	protected Result doInBackground(Void... params) {
		try {
			return command.execute();
		}
		catch (ArtAroundException e) {
			exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute(Result result) {
		if (callback == null) return;

		if (exception != null) {
			callback.onLoadingTaskError(command.getToken(), exception);
		}
		else {
			callback.afterLoadingTask(command.getToken(), result);
		}
	}

}
