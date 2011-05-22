package us.artaround.models;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
	private static final long serialVersionUID = 7985114174101659683L;

	public String artSlug;
	public String name;
	public String text;
	public String url;
	public Date createdAt;

	@Override
	public String toString() {
		return "Comment [artSlug=" + artSlug + ", name=" + name + ", text=" + text + ", url=" + url
				+ ", createdAt=" + createdAt + "]";
	}
}
