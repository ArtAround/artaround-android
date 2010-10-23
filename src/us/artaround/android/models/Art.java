package us.artaround.android.models;


public class Art {
	public String id;
	public String slug;
	public String category;
	public String title;

	public long createdAt;
	public long updatedAt;

	public float pitch;
	public int zoom;
	public int ward;

	public boolean approved;

	public long latitude;
	public long longitude;
	public String locationDesc;
	public String neighborhood;

	public Artist artist;

	public String[] photoIds;
}
