package us.artaround.models;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
	private static final long serialVersionUID = 7985114174101659683L;

	public String artSlug;
	public String username;
	public String message;
	public String url;
	public Date date;

	@Override
	public String toString() {
		return "Comment [artSlug=" + artSlug + ", username=" + username + ", text=" + message + ", url=" + url
				+ ", createdAt=" + date + "]";
	}
}
