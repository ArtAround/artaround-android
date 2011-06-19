package us.artaround.android.parsers;

public class ParseResult {
	public int totalCount;
	public int count;
	public int page;
	public int perPage;

	@Override
	public String toString() {
		return "ParseResult [ count=" + count + ", page=" + page + ", perPage=" + perPage + ", totalCount="
				+ totalCount + "]";
	}
}
