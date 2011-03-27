package us.artaround.android.database;

import static us.artaround.android.database.ArtAroundDatabase.ARTAROUND_AUTHORITY;
import static us.artaround.android.database.ArtAroundDatabase.createArtFavoritesTable;
import static us.artaround.android.database.ArtAroundDatabase.createArtistsTable;
import static us.artaround.android.database.ArtAroundDatabase.createArtsTable;
import static us.artaround.android.database.ArtAroundDatabase.createCategoriesTable;
import static us.artaround.android.database.ArtAroundDatabase.createNeighborhoodsTable;

import java.util.HashMap;

import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase.ArtFavorites;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
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

public class ArtAroundProvider extends ContentProvider {
	private static final String DATABASE_NAME = "artaround.db";
	private static final int DATABASE_VERSION = 1;

	private static HashMap<String, String> artsMap;
	private static HashMap<String, String> artistsMap;
	private static HashMap<String, String> categoriesMap;
	private static HashMap<String, String> neighborhoodsMap;

	private static final int ARTS = 0;
	private static final int ARTISTS = 2;
	private static final int CATEGORIES = 4;
	private static final int NEIGHBORHOODS = 5;
	private static final int ART_FAVORITES = 6;

	private static final String ARTS_TABLES;
	private static final String ART_FAVORITES_TABLES;

	private static final UriMatcher uriMatcher;

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
		case ARTISTS:
			return Artists.CONTENT_TYPE;
		case CATEGORIES:
			return Categories.CONTENT_TYPE;
		case NEIGHBORHOODS:
			return Neighborhoods.CONTENT_TYPE;
		case ART_FAVORITES:
			return ArtFavorites.CONTENT_TYPE;
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
		case ART_FAVORITES:
			return ArtFavorites.TABLE_NAME;
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
		case ART_FAVORITES:
			return ArtFavorites.CONTENT_URI;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (uriMatcher.match(uri)) {
		case ARTS:
			qb.setTables(ARTS_TABLES);
			qb.setProjectionMap(artsMap);
			break;
		case ARTISTS:
			qb.setTables(Artists.TABLE_NAME);
			qb.setProjectionMap(artistsMap);
			break;
		case CATEGORIES:
			qb.setTables(Categories.TABLE_NAME);
			qb.setProjectionMap(categoriesMap);
			break;
		case NEIGHBORHOODS:
			qb.setTables(Neighborhoods.TABLE_NAME);
			qb.setProjectionMap(neighborhoodsMap);
			break;
		case ART_FAVORITES:
			qb.setTables(ART_FAVORITES_TABLES);
			qb.setProjectionMap(artsMap);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: '" + uri + "'.");
		}

		qb.setDistinct(true);

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
		db.beginTransaction();

		long rowId = db.insert(getTableName(uri), BaseColumns._ID, initialValues);

