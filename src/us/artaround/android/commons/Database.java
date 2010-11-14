package us.artaround.android.commons;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import us.artaround.models.Art;
import us.artaround.models.Artist;
import us.artaround.services.ArtService;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class Database {
	public static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "artaround.db";
	
	private static final HashMap<String, String> ARTS_PROJECTION;
	private static final HashMap<String, String> ARTISTS_PROJECTION;
	private static final HashMap<String, String> ART_FAVORITES_PROJECTION;
	
	private static final String ARTS_TABLES;
	private static final String ARTS_WHERE;

	private static final String ART_FAVORITES_TABLES;
	private static final String ART_FAVORITES_WHERE;

	private static final SimpleDateFormat df = new SimpleDateFormat(Utils.DATE_FORMAT);

	private static SQLiteDatabase db;
	private static Database instance;

	private static DatabaseUtils.InsertHelper artInsert, artistInsert;

	private String prop(Cursor cursor, String name) {
		return cursor.getString(cursor.getColumnIndex(name));
	}

	private Art artFromCursor(Cursor c) {
		Art art = new Art();
		art.slug = prop(c, Arts.SLUG);
		art.category = prop(c, Arts.CATEGORY);
		art.title = prop(c, Arts.TITLE);
		art.locationDesc = prop(c, Arts.LOCATION_DESCRIPTION);
		art.neighborhood = prop(c, Arts.NEIGHBORHOOD);

		String slug = prop(c, Artists.SLUG);
		String name = prop(c, Artists.NAME);
		if (slug != null && name != null) {
			art.artist = new Artist(slug, name);
		}
		String photoIds = prop(c, Arts.PHOTO_IDS);
		if (!TextUtils.isEmpty(photoIds)) {
			art.photoIds = TextUtils.split(photoIds, Utils.STR_SEP);
		}

		art.ward = c.getInt(c.getColumnIndex(Arts.WARD));
		art.latitude = c.getFloat(c.getColumnIndex(Arts.LATITUDE));
		art.longitude = c.getFloat(c.getColumnIndex(Arts.LONGITUDE));

		try {
			art.createdAt = df.parse(prop(c, Arts.CREATED_AT));
			art.updatedAt = df.parse(prop(c, Arts.UPDATED_AT));
		} catch (ParseException e) {
			Log.w(Utils.TAG, "SQL: Could not parse art dates", e);
		}
		return art;
	}

	private List<Art> artsFromCursor(Cursor c) {
		List<Art> arts = null;
		if (c.moveToFirst()) {
			int count = c.getCount();
			arts = new ArrayList<Art>(count);

			for (int i = 0; i < count; i++) {
				arts.add(artFromCursor(c));
				c.moveToNext();
			}
		}
		return arts;
	}

	private Artist artistFromCursor(Cursor c) {
		Artist artist = new Artist();
		artist.slug = prop(c, Artists.SLUG);
		artist.name = prop(c, Artists.NAME);
		return artist;
	}

	private List<Artist> artistsFromCursor(Cursor c) {
		List<Artist> artists = null;
		if (c.moveToFirst()) {
			int count = c.getCount();
			artists = new ArrayList<Artist>(count);

			for (int i = 0; i < count; i++) {
				artists.add(artistFromCursor(c));
				c.moveToNext();
			}
		}
		return artists;
	}

	private ContentValues artToValues(Art art) {
		ContentValues cv = new ContentValues(12);
		cv.put(Arts.SLUG, art.slug);
		cv.put(Arts.TITLE, art.title);
		cv.put(Arts.CATEGORY, art.category);
		cv.put(Arts.NEIGHBORHOOD, art.neighborhood);
		cv.put(Arts.LOCATION_DESCRIPTION, art.locationDesc);
		cv.put(Arts.CREATED_AT, df.format(art.createdAt));
		cv.put(Arts.UPDATED_AT, df.format(art.updatedAt));
		cv.put(Arts.WARD, art.ward);
		cv.put(Arts.LATITUDE, art.latitude);
		cv.put(Arts.LONGITUDE, art.longitude);

		if (art.photoIds != null) {
			cv.put(Arts.PHOTO_IDS, TextUtils.join(Utils.STR_SEP, art.photoIds));
		}
		if (art.artist != null) {
			cv.put(Arts.ARTIST_SLUG, art.artist.slug);
		}
		return cv;
	}

	private ContentValues artistToValues(Artist artist) {
		ContentValues cv = new ContentValues(2);
		cv.put(Artists.SLUG, artist.slug);
		cv.put(Artists.NAME, artist.name);
		return cv;
	}

	/* Art CRUD */
	public List<Art> getArts() {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ARTS_TABLES);
		qb.setProjectionMap(ARTS_PROJECTION);
		qb.appendWhere(ARTS_WHERE);

		db.beginTransaction();
		long start = System.currentTimeMillis();
		try {
			String[] projection = ARTS_PROJECTION.keySet().toArray(new String[ARTS_PROJECTION.size()]);
			Cursor c = qb.query(db, projection, null, null, null, null, null);
			db.setTransactionSuccessful();

			List<Art> results = artsFromCursor(c);
			safeClose(c);
			Log.d(Utils.TAG, "SQL: There are " + results.size() + " arts in the db.");
			return results;
		} finally {
			db.endTransaction();
			Log.d(Utils.TAG, "SQL: Retrieval of arts took " + (System.currentTimeMillis() - start) + " millis.");
		}
	}

	public int addArts(List<Art> arts) {
		if (arts == null || arts.isEmpty()) return 0;

		int count = 0;
		db.beginTransaction();
		long start = System.currentTimeMillis();
		try {
			for (int i = 0; i < arts.size(); i++) {
				Art art = arts.get(i);
				//if (db.insert(Arts.TABLE_NAME, "", artToValues(art)) != -1) {
				//if (bindAndInsert(art)) {
				if (artInsert.insert(artToValues(art)) != -1) {
					++count;
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			Log.d(Utils.TAG, "SQL: Insertion of " + count + " arts took " + (System.currentTimeMillis() - start)
					+ " millis.");
		}
		return count;
	}

	public Art getArt(int artId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ARTS_TABLES);
		qb.setProjectionMap(ARTS_PROJECTION);

		StringBuilder where = new StringBuilder();
		where.append(ARTS_WHERE);
		where.append(" AND ").append(Arts.TABLE_NAME).append(".").append(Arts._ID).append("=").append(artId);
		qb.appendWhere(where.toString());

		db.beginTransaction();
		try {
			String[] projection = ARTS_PROJECTION.keySet().toArray(new String[ARTS_PROJECTION.size()]);
			Cursor c = qb.query(db, projection, null, null, null, null, null);
			db.setTransactionSuccessful();

			Art result = artFromCursor(c);
			safeClose(c);
			return result;
		} finally {
			db.endTransaction();
		}
	}

	/* Art favorites CRUD */
	public boolean isArtFavorite(int artId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ART_FAVORITES_TABLES);
		qb.setProjectionMap(ART_FAVORITES_PROJECTION);

		StringBuilder where = new StringBuilder();
		where.append(ART_FAVORITES_WHERE);
		where.append(" AND ").append(Arts.TABLE_NAME).append(".").append(Arts._ID).append("=").append(artId);
		qb.appendWhere(where.toString());

		db.beginTransaction();
		try {
			String[] projection = ART_FAVORITES_PROJECTION.keySet()
					.toArray(new String[ART_FAVORITES_PROJECTION.size()]);
			Cursor c = qb.query(db, projection, null, null, null, null, null);
			db.setTransactionSuccessful();

			boolean result = c.getCount() == 1;
			safeClose(c);
			return result;
		} finally {
			db.endTransaction();
		}
	}

	public boolean addArtFavorite(String slug) {
		db.beginTransaction();
		try {
			ContentValues vals = new ContentValues();
			vals.put(ArtFavorites.ART_SLUG, slug);
			long result = db.insert(ArtFavorites.TABLE_NAME, null, vals);
			db.setTransactionSuccessful();
			return result != -1;
		} finally {
			db.endTransaction();
		}
	}

	public int deleteArtFavorite(String slug) {
		return db.delete(ART_FAVORITES_TABLES, ArtFavorites.ART_SLUG + "=?", new String[] { "" + slug });
	}

	/* Artist CRUD */
	public List<Artist> getArtists() {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Artists.TABLE_NAME);
		qb.setProjectionMap(ARTISTS_PROJECTION);

		db.beginTransaction();
		long start = System.currentTimeMillis();
		try {
			String[] projection = ARTISTS_PROJECTION.keySet().toArray(new String[ARTISTS_PROJECTION.size()]);
			Cursor c = qb.query(db, projection, null, null, null, null, null);
			db.setTransactionSuccessful();

			List<Artist> results = artistsFromCursor(c);
			safeClose(c);
			Log.d(Utils.TAG, "SQL: There are " + results.size() + " artists in the db.");
			return results;
		} finally {
			db.endTransaction();
			Log.d(Utils.TAG, "SQL: Retrieval of artists took " + (System.currentTimeMillis() - start) + " millis.");
		}
	}

	public Artist getArtist(int id) {
		return null;
	}

	public int addArtists(Collection<Artist> artists) {
		if (artists == null || artists.isEmpty()) return 0;

		int count = 0;
		db.beginTransaction();
		long start = System.currentTimeMillis();
		try {
			for (Artist artist : artists) {
				//if (db.insert(Artists.TABLE_NAME, "", artistToValues(artist)) != -1) {
				//if (bindAndInsert(artist)) {
				if (artistInsert.insert(artistToValues(artist)) != -1) {
					++count;
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			Log.d(Utils.TAG, "SQL: Insertion of " + count + " artists took " + (System.currentTimeMillis() - start)
					+ " millis.");
		}
		return count;
	}

	private Database() {} // singleton, use getInstance()

	public static synchronized Database getInstance(Context context) {
		if (instance == null) {
			instance = new Database();
		}
		try {
			db = context.openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
		} catch (SQLiteException e) {
			// try again by deleting the old db and create a new one
			if (context.deleteDatabase(DATABASE_NAME)) {
				db = context.openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
			}
		}

		if (db != null && db.getVersion() != DATABASE_VERSION) {
			db.beginTransaction();
			try {
				upgradeDatabase();
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}

		newInsert();

		return instance;
	}

	private static void newInsert() {
		artInsert = new InsertHelper(db, Arts.TABLE_NAME);
		artistInsert = new InsertHelper(db, Artists.TABLE_NAME);
	}

	private static void upgradeDatabase() {
		int oldVersion = db.getVersion();
		if (oldVersion != 0) {
			Log.i(Utils.TAG, "SQL: Upgrading database from version " + oldVersion + " to " + DATABASE_VERSION
					+ ", which will destroy old data");

			// Version 1 - Caching art, artists and favorites
		} else {
			db.execSQL(createArtsTable());
			db.execSQL(createArtFavoritesTable());
			db.execSQL(createArtistsTable());
			db.setVersion(DATABASE_VERSION);
		}
	}

	public boolean isOpen() {
		return db.isOpen();
	}

	public void close() {
		if (isOpen()) {
			db.close();
		}
	}

	private void safeClose(Cursor c) {
		if (!c.isClosed()) {
			c.close();
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
		b.append(Arts.ARTIST_SLUG).append(" TEXT);");

		// index on slug
		b.append("CREATE INDEX idx ON ").append(Arts.TABLE_NAME).append(" (");
		b.append(Arts.SLUG).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	private static String createArtistsTable() {
		StringBuilder b = new StringBuilder();

		b.append("CREATE TABLE ").append(Artists.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Artists.SLUG).append(" TEXT,");
		b.append(Artists.NAME).append(" TEXT);");

		// index on slug
		b.append("CREATE INDEX idx ON ").append(Artists.TABLE_NAME).append(" (");
		b.append(Artists.SLUG).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	private static String createArtFavoritesTable() {
		StringBuilder b = new StringBuilder();

		b.append("CREATE TABLE ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(BaseColumns._ID).append(" INTEGER PRIMARY KEY,");
		b.append(ArtFavorites.ART_SLUG).append(" TEXT);");

		// index on slug
		b.append("CREATE INDEX idx ON ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(ArtFavorites.ART_SLUG).append(");");

		String str = b.toString();
		Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public void clearCache() {
		Log.d(Utils.TAG, "SQL: clearing cache...");
		emptyTable(Arts.TABLE_NAME);
		emptyTable(Artists.TABLE_NAME);
	}

	public void updateCache(List<Art> arts) {
		Log.d(Utils.TAG, "SQL: updating cache for artists and arts...");
		clearCache();
		addArtists(ArtService.artists.values());
		addArts(arts);
	}

	private static String emptyTable(String tableName) {
		return "DELETE FROM " + tableName + ";";
	}

	/* Arts table */
	public static final class Arts implements BaseColumns {
		public static final String TABLE_NAME = "arts";

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
		public static final String ARTIST_SLUG = "artist_slug";
	}

	/* Artists table */
	public static final class Artists implements BaseColumns {
		public static final String TABLE_NAME = "artists";

		public static final String SLUG = "slug";
		public static final String NAME = "name";
	}

	/* Favorites table */
	public static final class ArtFavorites implements BaseColumns {
		public static final String TABLE_NAME = "art_favorites";

		public static final String ART_SLUG = "art_slug";
	}

	static {
		ARTISTS_PROJECTION = new HashMap<String, String>();
		ARTISTS_PROJECTION.put(Artists.SLUG, Artists.TABLE_NAME + "." + Artists.SLUG);
		ARTISTS_PROJECTION.put(Artists.NAME, Artists.NAME);

		ARTS_PROJECTION = new HashMap<String, String>();
		ARTS_PROJECTION.put(Arts.SLUG, Arts.TABLE_NAME + "." + Arts.SLUG);
		ARTS_PROJECTION.put(Arts.CATEGORY, Arts.CATEGORY);
		ARTS_PROJECTION.put(Arts.NEIGHBORHOOD, Arts.NEIGHBORHOOD);
		ARTS_PROJECTION.put(Arts.LOCATION_DESCRIPTION, Arts.LOCATION_DESCRIPTION);
		ARTS_PROJECTION.put(Arts.LONGITUDE, Arts.LONGITUDE);
		ARTS_PROJECTION.put(Arts.LATITUDE, Arts.LATITUDE);
		ARTS_PROJECTION.put(Arts.ARTIST_SLUG, Arts.ARTIST_SLUG);
		ARTS_PROJECTION.put(Arts.CREATED_AT, Arts.CREATED_AT);
		ARTS_PROJECTION.put(Arts.UPDATED_AT, Arts.UPDATED_AT);
		ARTS_PROJECTION.put(Arts.TITLE, Arts.TITLE);
		ARTS_PROJECTION.put(Arts.PHOTO_IDS, Arts.PHOTO_IDS);
		ARTS_PROJECTION.put(Arts.WARD, Arts.WARD);
		ARTS_PROJECTION.putAll(ARTISTS_PROJECTION);

		ART_FAVORITES_PROJECTION = new HashMap<String, String>();
		ART_FAVORITES_PROJECTION.put(ArtFavorites.ART_SLUG, ArtFavorites.ART_SLUG);
		ART_FAVORITES_PROJECTION.putAll(ARTS_PROJECTION);

		ARTS_TABLES = Arts.TABLE_NAME + "," + Artists.TABLE_NAME;
		ART_FAVORITES_TABLES = ArtFavorites.TABLE_NAME + "," + Arts.TABLE_NAME;

		StringBuilder b = new StringBuilder();
		b.append(Arts.TABLE_NAME).append(".").append(Arts.ARTIST_SLUG).append("=")
		.append(Artists.TABLE_NAME).append(".").append(Artists.SLUG);
		ARTS_WHERE = b.toString();

		b.delete(0, b.length()); // clear and re-use
		b.append(ArtFavorites.TABLE_NAME).append(".").append(ArtFavorites.ART_SLUG).append("=")
		.append(Arts.TABLE_NAME).append(".").append(Arts.SLUG);
		ART_FAVORITES_WHERE = b.toString();
	}
}