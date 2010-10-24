package us.artaround.models;

import java.util.Date;


public class Art {
	public String slug;
	public String category;
	public String title;

	public Date createdAt;
	public Date updatedAt;

	public int ward;

	public float latitude;
	public float longitude;

	public String locationDesc;
	public String neighborhood;

	// public Artist artist;
	public String artist;

	public String[] photoIds;
}
