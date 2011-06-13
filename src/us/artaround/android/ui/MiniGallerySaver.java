package us.artaround.android.ui;

import android.os.Bundle;

public interface MiniGallerySaver {
	void saveMiniGalleryState(Bundle args);

	Bundle restoreMiniGalleryState();
}
