package us.artaround.models;

import java.util.Comparator;

public class ArtDispersionComparator implements Comparator<Art> {

	// descending
	@Override
	public int compare(Art a, Art b) {
		return (int) (b.mediumDistance - a.mediumDistance);
	}

}
