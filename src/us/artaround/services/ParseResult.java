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
		StringBuilder build = new StringBuilder();
		build.append("totalCount=").append(totalCount);
		build.append(",count=").append(count);
		build.append(",page=").append(page);
		build.append(",perPage").append(perPage);
		return build.toString();
	}

}
