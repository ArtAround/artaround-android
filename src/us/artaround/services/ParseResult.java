package us.artaround.services;

import java.util.List;

import us.artaround.models.Art;

public class ParseResult {
	public int totalCount;
	public int count;
	public int page;
	public int perPage;
	public List<Art> art;

	@Override
	public String toString() {
		return "ParseResult [art.size=" + (art == null ? 0 : art.size()) + ", count=" + count + ", page=" + page
				+ ", perPage=" + perPage
				+ ", totalCount=" + totalCount + "]";
	}
}
