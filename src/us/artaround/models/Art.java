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

}
