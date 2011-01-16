package us.artaround.android.commons;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * @hide pending API council review
 */
public class NotifyingAsyncQueryHandler extends AsyncQueryHandler {
	private WeakReference<AsyncQueryListener> queryListener;
	private WeakReference<AsyncInsertListener> insertListener;
	private WeakReference<AsyncDeleteListener> deleteListener;
	private WeakReference<AsyncUpdateListener> updateListener;

	/**
	 * Interface to listen for completed sql operations.
	 */
	public interface QueryListener {}

	public interface AsyncQueryListener extends QueryListener {
		void onQueryComplete(int token, Object cookie, Cursor cursor);
	}

	public interface AsyncInsertListener extends QueryListener {
		void onInsertComplete(int token, Object cookie, Uri uri);
	}

	public interface AsyncDeleteListener extends QueryListener {
		void onDeleteComplete(int token, Object cookie, int result);
	}

	public interface AsyncUpdateListener extends QueryListener {
		void onUpdateComplete(int token, Object cookie, int result);
	}

	public NotifyingAsyncQueryHandler(ContentResolver resolver, QueryListener listener) {
		super(resolver);
		if (listener instanceof AsyncQueryListener) {
			setQueryListener((AsyncQueryListener) listener);
		}
		if (listener instanceof AsyncInsertListener) {
			setInsertListener((AsyncInsertListener) listener);
		}
		if (listener instanceof AsyncDeleteListener) {
			setDeleteListener((AsyncDeleteListener) listener);
		}
		if (listener instanceof AsyncUpdateListener) {
			setUpdateListener((AsyncUpdateListener) listener);
		}
	}

	/**
	 * Assign the given {@link AsyncQueryListener} to receive query events from
	 * asynchronous calls. Will replace any existing listener.
	 */
	public void setQueryListener(AsyncQueryListener listener) {
		queryListener = new WeakReference<AsyncQueryListener>(listener);
	}

	public void setInsertListener(AsyncInsertListener listener) {
		insertListener = new WeakReference<AsyncInsertListener>(listener);
	}

	public void setDeleteListener(AsyncDeleteListener listener) {
		deleteListener = new WeakReference<AsyncDeleteListener>(listener);
	}

	public void setUpdateListener(AsyncUpdateListener listener) {
		updateListener = new WeakReference<AsyncUpdateListener>(listener);
	}

	/**
	 * Clear any {@link AsyncQueryListener} set through
	 * {@link #setQueryListener(AsyncQueryListener)}
	 */
	public void clearQueryListener() {
		queryListener = null;
	}

	/**
	 * Begin an asynchronous query with the given arguments. When finished,
	 * {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)} is called
	 * if a valid {@link AsyncQueryListener} is present.
	 */
	public void startQuery(int token, Uri uri, String[] projection, String selection, String[] selectionArgs) {
		super.startQuery(token, null, uri, projection, selection, selectionArgs, null);
	}

	public void startQuery(int token, Uri uri, String[] proj) {
		super.startQuery(token, null, uri, proj, null, null, null);
	}

	public void startQuery(int token, Object cookie, Uri uri, String[] proj) {
		super.startQuery(token, cookie, uri, proj, null, null, null);
	}

	/**
	 * Begin an asynchronous update with the given arguments.
	 */
	public void startUpdate(Uri uri, ContentValues values) {
		startUpdate(-1, null, uri, values, null, null);
	}

	public void startInsert(Uri uri, ContentValues values) {
		startInsert(-1, null, uri, values);
	}

	public void startInsert(int token, Uri uri, ContentValues values) {
		startInsert(token, null, uri, values);
	}

	public void startDelete(Uri uri) {
		startDelete(-1, null, uri, null, null);
	}

	public void startDelete(Uri uri, Object cookie) {
		startDelete(-1, cookie, uri, null, null);
	}

	/** {@inheritDoc} */
	@Override
	protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
		final AsyncQueryListener listener = queryListener == null ? null : queryListener.get();
		if (listener != null) {
			listener.onQueryComplete(token, cookie, cursor);
		}
		else if (cursor != null) {
			cursor.close();
		}
	}

	@Override
	protected void onDeleteComplete(int token, Object cookie, int result) {
		final AsyncDeleteListener listener = deleteListener == null ? null : deleteListener.get();
		if (listener != null) {
			listener.onDeleteComplete(token, cookie, result);
		}
	}

	@Override
	protected void onInsertComplete(int token, Object cookie, Uri uri) {
		final AsyncInsertListener listener = insertListener == null ? null : insertListener.get();
		if (listener != null) {
			listener.onInsertComplete(token, cookie, uri);
		}
	}

	@Override
	protected void onUpdateComplete(int token, Object cookie, int result) {
		final AsyncUpdateListener listener = updateListener == null ? null : updateListener.get();
		if (listener != null) {
			listener.onUpdateComplete(token, cookie, result);
		}
	}

}
