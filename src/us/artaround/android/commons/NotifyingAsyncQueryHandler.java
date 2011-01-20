package us.artaround.android.commons;

import java.lang.ref.WeakReference;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


/**
 * Slightly more abstract {@link AsyncQueryHandler} that helps keep a
 * {@link WeakReference} back to a listener. Will properly close any
 * {@link Cursor} if the listener ceases to exist.
 * <p>
 * This pattern can be used to perform background queries without leaking
 * {@link Context} objects.
 * 
 */
public class NotifyingAsyncQueryHandler extends AsyncQueryHandler {
	private final WeakReference<NotifyingAsyncListener> listener;

	/**
	 * Interface to listen for completed sql operations.
	 */
	private interface NotifyingAsyncListener {}

	public interface NotifyingAsyncQueryListener extends NotifyingAsyncListener {
		void onQueryComplete(int token, Object cookie, Cursor cursor);
	}

	public interface NotifyingAsyncInsertListener extends NotifyingAsyncListener {
		void onInsertComplete(int token, Object cookie, Uri uri);
	}

	public interface NotifyingAsyncDeleteListener extends NotifyingAsyncListener {
		void onDeleteComplete(int token, Object cookie, int result);
	}

	public interface NotifyingAsyncUpdateListener extends NotifyingAsyncListener {
		void onUpdateComplete(int token, Object cookie, int result);
	}

	public NotifyingAsyncQueryHandler(ContentResolver resolver, NotifyingAsyncListener listener) {
		super(resolver);
		this.listener = new WeakReference<NotifyingAsyncListener>(listener);
	}

	/**
	 * Clear any {@link QueryListener} set through
	 * {@link #setQueryListener(AsyncQueryListener)}
	 */
	public void clearQueryListener() {
		listener.clear();
	}

	/**
	 * Begin an asynchronous query with the given arguments. When finished,
	 * {@link QueryListener#onQueryComplete(int, Object, Cursor)} is called if a
	 * valid {@link QueryListener} is present.
	 */
	@Override
	public void startQuery(int token, Object cookie, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		super.startQuery(token, cookie, uri, projection, selection, selectionArgs, sortOrder);
	}

	/**
	 * Begin an asynchronous update with the given arguments.
	 */
	public void startUpdate(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		super.startUpdate(-1, null, uri, values, selection, selectionArgs);
	}

	public void startInsert(Uri uri, ContentValues values) {
		super.startInsert(-1, null, uri, values);
	}

	public void startDelete(Uri uri, String selection, String[] selectionArgs) {
		super.startDelete(-1, null, uri, selection, selectionArgs);
	}

	/** {@inheritDoc} */
	@Override
	protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
		final NotifyingAsyncQueryListener queryListener = (NotifyingAsyncQueryListener) (listener == null ? null
				: listener.get());
		if (queryListener != null) {
			queryListener.onQueryComplete(token, cookie, cursor);
		}
		else if (cursor != null) {
			cursor.close();
		}
	}

	@Override
	protected void onDeleteComplete(int token, Object cookie, int result) {
		if (!(listener instanceof NotifyingAsyncDeleteListener)) return;

		final NotifyingAsyncDeleteListener deleteListener = (NotifyingAsyncDeleteListener) (listener == null ? null
				: listener.get());
		if (deleteListener != null) {
			deleteListener.onDeleteComplete(token, cookie, result);
		}
	}

	@Override
	protected void onInsertComplete(int token, Object cookie, Uri uri) {
		if (!(listener instanceof NotifyingAsyncInsertListener)) return;

		final NotifyingAsyncInsertListener insertListener = (NotifyingAsyncInsertListener) (listener == null ? null
				: listener.get());
		if (insertListener != null) {
			insertListener.onInsertComplete(token, cookie, uri);
		}
	}

	@Override
	protected void onUpdateComplete(int token, Object cookie, int result) {
		if (!(listener instanceof NotifyingAsyncUpdateListener)) return;

		final NotifyingAsyncUpdateListener updateListener = (NotifyingAsyncUpdateListener) (listener == null ? null
				: listener.get());
		if (updateListener != null) {
			updateListener.onUpdateComplete(token, cookie, result);
		}
	}

}
