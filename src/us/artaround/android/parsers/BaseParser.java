package us.artaround.android.parsers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.Art;
import us.artaround.models.Artist;
import us.artaround.models.Comment;
import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

public class BaseParser {
	public static final String TAG = "ArtAround.Parser";

	public static final int TYPE_ARTS = 0;
	public static final int TYPE_ARTISTS = 1;
	public static final int TYPE_CATEGORIES = 2;
	public static final int TYPE_NEIGHBORHOODS = 3;
	public static final int TYPE_COMMENTS = 4;
	public static final int TYPE_RESPONSE = 5;
	public static final int TYPE_NONE = 6;


	private static final HashMap<String, Artist> temp = new HashMap<String, Artist>();
	
	private BaseParser() {}

	public static void parseResponse(StreamData data) {
		int parserType = data.getParserType();

		switch (parserType) {
		case TYPE_ARTS:
			parseArts(data);
			break;
		case TYPE_ARTISTS:
			parseArtists(data);
			break;
		case TYPE_CATEGORIES:
			parseCategories(data);
			break;
		case TYPE_NEIGHBORHOODS:
			parseNeighborhoods(data);
			break;
		case TYPE_COMMENTS:
			parseComments(data);
			break;
		case TYPE_RESPONSE:
			parseStringResponse(data, SUCCESS_KEY);
			break;
		case TYPE_NONE:
		default:
			//do nothing
		}
	}

	private static void closeParser(JsonParser jp) {
		if (jp != null) {
			try {
				jp.close();
			}
			catch (IOException e) {
				Log.w(TAG, "Could not close json parser.", e);
			}
		}
	}

