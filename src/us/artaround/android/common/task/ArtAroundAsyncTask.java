package us.artaround.android.common.task;


import java.lang.ref.WeakReference;

import us.artaround.models.ArtAroundException;
import android.os.AsyncTask;


public class ArtAroundAsyncTask extends AsyncTask<Void, Object, Object> {

    public static interface ArtAroundAsyncTaskListener {
        void onPreExecute(ArtAroundAsyncCommand command);

        void onPostExecute(ArtAroundAsyncCommand command, Object result,
                           ArtAroundException exception);

        void onPublishProgress(ArtAroundAsyncCommand command, Object progress);
    }

    private final ArtAroundAsyncCommand command;
    private WeakReference<ArtAroundAsyncTaskListener> listener;
    private ArtAroundException exception;

    public ArtAroundAsyncTask(ArtAroundAsyncCommand command, ArtAroundAsyncTaskListener listener) {
		this.command = command;
		this.listener = new WeakReference<ArtAroundAsyncTaskListener>(listener);
    }

    public void changeListener(ArtAroundAsyncTaskListener listener) {
		this.listener = new WeakReference<ArtAroundAsyncTaskListener>(listener);
    }

    public void clearListener() {
        if (listener != null) {
            listener.clear();
        }
    }

    @Override
    protected Object doInBackground(Void... params) {
        if (command != null) {
            try {
                return command.execute();
            } catch (ArtAroundException e) {
                exception = e;
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (listener == null) {
            return;
        }
        ArtAroundAsyncTaskListener ref = listener.get();
        if (ref != null) {
            ref.onPreExecute(command);
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        if (listener == null) {
            return;
        }
        ArtAroundAsyncTaskListener ref = listener.get();
        if (ref != null) {
            ref.onPostExecute(command, result, exception);
        }
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        if (listener == null) {
            return;
        }
        ArtAroundAsyncTaskListener ref = listener.get();
        if (ref != null) {
            ref.onPublishProgress(command, values);
        }
    }

    public String getCommandId() {
		return (command == null) ? null : command.id;
	}

	public int getTokenId() {
		return (command == null) ? -1 : command.token;
    }

}
