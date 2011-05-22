package us.artaround.models;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
	private static final long serialVersionUID = 7985114174101659683L;

	public String artSlug;
	public String name;
	public String text;
	public String url;
	public String email;
	public Date createdAt;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Comment [artSlug=").append(artSlug).append(", name=").append(name).append(", text=")
				.append(text).append(", url=").append(url).append(", email=").append(email).append(", createdAt=")
				.append(createdAt).append("]");
		return builder.toString();
	}
}
