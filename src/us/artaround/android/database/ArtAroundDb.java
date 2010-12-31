package us.artaround.android.database;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import us.artaround.android.commons.Utils;
import us.artaround.models.Art;
import us.artaround.models.Artist;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class ArtAroundDb {
	public static final String ARTAROUND_AUTHORITY = "us.artaround";

	public static final String[] ARTS_PROJECTION = { Arts.ID, Arts.TITLE, Arts.CATEGORY, Arts.CREATED_AT,
			Arts.UPDATED_AT, Arts.LATITUDE, Arts.LONGITUDE, Arts.NEIGHBORHOOD, Arts.PHOTO_IDS, Arts.WARD,
			Arts.LOCATION_DESCRIPTION, Arts.ARTIST };

	public static final String[] CATEGORIES_PROJECTION = { Categories.NAME };
	public static final String[] NEIGHBORHOODS_PROJECTION = { Neighborhoods.NAME };
	public static final String[] ARTISTS_PROJECTION = { Artists.NAME };

	// We don't need to instantiate this class.
	private ArtAroundDb() {}

	public static final class Arts implements BaseColumns {
		public static final String TABLE_NAME = "arts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String DEFAULT_SORT_ORDER = "id ASC";

		public static final String ID = "id";
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
		public static final String ARTIST = "artist";
	}

	public static final class Artists implements BaseColumns {
		public static final String TABLE_NAME = "artists";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String NAME = "name";
	}

	public static final class Categories implements BaseColumns {
		public static final String TABLE_NAME = "categories";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;

		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String NAME = "name";
	}

	public static final class Neighborhoods implements BaseColumns {
		public static final String TABLE_NAME = "neighborhoods";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;

		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String NAME = "name";
	}

	public static final class Filters implements BaseColumns {
		public static final String TABLE_NAME = "filters";
		public static final Uri CONTENT_URI = Uri.parse("content://" + ARTAROUND_AUTHORITY + "/" + TABLE_NAME);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.us.artaround." + TABLE_NAME;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.us.artaround." + TABLE_NAME;

		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String TITLE = "title";
		public static final String PROPERTY = "property";
		public static final String VALUE = "value";
	}

	public static String createArtsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Arts.TABLE_NAME).append(" (");
		b.append(Arts._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Arts.ID).append(" TEXT,");
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
		b.append(Arts.ARTIST).append(" TEXT);");

		// index on id
		b.append("CREATE INDEX idx ON ").append(Arts.TABLE_NAME).append(".").append(Arts.ID).append(";");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createArtistsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Artists.TABLE_NAME).append(" (");
		b.append(Artists._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Artists.NAME).append(" TEXT);");

		// index on name
		b.append("CREATE INDEX idx ON ").append(Artists.TABLE_NAME).append(".").append(Artists.NAME).append(";");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createCategoriesTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Categories.TABLE_NAME).append(" (");
		b.append(Categories._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Categories.NAME).append(" TEXT);");

		// index on name
		b.append("CREATE INDEX idx ON ").append(Categories.TABLE_NAME).append(".").append(Categories.NAME).append(";");

		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createNeighborhoodsTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Neighborhoods.TABLE_NAME).append(" (");
		b.append(Neighborhoods._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Neighborhoods.NAME).append(" TEXT);");

		// index on name
		b.append("CREATE INDEX idx ON ").append(Neighborhoods.TABLE_NAME).append(".").append(Neighborhoods.NAME)
				.append(";");
		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}

	public static String createFiltersTable() {
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(Filters.TABLE_NAME).append(" (");
		b.append(Filters._ID).append(" INTEGER PRIMARY KEY,");
		b.append(Filters.TITLE).append(" TEXT,");
		b.append(Filters.PROPERTY).append(" TEXT,");
		b.append(Filters.VALUE).append(" TEXT);");

		// index on name
		b.append("CREATE INDEX idx ON ").append(Filters.TABLE_NAME).append(".").append(Filters.TITLE).append(";");
		String str = b.toString();
		//Log.d(Utils.TAG, "SQL: " + str);
		return str;
	}


	//--- utility methods ---
	public static String prop(Cursor cursor, String name) {
		return cursor.getString(cursor.getColumnIndex(name));
	}

	public static Art artFromCursor(Cursor c) {
		Art art = new Art();

		art.id = prop(c, Arts.ID);
		art.category = prop(c, Arts.CATEGORY);
		art.title = prop(c, Arts.TITLE);
		art.locationDesc = prop(c, Arts.LOCATION_DESCRIPTION);
		art.neighborhood = prop(c, Arts.NEIGHBORHOOD);
		art.ward = c.getInt(c.getColumnIndex(Arts.WARD));
		art.latitude = c.getFloat(c.getColumnIndex(Arts.LATITUDE));
		art.longitude = c.getFloat(c.getColumnIndex(Arts.LONGITUDE));
		art.artist = new Artist(prop(c, Arts.ARTIST));

		String photoIds = prop(c, Arts.PHOTO_IDS);
		if (!TextUtils.isEmpty(photoIds)) art.photoIds = photoIds.split("\\" + Utils.STR_SEP);

		try {
			art.createdAt = Utils.parseDate(prop(c, Arts.CREATED_AT));
			art.updatedAt = Utils.parseDate(prop(c, Arts.UPDATED_AT));
		}
		catch (ParseException e) {
			Log.w(Utils.TAG, "SQL: Could not parse art dates", e);
		}
		return art;
	}

	public static List<Art> artsFromCursor(Cursor c) {
		List<Art> results = new ArrayList<Art>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = 0; i < count; i++) {
				results.add(artFromCursor(c));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ContentValues artToValues(Art art) {
		ContentValues cv = new ContentValues(12);
		cv.put(Arts.ID, art.id);
		cv.put(Arts.TITLE, art.title);
		cv.put(Arts.CATEGORY, art.category);
		cv.put(Arts.NEIGHBORHOOD, art.neighborhood);
		cv.put(Arts.LOCATION_DESCRIPTION, art.locationDesc);
		cv.put(Arts.CREATED_AT, Utils.formatDate(art.createdAt));
		cv.put(Arts.UPDATED_AT, Utils.formatDate(art.updatedAt));
		cv.put(Arts.WARD, art.ward);
		cv.put(Arts.LATITUDE, art.latitude);
		cv.put(Arts.LONGITUDE, art.longitude);
		if (art.artist != null) cv.put(Arts.ARTIST, art.artist.name);
		if (art.photoIds != null) cv.put(Arts.PHOTO_IDS, TextUtils.join(Utils.STR_SEP, art.photoIds));
		return cv;
	}

	public static List<String> categoriesFromCursor(Cursor c) {
		List<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Categories.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static List<String> neighborhoodsFromCursor(Cursor c) {
		List<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Neighborhoods.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static List<String> artistsFromCursor(Cursor c) {
		List<String> results = new ArrayList<String>();
		if (c != null && c.moveToFirst()) {
			int count = c.getCount();
			for (int i = count - 1; i >= 0; i--) {
				results.add(prop(c, Artists.NAME));
				c.moveToNext();
			}
		}
		return results;
	}

	public static ContentValues categoryToValues(String category) {
		ContentValues vals = new ContentValues(1);
		vals.put(Categories.NAME, category);
		return vals;
	}

	public static ContentValues neighborhoodToValues(String category) {
		ContentValues vals = new ContentValues(1);
		vals.put(Neighborhoods.NAME, category);
		return vals;
	}

	public static ContentValues artistToValues(Artist artist) {
		ContentValues vals = new ContentValues(1);
		vals.put(Artists.NAME, artist.name);
		return vals;
	}
}
