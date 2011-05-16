package us.artaround.android.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class InstantAutoComplete extends AutoCompleteTextView {

	public InstantAutoComplete(Context context) {
		super(context);
	}

	public InstantAutoComplete(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InstantAutoComplete(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean enoughToFilter() {
		return true;
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused) {
			performFiltering(getText(), 0);
		}
	}
}
