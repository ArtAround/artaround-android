package us.artaround.models;

import java.util.Date;

public class Comment {
	public String artSlug;
	public String username;
	public String text;
	public String url;
	public Date createdAt;

	@Override
	public String toString() {
		return "Comment [artSlug=" + artSlug + ", username=" + username + ", text=" + text + ", url=" + url
				+ ", createdAt=" + createdAt + "]";
	}
}
