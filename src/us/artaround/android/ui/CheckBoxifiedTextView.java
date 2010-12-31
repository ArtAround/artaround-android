package us.artaround.android.ui;

import us.artaround.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CheckBoxifiedTextView extends LinearLayout {

	private TextView textView;
	private CheckBox checkBox;

	public CheckBoxifiedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		textView = (TextView) findViewById(R.id.text);
		checkBox = (CheckBox) findViewById(R.id.check);
	}

	public void setText(String text) {
		textView.setText(text);
	}

	public void setCheckBoxState(boolean checked) {
		checkBox.setChecked(checked);
	}

	public TextView getTextView() {
		return textView;
	}

	public CheckBox getCheckBox() {
		return checkBox;
	}
}
