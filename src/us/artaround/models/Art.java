package us.artaround.models;

import java.io.Serializable;
import java.util.ArrayList;

public class Art implements Serializable
{
	private static final long serialVersionUID = 7782974469872721623L;

	//public String uuid; // let's hope the slug is unique enough
	public String slug;
	public String category;
	public String title;
	public String city;
	public String url;

	public String createdAt;
	public String updatedAt;

	public int year, ward;

	public double latitude;
	public double longitude;
	public double mediumDistance;

	public String locationDesc;
	public String description;
	public String neighborhood;

	public Artist artist;
	public ArrayList<String> photoIds;
	public ArrayList<Comment> comments;

	public transient double _distanceFromCurrentPosition;
	public transient double _bearingFromCurrentPosition;

	public Art() {}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Art [slug=");
		builder.append(slug);
		builder.append(", category=");
		builder.append(category);
		builder.append(", title=");
		builder.append(title);
		builder.append(", city=");
		builder.append(city);
		builder.append(", url=");
		builder.append(url);
		builder.append(", createdAt=");
		builder.append(createdAt);
		builder.append(", updatedAt=");
		builder.append(updatedAt);
		builder.append(", year=");
		builder.append(year);
		builder.append(", ward=");
		builder.append(ward);
		builder.append(", latitude=");
		builder.append(latitude);
		builder.append(", longitude=");
		builder.append(longitude);
		builder.append(", mediumDistance=");
		builder.append(mediumDistance);
		builder.append(", locationDesc=");
		builder.append(locationDesc);
		builder.append(", description=");
		builder.append(description);
		builder.append(", neighborhood=");
		builder.append(neighborhood);
		builder.append(", artist=");
		builder.append(artist);
		builder.append(", photoIds=");
		builder.append(photoIds);
		builder.append(", comments=");
		builder.append(comments);
		builder.append("]");
		return builder.toString();
	}
}
