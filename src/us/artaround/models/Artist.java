package us.artaround.models;

import java.io.Serializable;

public class Artist implements Serializable {
	private static final long serialVersionUID = 8990284057002777646L;

	public String uuid;
	public String name;

	public Artist(String uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	public Artist(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Artist [name=" + name + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Artist)) return false;
		Artist a = (Artist) o;
		return name.equals(a.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
