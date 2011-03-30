package us.artaround.android.common;

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
	public void startQuery(int token, Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		super.startQuery(token, null, uri, projection, selection, selectionArgs, sortOrder);
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
		NotifyingAsyncListener l = listener.get();
		if (l == null || !(l instanceof NotifyingAsyncQueryListener)) {
			if (cursor != null) {
				cursor.close();
			}
			return;
		}

		((NotifyingAsyncQueryListener) l).onQueryComplete(token, cookie, cursor);
	}

	@Override
	protected void onDeleteComplete(int token, Object cookie, int result) {
		NotifyingAsyncListener l = listener.get();
		if (l == null || !(l instanceof NotifyingAsyncDeleteListener)) return;
		((NotifyingAsyncDeleteListener) l).onDeleteComplete(token, cookie, result);
	}

	@Override
	protected void onInsertComplete(int token, Object cookie, Uri uri) {
		NotifyingAsyncListener l = listener.get();
		if (l == null || !(l instanceof NotifyingAsyncInsertListener)) return;
		((NotifyingAsyncInsertListener) l).onInsertComplete(token, cookie, uri);
	}

	@Override
	protected void onUpdateComplete(int token, Object cookie, int result) {
		NotifyingAsyncListener l = listener.get();
		if (l == null || !(l instanceof NotifyingAsyncUpdateListener)) return;
		((NotifyingAsyncUpdateListener) l).onUpdateComplete(token, cookie, result);
	}
}
