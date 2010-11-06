package us.artaround.android.commons;

import java.io.File;
import java.io.IOException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

public class ArtAroundProvider extends ContentProvider {
	public static final String ARTAROUND_AUTHORITY = "us.artaround.android";

	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "artaround.db";
	public static final String DATABASE_PATH = "Android/data/us.artaround.android";

	public static final String KEY_USE_EXTERNAL_STORAGE = "useExternalStorage";
	private static final boolean DEFAULT_USE_EXTERNAL_STORAGE = true;

	public static ContentResolver contentResolver;
	private static UriMatcher uriMatcher;

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* Helper class for ArtAround database */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private boolean useExternalStorage;
		private SQLiteDatabase database;
		private Exception exception;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			doInitStorage(context);
		}

		private void doInitStorage(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			useExternalStorage = prefs.getBoolean(KEY_USE_EXTERNAL_STORAGE, DEFAULT_USE_EXTERNAL_STORAGE);
		}

		private void doCheckStorage(int permission) {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				exception = new SQLiteException("Can't access external storage!");
			}

			if (permission == SQLiteDatabase.OPEN_READWRITE
					&& Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
				exception = new SQLiteException("Can't access external storage: read only!");
			}
		}

		private SQLiteDatabase doOpenAndUpgrade(int permission) throws IOException {
			SQLiteDatabase db = null;

			File storage = Environment.getExternalStorageDirectory();
			File dir = new File(storage.getAbsolutePath(), DATABASE_PATH);

			boolean ok = dir.mkdirs();
			Log.d(Utils.TAG, ok == true ? "Created dir " : "Did not create dir " + DATABASE_PATH + " on sdcard.");

			File file = new File(dir.getAbsolutePath(), DATABASE_NAME);
			if (!file.exists()) {
				ok = file.createNewFile();
				Log.d(Utils.TAG, ok == true ? "Created file " : "Could not create file "+ DATABASE_NAME + " on sdcard.");
			} else {
				Log.d(Utils.TAG, "File " + DATABASE_NAME + " already exists on sdcard.");
			}

			db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY
					+ permission);

			int version = db.getVersion();

			// first time, create database
			if (version == 0) {
				onCreate(db);
			}
			// upgrade database
			else if (version != DATABASE_VERSION) {
				onUpgrade(db, version, DATABASE_VERSION);
			}

			onOpen(db);
			return db;
		}

		private SQLiteDatabase doGetDatabase(int permission) {
			doCheckStorage(permission);

			if (exception == null) {
				try {
					database = doOpenAndUpgrade(permission);
				} catch (Exception e) {
					exception = e;
				}
			}
			return database;
		}

		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			if (!useExternalStorage) {
				return super.getWritableDatabase();
			}
			return doGetDatabase(SQLiteDatabase.OPEN_READWRITE);
		}

		@Override
		public synchronized SQLiteDatabase getReadableDatabase() {
			if (!useExternalStorage) {
				return super.getReadableDatabase();
			}
			return doGetDatabase(SQLiteDatabase.OPEN_READONLY);
		}

		@Override
		public synchronized void close() {
			super.close();

			if (database != null && database.isOpen()) {
				database.close();
			}
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(createArtsTable());
			db.execSQL(createArtistsTable());
			db.execSQL(createArtFavoritesTable());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(Utils.TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to " + newVersion + ".");

			// version 1 - arts, artists, art favorites
		}
	}

	private static String createArtsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Arts.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Arts.SLUG).append(" TEXT,");
		b.append(Arts.TITLE).append(" TEXT,");
		b.append(Arts.CATEGORY).append(" TEXT,");
		b.append(Arts.NEIGHBORHOOD).append(" TEXT,");
		b.append(Arts.LOCATION_DESCRIPTION).append(" TEXT,");
		b.append(Arts.LATITUDE).append(" REAL,");
		b.append(Arts.LONGITUDE).append(" REAL,");
		b.append(Arts.CREATED_AT).append(" TEXT,");
		b.append(Arts.UPDATED_AT).append(" TEXT,");
		b.append(Arts.WARD).append(" INTEGER,");
		b.append(Arts.PHOTO_IDS).append(" TEXT,");
		b.append(Arts.ARTIST_ID).append(" INTEGER").append(");");

		// index on art
		b.append("CREATE INDEX idx ON ").append(Arts.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(");");

		// unique index on art and artist
		b.append("CREATE UNIQUE INDEX uidx ON ").append(Arts.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(",").append(Arts.ARTIST_ID).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	private static String createArtistsTable() {
		StringBuilder b = new StringBuilder();
		
		b.append("CREATE TABLE ").append(Artists.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Artists.NAME).append(" TEXT").append(");");

		// index on artist
		b.append("CREATE INDEX idx ON ").append(Artists.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	private static String createArtFavoritesTable() {
		StringBuilder b = new StringBuilder();
		
		b.append("CREATE TABLE ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(" INTEGER PRIMARY KEY,");
		b.append(ArtFavorites.ART_ID).append(" INTEGER").append(");");

		// index
		b.append("CREATE INDEX idx ON ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(");");
		
		// unique index
		b.append("CREATE UNIQUE INDEX uidx ON ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(",").append(ArtFavorites.ART_ID).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	/* Arts table */
	public static final class Arts implements BaseColumns {
		public static final String TABLE_NAME = "arts";

		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround.android." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround.android." + TABLE_NAME;

		public static final String _ID = BaseColumns._ID;
		public static final String SLUG = "slug";
		public static final String TITLE = "title";
		public static final String CATEGORY = "category";
		public static final String NEIGHBORHOOD = "neighborhood";
		public static final String LOCATION_DESCRIPTION = "location_description";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String WARD = "ward";
		public static final String CREATED_AT = "created_at";
		public static final String UPDATED_AT = "updated_at";
		public static final String PHOTO_IDS = "photo_ids";
		public static final String ARTIST_ID = "artist_id";
	}
	
	/* Artists table */
	public static final class Artists implements BaseColumns {
		public static final String TABLE_NAME = "artists";

		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround.android." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround.android." + TABLE_NAME;

		public static final String _ID = BaseColumns._ID;
		public static final String NAME = "name";
	}

	/* Favorites table */
	public static final class ArtFavorites implements BaseColumns {
		public static final String TABLE_NAME = "art_favorites";

		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround.android." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround.android." + TABLE_NAME;

		public static final String _ID = BaseColumns._ID;
		public static final String ART_ID = "art_id";
	}
}