	private static void parseArts(StreamData data) {
		JsonFactory f = new JsonFactory();
		JsonParser jp = null;

		try {
			jp = f.createJsonParser(data.getHttpData());

			JsonToken token = jp.nextToken();
			if (token != JsonToken.START_OBJECT) {
				Utils.d(TAG, "parseArt(): cannot move current json token on START_OBJECT.");
				return;
			}

			ArrayList<ContentValues> arts = new ArrayList<ContentValues>();
			ArrayList<ContentValues> artists = new ArrayList<ContentValues>();
			ArrayList<ContentValues> categories = new ArrayList<ContentValues>();
			ArrayList<ContentValues> neighborhoods = new ArrayList<ContentValues>();

			ParseResult result = new ParseResult();

			String city = ServiceFactory.getCurrentCity().name;

			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String key = jp.getCurrentName();
				jp.nextValue();

				if (PAGE_KEY.equalsIgnoreCase(key)) {
					while (jp.nextToken() != JsonToken.END_OBJECT) {
						String pageKey = jp.getCurrentName();
						jp.nextValue();

						if (COUNT_KEY.equalsIgnoreCase(pageKey)) {
							result.count = jp.getIntValue();
						}
						else if (PAGE_KEY.equalsIgnoreCase(pageKey)) {
							result.page = jp.getIntValue();
						}
						else if (PER_PAGE_KEY.equalsIgnoreCase(pageKey)) {
							result.perPage = jp.getIntValue();
						}
						else {
							jp.skipChildren();
						}
					}
				}
				else if (TOTAL_COUNT_KEY.equalsIgnoreCase(key)) {
					result.totalCount = jp.getIntValue();
				}
				else if (ARTS_KEY.equalsIgnoreCase(key)) {

					while (jp.nextToken() != JsonToken.END_ARRAY) {

						ContentValues art = new ContentValues();
						ContentValues category = new ContentValues();
						ContentValues neighborhood = new ContentValues();

						while (jp.nextToken() != JsonToken.END_OBJECT) {
							String artKey = jp.getCurrentName();
							jp.nextValue();

							if (SLUG_KEY.equalsIgnoreCase(artKey)) {
								String slug = jp.getText().trim();
								art.put(Arts.SLUG, slug);
							}
							else if (TITLE_KEY.equalsIgnoreCase(artKey)) {
								String title = jp.getText().trim();
								art.put(Arts.TITLE, title);
							}
							else if (CATEGORY_KEY.equalsIgnoreCase(artKey)) {
								String cat = jp.getText().trim();
								art.put(Arts.CATEGORY, cat);
								category.put(Categories.NAME, cat);
								categories.add(category);
							}
							else if (NEIGHBORHOOD_KEY.equalsIgnoreCase(artKey)) {
								String nbhd = jp.getText().trim();
								art.put(Arts.NEIGHBORHOOD, nbhd);
								neighborhood.put(Neighborhoods.NAME, nbhd);
								neighborhoods.add(neighborhood);
							}
							else if (LOCATION_DESCRIPTION_KEY.equalsIgnoreCase(artKey)) {
								String desc = jp.getText().trim();
								art.put(Arts.LOCATION_DESCRIPTION, desc);
							}
							else if (LOCATION_KEY.equalsIgnoreCase(artKey)) {
								int index = 0;
								// [latitude, longitude]
								while (jp.nextToken() != JsonToken.END_ARRAY) {
									if (jp.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
										if (index == 0) {
											float lat = jp.getFloatValue();
											art.put(Arts.LATITUDE, lat);
										}
										else if (index == 1) {
											float lon = jp.getFloatValue();
											art.put(Arts.LONGITUDE, lon);
										}
										index++;
									}
								}
							}
							else if (WARD_KEY.equalsIgnoreCase(artKey)) {
								String ward = jp.getText().trim();
								try {
									art.put(Arts.WARD, ward);
								}
								catch (NumberFormatException e) {
									Utils.w(TAG, "Could not parse ward " + ward);
								}
							}
							else if (YEAR_KEY.equalsIgnoreCase(artKey)) {
								String year = jp.getText().trim();
								try {
									art.put(Arts.YEAR, year);
								}
								catch (NumberFormatException e) {
									Utils.w(TAG, "Could not parse year " + year);
								}
							}
							else if (CREATED_AT_KEY.equalsIgnoreCase(artKey)) {
								String date = jp.getText().trim();
								art.put(Arts.CREATED_AT, date);
							}
							else if (UPDATED_AT_KEY.equalsIgnoreCase(artKey)) {
								String date = jp.getText().trim();
								art.put(Arts.UPDATED_AT, date);
							}
							else if (PHOTO_IDS_KEY.equalsIgnoreCase(artKey)) {
								List<String> photoIds = new ArrayList<String>();
								//[id1, id2, ...]
								while (jp.nextToken() != JsonToken.END_ARRAY) {
									photoIds.add(jp.getText());
								}
								art.put(Arts.PHOTO_IDS, TextUtils.join(Utils.STR_SEP, photoIds));
							}
							else if (ARTIST_KEY.equalsIgnoreCase(artKey)) {
								String name = jp.getText().trim();

								if (temp.containsKey(name)) { // we already parsed this artist
									art.put(Arts.ARTIST, temp.get(name).uuid);
								}
								else if (!TextUtils.isEmpty(name)) {
									String artistUUID = UUID.randomUUID().toString();
									Artist newArtist = new Artist(artistUUID, name);
									temp.put(name, newArtist);

									ContentValues artist = new ContentValues();
									artist.put(Artists.SLUG, artistUUID);
									artist.put(Artists.NAME, name);
									artists.add(artist);

									art.put(Arts.ARTIST, artistUUID);
								}
							}
							else if (DESCRIPTION_KEY.equalsIgnoreCase(artKey)) {
								String desc = jp.getText().trim();
								art.put(Arts.DESCRIPTION, desc);
							}
							else {
								jp.skipChildren();
							}
						}

						// the art has some fields
						if (art.size() > 0) {
							art.put(Arts.CITY, city);
							arts.add(art);
						}

					} // end arts parsing
				}
				else {
					jp.skipChildren();
				}
			}

			jp.close(); // close a.s.a.p

			Utils.d(Utils.TAG, "Parsed arts =" + arts.size());
			Utils.d(Utils.TAG, "Parsed artists =" + artists.size());

			ArtAroundProvider.contentResolver
					.bulkInsert(Arts.CONTENT_URI, arts.toArray(new ContentValues[arts.size()]));

			// save artists
			ArtAroundProvider.contentResolver.bulkInsert(Artists.CONTENT_URI,
					artists.toArray(new ContentValues[artists.size()]));

			// save categories
			ArtAroundProvider.contentResolver.bulkInsert(Categories.CONTENT_URI,
					categories.toArray(new ContentValues[categories.size()]));

			// save neighborhoods
			ArtAroundProvider.contentResolver.bulkInsert(Neighborhoods.CONTENT_URI,
					neighborhoods.toArray(new ContentValues[neighborhoods.size()]));

			Boolean notifyMe = (Boolean) data.getAuxData()[0];
			if (notifyMe != null && notifyMe) {
				ArtAroundProvider.contentResolver.notifyChange(Arts.CONTENT_URI, null);
				ArtAroundProvider.contentResolver.notifyChange(Artists.CONTENT_URI, null);
				ArtAroundProvider.contentResolver.notifyChange(Categories.CONTENT_URI, null);
				ArtAroundProvider.contentResolver.notifyChange(Neighborhoods.CONTENT_URI, null);
			}

			data.setAuxData(result);
		}
		catch (JsonParseException e) {
			Log.w(TAG, "parseArt(): exception", e);
		}
		catch (IOException e) {
			Log.w(TAG, "parseArt(): exception", e);
		}
		finally {
			closeParser(jp);
		}
	}

	private static void parseArtists(StreamData data) {
		// TODO Auto-generated method stub
	}

	private static void parseCategories(StreamData data) {
		JsonFactory f = new JsonFactory();
		JsonParser jp = null;

		try {
			jp = f.createJsonParser(data.getHttpData());

			JsonToken token = jp.nextToken();
			if (token != JsonToken.START_ARRAY) {
				Utils.d(TAG, "parseCategories(): cannot move current json token on START_ARRAY.");
				return;
			}

			ArrayList<ContentValues> categories = new ArrayList<ContentValues>();

			while (jp.nextToken() != JsonToken.END_ARRAY) {
				ContentValues category = new ContentValues(1);
				category.put(Categories.NAME, jp.getText().trim());
				categories.add(category);
			}

			jp.close(); // close a.s.a.p

			ArtAroundProvider.contentResolver.bulkInsert(Categories.CONTENT_URI,
					categories.toArray(new ContentValues[categories.size()]));

			Boolean notifyMe = (Boolean) data.getAuxData()[0];
			if (notifyMe != null && notifyMe) {
				ArtAroundProvider.contentResolver.notifyChange(Categories.CONTENT_URI, null);
			}

			data.setHttpData(null);
		}
		catch (JsonParseException e) {
			Log.w(TAG, "parseCategories(): exception", e);
		}
		catch (IOException e) {
			Log.w(TAG, "parseCategories(): exception", e);
		}
		finally {
			closeParser(jp);
		}
	}

	private static void parseNeighborhoods(StreamData data) {
		JsonFactory f = new JsonFactory();
		JsonParser jp = null;

		try {
			jp = f.createJsonParser(data.getHttpData());

			JsonToken token = jp.nextToken();
			if (token != JsonToken.START_ARRAY) {
				Utils.d(TAG, "parseNeighborhoods(): cannot move current json token on START_ARRAY.");
				return;
			}

			ArrayList<ContentValues> neighborhoods = new ArrayList<ContentValues>();

			while (jp.nextToken() != JsonToken.END_ARRAY) {
				ContentValues n = new ContentValues(1);
				n.put(Neighborhoods.NAME, jp.getText().trim());
				neighborhoods.add(n);
			}

			jp.close(); // close a.s.a.p

			ArtAroundProvider.contentResolver.bulkInsert(Neighborhoods.CONTENT_URI,
					neighborhoods.toArray(new ContentValues[neighborhoods.size()]));

			Boolean notifyMe = (Boolean) data.getAuxData()[0];
			if (notifyMe != null && notifyMe) {
				ArtAroundProvider.contentResolver.notifyChange(Neighborhoods.CONTENT_URI, null);
			}

			data.setHttpData(null);
		}
		catch (JsonParseException e) {
			Log.w(TAG, "parseNeighborhoods(): exception", e);
		}
		catch (IOException e) {
			Log.w(TAG, "parseNeighborhoods(): exception", e);
		}
		finally {
			closeParser(jp);
		}
	}

	private static void parseComments(StreamData data) {
		JsonFactory f = new JsonFactory();
		JsonParser jp = null;

		try {
			jp = f.createJsonParser(data.getHttpData());

			JsonToken token = jp.nextToken();
			if (token != JsonToken.START_OBJECT) {
				Utils.d(TAG, "parseComments(): cannot move current json token on START_OBJECT.");
				return;
			}

			List<Comment> comments = new ArrayList<Comment>();
			String artSlug = (String) data.getAuxData()[0];

			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String key = jp.getCurrentName();
				jp.nextValue();

				if (ART_KEY.equalsIgnoreCase(key)) {
					while (jp.nextToken() != JsonToken.END_OBJECT) {
						String artKey = jp.getCurrentName();
						jp.nextValue();

						if (COMMENTS_KEY.equalsIgnoreCase(artKey)) {
							while (jp.nextToken() != JsonToken.END_ARRAY) {
								Comment comment = new Comment();
								comment.artSlug = artSlug;

								while (jp.nextToken() != JsonToken.END_OBJECT) {
									String commKey = jp.getCurrentName();
									jp.nextValue();

									if (NAME_KEY.equalsIgnoreCase(commKey)) {
										comment.name = jp.getText().trim();
									}
									else if (TEXT_KEY.equalsIgnoreCase(commKey)) {
										comment.text = jp.getText().trim();
									}
									else if (CREATED_AT_KEY.equalsIgnoreCase(commKey)) {
										String date = jp.getText().trim();
										try {
											if (date != null) {
												comment.createdAt = Utils.sqlDateFormatter.parse(date);
											}
										}
										catch (ParseException e) {
											Utils.w(TAG, "Date not parsed correctly: " + date);
										}
									}
									else if (URL_KEY.equalsIgnoreCase(commKey)) {
										comment.url = jp.getText().trim();
									}
									else {
										jp.skipChildren();
									}
								}

								comments.add(comment);
							}
						}
						else {
							jp.skipChildren();
						}
					}
				}
				else {
					jp.skipChildren();
				}
			}

			jp.close(); // close a.s.a.p

			data.setHttpData(null);
			data.setAuxData(comments);

		}
		catch (JsonParseException e) {
			Log.w(TAG, "parseComments(): exception", e);
		}
		catch (IOException e) {
			Log.w(TAG, "parseComments(): exception", e);
		}
		finally {
			closeParser(jp);
		}
	}

	private static void parseStringResponse(StreamData data, String keyName) {
		JsonFactory f = new JsonFactory();
		JsonParser jp = null;

		try {
			jp = f.createJsonParser(data.getHttpData());

			JsonToken token = jp.nextToken();
			if (token != JsonToken.START_OBJECT) {
				Utils.d(TAG,
						"parseStringResponse(): cannot move current json token on START_OBJECT.");
				return;
			}

			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String key = jp.getCurrentName();
				jp.nextValue();

				if (keyName.equalsIgnoreCase(key)) {
					data.setAuxData(jp.getText());
				} else {
					jp.skipChildren();
				}
			}
		}
		catch (JsonParseException e) {
			Log.w(TAG, "parseStringResponse(): exception", e);
		}
		catch (IOException e) {
			Log.w(TAG, "parseStringResponse(): exception", e);
		}
		finally {
			closeParser(jp);
		}

	}

	public static String writeArt(Art art) {
		JsonFactory f = new JsonFactory();
		StringWriter writer = new StringWriter();
		try {
			JsonGenerator g = f.createJsonGenerator(writer);
			g.writeStartObject();
			g.writeStringField(TITLE_KEY, art.title);
			g.writeStringField(CATEGORY_KEY, art.category);
			g.writeStringField(NEIGHBORHOOD_KEY, art.neighborhood);
			g.writeStringField(LOCATION_DESCRIPTION_KEY, art.locationDesc);
			g.writeStringField(DESCRIPTION_KEY, art.description);

			if (art.artist != null) {
				g.writeStringField(ARTIST_KEY, art.artist.name);
			}
			g.writeNumberField(WARD_KEY, art.ward);
			g.writeNumberField(YEAR_KEY, art.year);

			g.writeArrayFieldStart(LOCATION_KEY);
			g.writeNumber(art.latitude);
			g.writeNumber(art.longitude);
			g.writeEndArray();

			g.writeEndObject();
			g.flush();
			g.close();

			return writer.toString();
		}
		catch (IOException e) {
			Log.w(TAG, "writeArt(): exception", e);
			return null;
		}
		finally {
			closeWriter(writer);
		}
	}

	public static String writeComment(Comment comment) {
		JsonFactory f = new JsonFactory();
		StringWriter writer = new StringWriter();
		try {
			JsonGenerator g = f.createJsonGenerator(writer);
			g.writeStartObject();
			g.writeStringField(NAME_KEY, comment.name);
			g.writeStringField(TEXT_KEY, comment.text);
			g.writeStringField(URL_KEY, comment.url);
			g.writeEndObject();

			g.flush();
			g.close();

			return writer.toString();
		}
		catch (IOException e) {
			Log.w(TAG, "writeArt(): exception", e);
			return null;
		}
		finally {
			closeWriter(writer);
		}
	}

	private static final void closeWriter(Writer wr) {
		try {
			wr.close();
		}
		catch (IOException e) {
			Log.w(TAG, "Could not close the writer.", e);
		}
	}

	//--- Keys ---
	private static final String ART_KEY = "art";
	private static final String ARTS_KEY = "arts";
	private static final String TOTAL_COUNT_KEY = "total_count";
	private static final String PER_PAGE_KEY = "per_page";
	private static final String PAGE_KEY = "page";
	private static final String COUNT_KEY = "count";
	private static final String SLUG_KEY = "slug";
	private static final String TITLE_KEY = "title";
	private static final String CATEGORY_KEY = "category";
	private static final String CREATED_AT_KEY = "created_at";
	private static final String UPDATED_AT_KEY = "updated_at";
	private static final String YEAR_KEY = "year";
	private static final String WARD_KEY = "ward";
	private static final String LOCATION_KEY = "location";
	private static final String LOCATION_DESCRIPTION_KEY = "location_description";
	private static final String NEIGHBORHOOD_KEY = "neighborhood";
	private static final String ARTIST_KEY = "artist";
	private static final String PHOTO_IDS_KEY = "flickr_ids";
	private static final String DESCRIPTION_KEY = "description";
	private static final String COMMENTS_KEY = "comments";
	private static final String NAME_KEY = "name";
	private static final String URL_KEY = "url";
	private static final String TEXT_KEY = "text";
	private static final String SUCCESS_KEY = "success";
}
