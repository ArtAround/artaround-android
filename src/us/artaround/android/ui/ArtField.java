package us.artaround.android.ui;

import us.artaround.R;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArtField extends LinearLayout {

	private TextView label;
	private EditText value;

	private String savedLabel;
	private String savedValue;

	private int orientation;
	private int colorBgNormal, colorBgEditing;
	private int colorTxtNormal, colorTxtEditing;
	private boolean multiline;
	private boolean labelLeft;

	public ArtField(Context context, AttributeSet attrs) {
		super(context, attrs);

		Resources res = context.getResources();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArtField);

		labelLeft = a.getBoolean(R.styleable.ArtField_label_left, true);
		multiline = a.getBoolean(R.styleable.ArtField_multiline, false);

		colorBgNormal = res.getColor(a.getResourceId(R.styleable.ArtField_color_bg_normal, R.color.art_field_value_bg));
		colorBgEditing = res.getColor(a.getResourceId(R.styleable.ArtField_color_bg_editing,
				R.color.art_field_value_editing_bg));
		colorTxtNormal = res.getColor(a.getResourceId(R.styleable.ArtField_color_txt_normal,
				R.color.art_field_value_txt));
		colorTxtEditing = res.getColor(a.getResourceId(R.styleable.ArtField_color_txt_editing,
				R.color.art_field_value_editing_txt));

		a.recycle();

		orientation = labelLeft ? HORIZONTAL : VERTICAL;
		setOrientation(orientation);

		initUi(context, attrs);
	}

	private void initUi(Context context, AttributeSet attrs) {
		LayoutInflater inflater = LayoutInflater.from(context);

		label = (TextView) inflater.inflate(R.layout.art_field_label, null);
		value = (EditText) inflater.inflate(R.layout.art_field_value, null);

		LinearLayout.LayoutParams params = new LayoutParams(context, attrs);
		LinearLayout.LayoutParams params2 = new LayoutParams(context, attrs);

		if (VERTICAL == orientation) {
			params.width = LayoutParams.FILL_PARENT;
			params2.width = LayoutParams.FILL_PARENT;

		}
		else {
			params.width = 0;
			params.weight = 1;

			params2.width = 0;
			params2.weight = 3;
		}

		params.height = LayoutParams.WRAP_CONTENT;
		label.setLayoutParams(params);

		params2.height = LayoutParams.WRAP_CONTENT;
		value.setLayoutParams(params2);

		value.setSingleLine(!multiline);
		value.setFocusable(false); // default

		addView(label);
		addView(value);

	}

	@Override
	public Parcelable onSaveInstanceState() {
		//begin boilerplate code that allows parent classes to save state
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		//end

		ss.savedLabel = label.getText().toString();
		ss.savedValue = value.getText().toString();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		//begin boilerplate code so parent classes can restore state
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		//end

		savedLabel = ss.savedLabel;
		savedValue = ss.savedValue;

		setLabelText(savedLabel);
		setValueText(savedValue);
	}

	static class SavedState extends BaseSavedState {
		String savedLabel, savedValue;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			savedLabel = in.readString();
			savedValue = in.readString();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeString(savedLabel);
			out.writeString(savedValue);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	public TextView getLabel() {
		return label;
	}

	public void setLabelText(String text) {
		label.setText(text);
	}

	public EditText getValue() {
		return value;
	}

	public void setValueText(String text) {
		value.setText(text);
	}

	public void setEditing(boolean isEditing) {
		if (isEditing) {
			value.setFocusableInTouchMode(true);
		}
		else {
			value.setFocusable(false);
		}

		value.setCursorVisible(isEditing);
		value.setBackgroundColor(isEditing ? colorBgEditing : colorBgNormal);
		value.setTextColor(isEditing ? colorTxtEditing : colorTxtNormal);
	}
}
