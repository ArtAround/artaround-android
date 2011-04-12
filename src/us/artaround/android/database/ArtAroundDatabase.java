package us.artaround.android.database;

import java.util.ArrayList;

import us.artaround.android.common.Utils;
import us.artaround.models.Art;
import us.artaround.models.Artist;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class ArtAroundDatabase {
	public static final String ARTAROUND_AUTHORITY = "us.artaround";

	public static final String[] ARTS_PROJECTION = { Arts._ID, Arts.SLUG, Arts.TITLE, Arts.CATEGORY, Arts.CREATED_AT,
			Arts.UPDATED_AT, Arts.LATITUDE, Arts.LONGITUDE, Arts.NEIGHBORHOOD, Arts.PHOTO_IDS, Arts.WARD, Arts.YEAR,
			Arts.LOCATION_DESCRIPTION, Arts.ARTIST, Arts.DESCRIPTION, Arts.MEDIUM_DISTANCE, Arts.CITY, Artists.NAME };

	public static final String[] CATEGORIES_PROJECTION = { Categories._ID, Categories.NAME };
	public static final String[] NEIGHBORHOODS_PROJECTION = { Neighborhoods._ID, Neighborhoods.NAME };
	public static final String[] ARTISTS_PROJECTION = { Artists._ID, Artists.UUID, Artists.NAME };

	// We don't need to instantiate this class.
	private ArtAroundDatabase() {}

	public static final class Arts implements BaseColumns {
		public static final String TABLE_NAME = "arts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String SLUG = "slug";
		public static final String TITLE = "title";
		public static final String CATEGORY = "category";
		public static final String NEIGHBORHOOD = "neighborhood";
		public static final String LOCATION_DESCRIPTION = "location_description";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String WARD = "ward";
		public static final String YEAR = "year";
		public static final String CREATED_AT = "created_at";
		public static final String UPDATED_AT = "updated_at";
		public static final String PHOTO_IDS = "photo_ids";
		public static final String ARTIST = "artist";
		public static final String DESCRIPTION = "description";
		public static final String MEDIUM_DISTANCE = "medium_distance";
		public static final String CITY = "city";

		public static final String DEFAULT_SORT_ORDER = MEDIUM_DISTANCE + " DESC";
	}

	public static final class ArtFavorites implements BaseColumns {
		public static final String TABLE_NAME = "art_favorites";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String SLUG = "slug";
	}

	public static final class Artists implements BaseColumns {
		public static final String TABLE_NAME = "artists";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String UUID = "uuid";
		public static final String NAME = "name";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Categories implements BaseColumns {
		public static final String TABLE_NAME = "categories";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;

		public static final String NAME = "name";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Neighborhoods implements BaseColumns {
		public static final String TABLE_NAME = "neighborhoods";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;

		public static final String NAME = "name";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static String createArtsTable() {
		StringBuilder b = new StringBuilder("CREATE TABLE ").append(Arts.TABLE_NAME).append(" (");
		b.append(Arts._ID).append(" INTEGER PRIMARY KEY,");
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
		b.append(Arts.YEAR).append(" INTEGER,");
		b.append(Arts.PHOTO_IDS).append(" TEXT,");
		b.append(Arts.DESCRIPTION).append(" TEXT,");
		b.append(Arts.MEDIUM_DISTANCE).append(" REAL,");
		b.append(Arts.CITY).append(" TEXT,");
		b.append(Arts.ARTIST).append(" TEXT);");

		// index on id
		b.append("CREATE INDEX IF NOT EXISTS idx ON ").append(Arts.TABLE_NAME).append("(").append(Arts.SLUG)
				.append(");");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createArtFavoritesTable() {
		StringBuilder b = new StringBuilder("CREATE TABLE ").append(ArtFavorites.TABLE_NAME).append(" (");
		b.append(ArtFavorites._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Arts.SLUG).append(" TEXT);");

		b.append("CREATE INDEX IF NOT EXISTS idx ON ").append(ArtFavorites.TABLE_NAME).append("(")
				.append(ArtFavorites.SLUG).append(");");

		return b.toString();
	}

	public static String createArtistsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Artists.TABLE_NAME).append(" (");
		b.append(Artists._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Artists.UUID).append(" TEXT,");
		b.append(Artists.NAME).append(" TEXT, ");

		b.append("UNIQUE (").append(Artists.NAME).append("));");

		// index on name
		b.append("CREATE INDEX IF NOT EXISTS idu ON ").append(Artists.TABLE_NAME).append("(").append(Artists.UUID)
				.append(");");
		b.append("CREATE INDEX IF NOT EXISTS idx ON ").append(Artists.TABLE_NAME).append("(").append(Artists.NAME)
				.append(");");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createCategoriesTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Categories.TABLE_NAME).append(" (");
		b.append(Categories._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Categories.NAME).append(" TEXT, ");

		b.append("UNIQUE (").append(Categories.NAME).append("));");

		// index on name
		b.append("CREATE INDEX IF NOT EXISTS idx ON ").append(Categories.TABLE_NAME).append("(")
				.append(Categories.NAME).append(");");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createNeighborhoodsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Neighborhoods.TABLE_NAME).append(" (");
		b.append(Neighborhoods._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Neighborhoods.NAME).append(" TEXT, ");

		b.append("UNIQUE (").append(Neighborhoods.NAME).append("));");

		// index on name
		b.append("CREATE INDEX IF NOT EXISTS idx ON ").append(Neighborhoods.TABLE_NAME).append("(")
				.append(Neighborhoods.NAME).append(");");
		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	//--- utility methods ---
	public static String prop(Cursor cursor, String name) {
		return cursor.getString(cursor.getColumnIndex(name));
	}

	public static long propl(Cursor cursor, String name) {
		return cursor.getLong(cursor.getColumnIndex(name));
	}

	public static int propi(Cursor cursor, String name) {
		return cursor.getInt(cursor.getColumnIndex(name));
	}

	public static double propd(Cursor cursor, String name) {
		return cursor.getFloat(cursor.getColumnIndex(name));
	}

	public static Art artFromCursor(Cursor c) {
		Art art = new Art();
		art.slug = prop(c, Arts.SLUG);
		art.category = prop(c, Arts.CATEGORY);
		art.title = prop(c, Arts.TITLE);
		art.locationDesc = prop(c, Arts.LOCATION_DESCRIPTION);
		art.neighborhood = prop(c, Arts.NEIGHBORHOOD);
		art.ward = propi(c, Arts.WARD);
		art.latitude = propd(c, Arts.LATITUDE);
		art.longitude = propd(c, Arts.LONGITUDE);

		art.artist = new Artist(prop(c, Arts.ARTIST), prop(c, Artists.NAME));

		art.year = propi(c, Arts.YEAR);
		art.description = prop(c, Arts.DESCRIPTION);
		art.mediumDistance = propd(c, Arts.MEDIUM_DISTANCE);
		art.city = prop(c, Arts.CITY);

		String photoIds = prop(c, Arts.PHOTO_IDS);
		if (!TextUtils.isEmpty(photoIds)) {
			String[] arr = photoIds.split("\\" + Utils.STR_SEP);
			art.photoIds = new ArrayList<String>();
			for (int i = 0; i < arr.length; i++) {
				art.photoIds.add(arr[i]);
			}
		}

		art.createdAt = prop(c, Arts.CREATED_AT);
		art.updatedAt = prop(c, Arts.UPDATED_AT);

		return art;
	}

	public static ArrayList<Art> artsFromCursor(Cursor c) {
		ArrayList<Art> results = new ArrayList<Art>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = 0; i < count; i++) {
				results.add(artFromCursor(c));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ArrayList<String> categoriesFromCursor(Cursor c) {
		ArrayList<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Categories.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ArrayList<String> neighborhoodsFromCursor(Cursor c) {
		ArrayList<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Neighborhoods.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ArrayList<String> artistsFromCursor(Cursor c) {
		ArrayList<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Artists.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ContentValues artToValues(Art art) {
		ContentValues cv = new ContentValues();
		cv.put(Arts.SLUG, art.slug);
		cv.put(Arts.TITLE, art.title);
		cv.put(Arts.CATEGORY, art.category);
		cv.put(Arts.NEIGHBORHOOD, art.neighborhood);
		cv.put(Arts.LOCATION_DESCRIPTION, art.locationDesc);
		cv.put(Arts.DESCRIPTION, art.description);
		cv.put(Arts.CREATED_AT, art.createdAt);
		cv.put(Arts.UPDATED_AT, art.updatedAt);
		cv.put(Arts.WARD, art.ward);
		cv.put(Arts.YEAR, art.year);
		cv.put(Arts.LATITUDE, art.latitude);
		cv.put(Arts.LONGITUDE, art.longitude);
		cv.put(Arts.MEDIUM_DISTANCE, art.mediumDistance);
		cv.put(Arts.CITY, art.city);
		if (art.artist != null) cv.put(Arts.ARTIST, art.artist.uuid);
		if (art.photoIds != null) cv.put(Arts.PHOTO_IDS, TextUtils.join(Utils.STR_SEP, art.photoIds));
		return cv;
	}

	public static ContentValues[] artsToValues(ArrayList<Art> arts) {
		int size = arts.size();
		ContentValues[] values = new ContentValues[size];
		for (int i = 0; i < size; i++)
			values[i] = artToValues(arts.get(i));
		return values;
	}
}
