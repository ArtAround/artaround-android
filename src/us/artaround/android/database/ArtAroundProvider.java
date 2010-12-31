package us.artaround.android.database;

import static us.artaround.android.database.ArtAroundDb.ARTAROUND_AUTHORITY;
import static us.artaround.android.database.ArtAroundDb.createArtistsTable;
import static us.artaround.android.database.ArtAroundDb.createArtsTable;
import static us.artaround.android.database.ArtAroundDb.createCategoriesTable;
import static us.artaround.android.database.ArtAroundDb.createFiltersTable;
import static us.artaround.android.database.ArtAroundDb.createNeighborhoodsTable;

import java.util.HashMap;

import us.artaround.R;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDb.Artists;
import us.artaround.android.database.ArtAroundDb.Arts;
import us.artaround.android.database.ArtAroundDb.Categories;
import us.artaround.android.database.ArtAroundDb.Filters;
import us.artaround.android.database.ArtAroundDb.Neighborhoods;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class ArtAroundProvider extends ContentProvider {
	private static final String DATABASE_NAME = "artaround.db";
	private static final int DATABASE_VERSION = 1;

	private static HashMap<String, String> artsMap;
	private static HashMap<String, String> artistsMap;
	private static HashMap<String, String> categoriesMap;
	private static HashMap<String, String> neighborhoodsMap;
	private static HashMap<String, String> filtersMap;
	
	private static final int ARTS = 0;
	private static final int ART_ID = 1;
	private static final int ARTISTS = 2;
	private static final int ARTIST_ID = 3;
	private static final int CATEGORIES = 4;
	private static final int NEIGHBORHOODS = 5;
	private static final int FILTERS = 6;
	private static final int FILTER_ID = 7;

	private static final UriMatcher uriMatcher;
	
	private static SQLiteStatement sqlCount;
	public static ContentResolver contentResolver;
	
	private DatabaseHelper openHelper;
	private SQLiteDatabase db;

	//----------------------------------------------
	//--- PROVIDER OPERATIONS ---
	//----------------------------------------------

	@Override
	public boolean onCreate() {
		Context ctx = getContext();
		contentResolver = ctx.getContentResolver();
		openHelper = new DatabaseHelper(ctx);
		db = openHelper.getWritableDatabase();
		return openHelper != null;
	}

	/**
	 * Get type of the URI to make sure we use the right table(s).
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ARTS:
			return Arts.CONTENT_TYPE;
		case ART_ID:
			return Arts.CONTENT_ITEM_TYPE;
		case ARTISTS:
			return Artists.CONTENT_TYPE;
		case ARTIST_ID:
			return Artists.CONTENT_ITEM_TYPE;
		case CATEGORIES:
			return Categories.CONTENT_TYPE;
		case NEIGHBORHOODS:
			return Neighborhoods.CONTENT_TYPE;
		case FILTERS:
			return Filters.CONTENT_TYPE;
		case FILTER_ID:
			return Filters.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}
	}

	private String getTableName(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ARTS:
			return Arts.TABLE_NAME;
		case ARTISTS:
			return Artists.TABLE_NAME;
		case CATEGORIES:
			return Categories.TABLE_NAME;
		case NEIGHBORHOODS:
			return Neighborhoods.TABLE_NAME;
		case FILTERS:
			return Filters.TABLE_NAME;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}
	}

	private Uri getContentUri(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ARTS:
			return Arts.CONTENT_URI;
		case ARTISTS:
			return Artists.CONTENT_URI;
		case CATEGORIES:
			return Categories.CONTENT_URI;
		case NEIGHBORHOODS:
			return Neighborhoods.CONTENT_URI;
		case FILTERS:
			return Filters.CONTENT_URI;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		StringBuilder where = uri.getPathSegments().size() == 1 ? null : new StringBuilder(100);

		switch (uriMatcher.match(uri)) {
		case ARTS:
			qb.setTables(Arts.TABLE_NAME);
			qb.setProjectionMap(artsMap);
			break;
		case ART_ID:
			qb.setTables(Arts.TABLE_NAME);
			qb.setProjectionMap(artsMap);

			where.append(Arts.ID);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // art id
			qb.appendWhere(where.toString());
			break;
		case ARTISTS:
			qb.setTables(Artists.TABLE_NAME);
			qb.setProjectionMap(artistsMap);
			break;
		case ARTIST_ID:
			qb.setTables(Artists.TABLE_NAME);
			qb.setProjectionMap(artistsMap);

			where.append(Artists.NAME);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // artist name
			qb.appendWhere(where.toString());
			break;
		case CATEGORIES:
			qb.setTables(Categories.TABLE_NAME);
			qb.setProjectionMap(categoriesMap);
			break;
		case NEIGHBORHOODS:
			qb.setTables(Neighborhoods.TABLE_NAME);
			qb.setProjectionMap(neighborhoodsMap);
			break;
		case FILTERS:
			qb.setTables(Filters.TABLE_NAME);
			qb.setProjectionMap(filtersMap);
			break;
		case FILTER_ID:
			qb.setTables(Filters.TABLE_NAME);
			qb.setProjectionMap(filtersMap);

			where.append(Filters.TITLE);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // filter name
			qb.appendWhere(where.toString());
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}

		//  if no sort order is specified use the default
		String orderBy = "";
		if (TextUtils.isEmpty(sortOrder)) {
			switch (uriMatcher.match(uri)) {
			case ARTS:
				orderBy = Arts.DEFAULT_SORT_ORDER;
				break;
			case ARTISTS:
				orderBy = Artists.DEFAULT_SORT_ORDER;
				break;
			case CATEGORIES:
				orderBy = Categories.DEFAULT_SORT_ORDER;
				break;
			case NEIGHBORHOODS:
				orderBy = Neighborhoods.DEFAULT_SORT_ORDER;
				break;
			case FILTERS:
				orderBy = Filters.DEFAULT_SORT_ORDER;
				break;
			}
		}
		else {
			orderBy = sortOrder;
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// tell the cursor what URI to watch, so it knows when its source data changes
		c.setNotificationUri(contentResolver, uri);

		return c;
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		}
		else {
			values = new ContentValues();
		}

		// make sure that the fields are all set
		String unknown = getContext().getString(R.string.value_unknown);

		switch (uriMatcher.match(uri)) {
		case ARTS:
			if (!values.containsKey(Arts.ID)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri + " because '" + Arts.ID
						+ "' was not found!");
			}
			if (!values.containsKey(Arts.TITLE)) {
				values.put(Arts.TITLE, unknown);
			}
			if (!values.containsKey(Arts.CATEGORY)) {
				values.put(Arts.CATEGORY, unknown);
			}
			if (!values.containsKey(Arts.NEIGHBORHOOD)) {
				values.put(Arts.NEIGHBORHOOD, unknown);
			}
			if (!values.containsKey(Arts.LOCATION_DESCRIPTION)) {
				values.put(Arts.LOCATION_DESCRIPTION, unknown);
			}
			if (!values.containsKey(Arts.LATITUDE)) {
				values.put(Arts.LATITUDE, 0.0);
			}
			if (!values.containsKey(Arts.LONGITUDE)) {
				values.put(Arts.LONGITUDE, 0.0);
			}
			if (!values.containsKey(Arts.CREATED_AT)) {
				values.put(Arts.CREATED_AT, "");
			}
			if (!values.containsKey(Arts.UPDATED_AT)) {
				values.put(Arts.UPDATED_AT, "");
			}
			if (!values.containsKey(Arts.WARD)) {
				values.put(Arts.WARD, 0);
			}
			if (!values.containsKey(Arts.PHOTO_IDS)) {
				values.put(Arts.PHOTO_IDS, "");
			}
			if (!values.containsKey(Arts.ARTIST)) {
				values.put(Arts.ARTIST, "");
			}
			break;
		case ARTISTS:
			if (!values.containsKey(Artists.NAME)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri
 + " because '" + Artists.NAME
						+ "' was not found!");
			}
			break;
		case CATEGORIES:
			if (!values.containsKey(Categories.NAME)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri + " because " + Categories.NAME
						+ "' was not found!");
			}
			break;
		case NEIGHBORHOODS:
			if (!values.containsKey(Neighborhoods.NAME)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri + " because "
						+ Neighborhoods.NAME + "' was not found!");
			}
			break;
		case FILTERS:
			if (!values.containsKey(Filters.TITLE)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri
						+ " because 'title' was not found!");
			}
			if (!values.containsKey(Filters.PROPERTY)) {
				throw new IllegalArgumentException("Failed to insert row into " + uri
						+ " because 'property' was not found!");
			}
			if (!values.containsKey(Filters.VALUE)) {
				values.put(Filters.VALUE, "");
			}
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}

		long rowId = db.insert(getTableName(uri), unknown, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(getContentUri(uri), rowId);
			contentResolver.notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	/*
	 * @see android.content.ContentProvider#bulkInsert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {

		int rowCount = values.length;
		String tableName = getTableName(uri);

		db.beginTransaction();
		int countBefore = getRowsCount(db, tableName);

		for (int i = 0; i < rowCount; ++i) {
			try {
				db.replaceOrThrow(tableName, BaseColumns._ID, values[i]);
			}
			catch (Exception e) {
				// ignore...
				Log.w(Utils.TAG, "Exception replacing row " + i + " in table '" + tableName + "':" + e.getMessage());
			}
		}

		final int count = getRowsCount(db, tableName) - countBefore;
		db.setTransactionSuccessful();
		db.endTransaction();

		Log.i(Utils.TAG, "INSERTED into '" + tableName + "': " + count);
		Log.i(Utils.TAG, "REPLACED into '" + tableName + "': " + (rowCount - count));

		return count;
	}

	private int getRowsCount(SQLiteDatabase db, String table) {
		if (sqlCount == null) {
			sqlCount = db.compileStatement("SELECT COUNT(*) FROM ?");
		}

		try {
			sqlCount.bindString(0, table);
			return (int) sqlCount.simpleQueryForLong();
		}
		finally {
			sqlCount.clearBindings();
		}
	}

	/**
	 * Update a record in the database.
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 *      android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		StringBuilder where = uri.getPathSegments().size() == 1 ? null : new StringBuilder(100);
		int count = 0;

		db.beginTransaction();

		switch (uriMatcher.match(uri)) {
		case ARTS:
			count = db.update(Arts.TABLE_NAME, values, selection, selectionArgs);
			break;
		case ART_ID:
			where.append(Arts.ID);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // art id
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.update(Arts.TABLE_NAME, values, where.toString(), selectionArgs);
			break;
		case ARTISTS:
			count = db.update(Artists.TABLE_NAME, values, selection, selectionArgs);
			break;
		case ARTIST_ID:
			where.append(Artists.NAME);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // artist name
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.update(Artists.TABLE_NAME, values, where.toString(), selectionArgs);
			break;
		case CATEGORIES:
			count = db.update(Categories.TABLE_NAME, values, selection, selectionArgs);
			break;
		case NEIGHBORHOODS:
			count = db.update(Neighborhoods.TABLE_NAME, values, selection, selectionArgs);
			break;
		case FILTERS:
			count = db.update(Filters.TABLE_NAME, values, selection, selectionArgs);
			break;
		case FILTER_ID:
			where.append(Filters.TITLE);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // filter name
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.update(Filters.TABLE_NAME, values, where.toString(), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}

		db.setTransactionSuccessful();
		db.endTransaction();

		// notify all CursorAdapters
		contentResolver.notifyChange(uri, null);

		return count;
	}

	/**
	 * Delete a record from the database.
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 *      java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		StringBuilder where = uri.getPathSegments().size() == 1 ? null : new StringBuilder(100);
		int count = 0;

		db.beginTransaction();

		switch (uriMatcher.match(uri)) {
		case ARTS:
			count = db.delete(Arts.TABLE_NAME, selection, selectionArgs);
			break;
		case ART_ID:
			where.append(Arts.ID);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // art id
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.delete(Arts.TABLE_NAME, where.toString(), selectionArgs);
			break;
		case ARTISTS:
			count = db.delete(Artists.TABLE_NAME, selection, selectionArgs);
			break;
		case ARTIST_ID:
			where.append(Artists.NAME);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // artist name
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.delete(Artists.TABLE_NAME, where.toString(), selectionArgs);
			break;
		case CATEGORIES:
			count = db.delete(Categories.TABLE_NAME, selection, selectionArgs);
			break;
		case NEIGHBORHOODS:
			count = db.delete(Neighborhoods.TABLE_NAME, selection, selectionArgs);
			break;
		case FILTERS:
			count = db.delete(Filters.TABLE_NAME, selection, selectionArgs);
			break;
		case FILTER_ID:
			where.append(Filters.TITLE);
			where.append('=');
			where.append(uri.getPathSegments().get(1)); // filter name
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (");
				where.append(selection);
				where.append(')');
			}
			count = db.delete(Filters.TABLE_NAME, where.toString(), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}

		db.setTransactionSuccessful();
		db.endTransaction();

		// notify all CursorAdapters
		contentResolver.notifyChange(uri, null);

		return count;
	}

	//----------------------------------------------
	//--- DATABASE HELPER ---
	//----------------------------------------------
	/**
	 * Database Helper class
	 */
	private class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public synchronized void close() {
			db.close();
			super.close();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(createArtsTable());
			db.execSQL(createArtistsTable());
			db.execSQL(createCategoriesTable());
			db.execSQL(createNeighborhoodsTable());
			db.execSQL(createFiltersTable());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion != 0) {
				Log.i(Utils.TAG, "SQL: Upgrading database from version " + oldVersion + " to " + DATABASE_VERSION
						+ ", which will destroy old data");

				// Version 1 - Arts, Artists, Categories, Neighborhoods, Filters
			}
		}
	}

	//--------------------------------------------------
	//--- STATIC INITIALIZATIONS ---
	//--------------------------------------------------
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Arts.TABLE_NAME, ARTS);
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Arts.TABLE_NAME + "/#", ART_ID);
		
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Artists.TABLE_NAME, ARTISTS);
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Artists.TABLE_NAME + "/#", ARTIST_ID);
		
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Categories.TABLE_NAME, CATEGORIES);
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Neighborhoods.TABLE_NAME, NEIGHBORHOODS);
		
		artistsMap = new HashMap<String, String>();
		artistsMap.put(Artists._ID, Artists.TABLE_NAME + "." + Artists._ID);
		artistsMap.put(Artists.NAME, Artists.TABLE_NAME + "." + Artists.NAME);

		artsMap = new HashMap<String, String>();
		artsMap.put(Arts._ID, Arts.TABLE_NAME + "." + Arts._ID);
		artsMap.put(Arts.ID, Arts.ID);
		artsMap.put(Arts.TITLE, Arts.TITLE);
		artsMap.put(Arts.CATEGORY, Arts.CATEGORY);
		artsMap.put(Arts.NEIGHBORHOOD, Arts.NEIGHBORHOOD);
		artsMap.put(Arts.LOCATION_DESCRIPTION, Arts.LOCATION_DESCRIPTION);
		artsMap.put(Arts.LONGITUDE, Arts.LONGITUDE);
		artsMap.put(Arts.LATITUDE, Arts.LATITUDE);
		artsMap.put(Arts.CREATED_AT, Arts.CREATED_AT);
		artsMap.put(Arts.UPDATED_AT, Arts.UPDATED_AT);
		artsMap.put(Arts.PHOTO_IDS, Arts.PHOTO_IDS);
		artsMap.put(Arts.WARD, Arts.WARD);
		artsMap.put(Arts.ARTIST, Arts.ARTIST);

		categoriesMap = new HashMap<String, String>();
		categoriesMap.put(Categories._ID, Categories.TABLE_NAME + "." + Categories._ID);
		categoriesMap.put(Categories.NAME, Categories.NAME);

		neighborhoodsMap = new HashMap<String, String>();
		neighborhoodsMap.put(Neighborhoods._ID, Neighborhoods.TABLE_NAME + "." + Neighborhoods._ID);
		neighborhoodsMap.put(Neighborhoods.NAME, Neighborhoods.NAME);

		filtersMap = new HashMap<String, String>();
		filtersMap.put(Filters._ID, Filters.TABLE_NAME + "." + Filters._ID);
		filtersMap.put(Filters.TITLE, Filters.TITLE);
		filtersMap.put(Filters.PROPERTY, Filters.PROPERTY);
		filtersMap.put(Filters.VALUE, Filters.VALUE);
	}
}
