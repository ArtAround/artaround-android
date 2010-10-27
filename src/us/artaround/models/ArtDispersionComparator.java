package us.artaround.models;

import java.util.Comparator;

public class ArtDispersionComparator implements Comparator<Art>{

	@Override
	public int compare(Art a, Art b) {
		return (int) (a.mediumDistance - b.mediumDistance);
	}

}