		db.setTransactionSuccessful();
		db.endTransaction();

		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(getContentUri(uri), rowId);
			contentResolver.notifyChange(newUri, null);
			return newUri;
		}
		else {
			throw new SQLException("Failed to insert row into " + uri);
		}
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

		boolean hasNull = false;
		try {
			for (int i = 0; i < rowCount; ++i) {
				ContentValues value = values[i];
				if (value == null || value.size() == 0)
					hasNull = true;
				else
					db.replaceOrThrow(tableName, BaseColumns._ID, value);
			}

			// insert null only once!!
			if (hasNull) {
				db.replaceOrThrow(tableName, BaseColumns._ID, null);
			}
		}
		catch (Exception e) {
			// ignore...
			Utils.w(Utils.TAG, "Exception replacing row in table '" + tableName, e);
		}

		final int count = getRowsCount(db, tableName) - countBefore;
		db.setTransactionSuccessful();
		db.endTransaction();

		//contentResolver.notifyChange(uri, null);

		if (count > 0) {
			Utils.i(Utils.TAG, "INSERTED into '" + tableName + "': " + count);
		}

		int rest = rowCount - count;
		if (rest > 0) {
			Utils.i(Utils.TAG, "REPLACED into '" + tableName + "': " + (rowCount - count));
		}

		return count;
	}

	private int getRowsCount(SQLiteDatabase db, String table) {
		SQLiteStatement sqlCount = null;
		try {
			sqlCount = db.compileStatement("SELECT COUNT(*) FROM " + table);
			return (int) sqlCount.simpleQueryForLong();
		}
		finally {
			if (sqlCount != null) {
				sqlCount.close();
			}
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

		//StringBuilder where = uri.getPathSegments().size() == 1 ? null : new StringBuilder(100);
		int count = 0;

		db.beginTransaction();

		switch (uriMatcher.match(uri)) {
		case ARTS:
			count = db.update(Arts.TABLE_NAME, values, selection, selectionArgs);
			break;
		case ARTISTS:
			count = db.update(Artists.TABLE_NAME, values, selection, selectionArgs);
			break;
		case CATEGORIES:
			count = db.update(Categories.TABLE_NAME, values, selection, selectionArgs);
			break;
		case NEIGHBORHOODS:
			count = db.update(Neighborhoods.TABLE_NAME, values, selection, selectionArgs);
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
	 * From SQLite: When the WHERE is omitted from a DELETE statement and the
	 * table being deleted has no triggers, SQLite uses an optimization to erase
	 * the entire table content without having to visit each row of the table
	 * individually. This "truncate" optimization makes the delete run much
	 * faster
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 *      java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		//StringBuilder where = uri.getPathSegments().size() == 1 ? null : new StringBuilder(100);
		int count = 0;

		db.beginTransaction();

		switch (uriMatcher.match(uri)) {
		case ARTS:
			count = db.delete(Arts.TABLE_NAME, selection, selectionArgs);
			break;
		case ARTISTS:
			count = db.delete(Artists.TABLE_NAME, selection, selectionArgs);
			break;
		case CATEGORIES:
			count = db.delete(Categories.TABLE_NAME, selection, selectionArgs);
			break;
		case NEIGHBORHOODS:
			count = db.delete(Neighborhoods.TABLE_NAME, selection, selectionArgs);
			break;
		case ART_FAVORITES:
			count = db.delete(ArtFavorites.TABLE_NAME, selection, selectionArgs);
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
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(createArtsTable());
			db.execSQL(createArtFavoritesTable());
			db.execSQL(createArtistsTable());
			db.execSQL(createCategoriesTable());
			db.execSQL(createNeighborhoodsTable());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion != 0) {
				Utils.i(Utils.TAG, "SQL: Upgrading database from version " + oldVersion + " to " + DATABASE_VERSION
						+ ", which will destroy old data");

				// Version 1 - Arts, Artists, Categories, Neighborhoods
			}
		}
	}

	//--------------------------------------------------
	//--- STATIC INITIALIZATIONS ---
	//--------------------------------------------------
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		uriMatcher.addURI(ARTAROUND_AUTHORITY, Arts.TABLE_NAME, ARTS);
		//uriMatcher.addURI(ARTAROUND_AUTHORITY, Arts.TABLE_NAME + "/#", ART_UUID);

		uriMatcher.addURI(ARTAROUND_AUTHORITY, Artists.TABLE_NAME, ARTISTS);
		//uriMatcher.addURI(ARTAROUND_AUTHORITY, Artists.TABLE_NAME + "/#", ARTIST_UUID);

		uriMatcher.addURI(ARTAROUND_AUTHORITY, Categories.TABLE_NAME, CATEGORIES);
		uriMatcher.addURI(ARTAROUND_AUTHORITY, Neighborhoods.TABLE_NAME, NEIGHBORHOODS);

		uriMatcher.addURI(ARTAROUND_AUTHORITY, ArtFavorites.TABLE_NAME, ART_FAVORITES);

		artistsMap = new HashMap<String, String>();
		artistsMap.put(Artists._ID, Artists._ID);
		artistsMap.put(Artists.UUID, Artists.UUID);
		artistsMap.put(Artists.NAME, Artists.NAME);

		artsMap = new HashMap<String, String>();
		artsMap.put(Arts._ID, Arts.TABLE_NAME + "." + Arts._ID);
		artsMap.put(Arts.SLUG, Arts.TABLE_NAME + "." + Arts.SLUG);
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
		artsMap.put(Arts.YEAR, Arts.YEAR);
		artsMap.put(Arts.DESCRIPTION, Arts.DESCRIPTION);
		artsMap.put(Arts.MEDIUM_DISTANCE, Arts.MEDIUM_DISTANCE);
		artsMap.put(Arts.CITY, Arts.CITY);
		artsMap.put(Arts.ARTIST, Arts.ARTIST);
		artsMap.put(Artists.NAME, Artists.TABLE_NAME + "." + Artists.NAME);

		categoriesMap = new HashMap<String, String>();
		categoriesMap.put(Categories._ID, Categories._ID);
		categoriesMap.put(Categories.NAME, Categories.NAME);

		neighborhoodsMap = new HashMap<String, String>();
		neighborhoodsMap.put(Neighborhoods._ID, Neighborhoods._ID);
		neighborhoodsMap.put(Neighborhoods.NAME, Neighborhoods.NAME);

		// matches all the arts, even when art.artist is null
		StringBuilder b = new StringBuilder();
		b.append(Arts.TABLE_NAME).append(" LEFT OUTER JOIN ").append(Artists.TABLE_NAME);
		b.append(" ON (").append(Arts.TABLE_NAME).append(".").append(Arts.ARTIST).append("=");
		b.append(Artists.TABLE_NAME).append(".").append(Artists.UUID).append(")");

		ARTS_TABLES = b.toString();

		// matches the slugs that are both in Arts and ArtFavorites
		b.delete(0, b.length());
		b.append(ArtFavorites.TABLE_NAME).append(" INNER JOIN ").append(Arts.TABLE_NAME);
		b.append(" ON (").append(ArtFavorites.TABLE_NAME).append(".").append(ArtFavorites.SLUG).append("=");
		b.append(Arts.TABLE_NAME).append(".").append(Arts.SLUG).append(") LEFT OUTER JOIN ").append(Artists.TABLE_NAME);
		b.append(" ON (").append(Arts.TABLE_NAME).append(".").append(Arts.ARTIST).append("=");
		b.append(Artists.TABLE_NAME).append(".").append(Artists.UUID).append(")");

		ART_FAVORITES_TABLES = b.toString();
	}
}
