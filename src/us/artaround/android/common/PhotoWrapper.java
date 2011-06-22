package us.artaround.android.common;

import java.io.Serializable;

public class PhotoWrapper implements Serializable {
	private static final long serialVersionUID = -662225757938609228L;

	public String id;
	public String thumbUri;
	public String mediumUri;

	public PhotoWrapper(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PhotoWrapper [id=").append(id).append(", thumbUri=").append(thumbUri).append(", mediumUri=")
				.append(mediumUri).append("]");
		return builder.toString();
	}
}
