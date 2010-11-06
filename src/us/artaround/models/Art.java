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
	public float mediumDistance;

	public String locationDesc;
	public String neighborhood;

	// public Artist artist;
	public String artist;

	public String[] photoIds;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Art={");
		builder.append("slug: ").append(slug);
		builder.append(",title: ").append(title);
		builder.append(",category: ").append(category);
		builder.append(",neighborhood: ").append(neighborhood);
		builder.append(",createdAt: ").append(createdAt);
		builder.append(",updatedAt: ").append(updatedAt);
		builder.append(",latitude: ").append(latitude);
		builder.append(",longitude: ").append(longitude);
		builder.append(",locationDesc: ").append(locationDesc);
		builder.append(",artist: ").append(artist);
		builder.append("}");
		return builder.toString();
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
