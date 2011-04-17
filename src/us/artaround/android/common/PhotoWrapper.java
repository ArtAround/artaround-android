package us.artaround.android.common;

import java.io.Serializable;

import android.graphics.drawable.Drawable;


public class PhotoWrapper implements Serializable {
	private static final long serialVersionUID = -662225757938609228L;
	public String id, uri;
	public Drawable drawable;

	public PhotoWrapper(String id, String uri) {
		this(id, uri, null);
	}

	public PhotoWrapper(String id, String uri, Drawable drawable) {
		this.id = id;
		this.uri = uri;
		this.drawable = drawable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof PhotoWrapper)) return false;
		PhotoWrapper other = (PhotoWrapper) obj;
		if (id == null) {
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (uri == null) {
			if (other.uri != null) return false;
		}
		else if (!uri.equals(other.uri)) return false;
		return true;
	}
}
