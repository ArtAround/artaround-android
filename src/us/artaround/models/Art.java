package us.artaround.models;

import java.util.Arrays;
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

	public Artist artist;
	public String[] photoIds;

	@Override
	public String toString() {
		return "Art [artist=" + artist + ", category=" + category + ", createdAt=" + createdAt + ", latitude="
				+ latitude + ", locationDesc=" + locationDesc + ", longitude=" + longitude + ", neighborhood="
				+ neighborhood + ", photoIds=" + Arrays.toString(photoIds) + ", slug=" + slug + ", title=" + title
				+ ", updatedAt=" + updatedAt + ", ward=" + ward + "]";
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
