package us.artaround.android.ui;

import android.os.Bundle;

public interface GallerySaver {
	void saveGalleryState(Bundle args);

	Bundle restoreGalleryState();
}
