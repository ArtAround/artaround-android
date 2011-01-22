package us.artaround.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Art implements Serializable
{
	private static final long serialVersionUID = 7782974469872721623L;

	//public String uuid; // let's hope the slug is unique enough
	public String slug;
	public String category;
	public String title;
	public String city;
	public String url;

	public Date createdAt;
	public Date updatedAt;

	public int year, ward;

	public float latitude;
	public float longitude;
	public float mediumDistance;

	public String locationDesc;
	public String description;
	public String neighborhood;

	public Artist artist;
	public List<String> photoIds;
	public List<Comment> comments;

	public transient float _distanceFromCurrentPosition;
	public transient double _bearingFromCurrentPosition;

	public Art() {}

	@Override
	public String toString() {
		return "Art [slug=" + slug + ", category=" + category + ", title=" + title + ", city=" + city + ", url=" + url
				+ ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", year=" + year + ", ward=" + ward
				+ ", latitude=" + latitude + ", longitude=" + longitude + ", mediumDistance=" + mediumDistance
				+ ", locationDesc=" + locationDesc + ", neighborhood=" + neighborhood + ", artist=" + artist
				+ ", photoIds=" + photoIds + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o instanceof Art) return false;
		Art a = (Art) o;
		return this.latitude == a.latitude && this.longitude == a.longitude;
	}

	@Override
	public int hashCode() {
		return (int) (latitude * 101 + latitude);
	}
}
