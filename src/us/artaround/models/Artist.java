package us.artaround.models;

public class Artist {
	public String slug;
	public String name;

	public Artist() {}

	public Artist(String slug, String name) {
		this.slug = slug;
		this.name = name;
	}

	public Artist(String slug) {
		this(slug, slug);
	}

	@Override
	public String toString() {
		return "Artist [name=" + name + ", slug=" + slug + "]";
	}
}
