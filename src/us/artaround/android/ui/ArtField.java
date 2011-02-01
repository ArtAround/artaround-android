package us.artaround.android.ui;

import us.artaround.R;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArtField extends LinearLayout {

	private TextView label;
	private AutoCompleteTextView value;

	private String savedLabel;
	private String savedValue;

	// see http://developer.android.com/reference/android/widget/TextView.html#attr_android:inputType
	private final static int INPUT_TYPE_TEXT = 1;
	private final static int INPUT_TYPE_TEXT_MULTILINE = 131072;

	private final int colorTxtNormal, colorTxtEditing;
	private final int minLines;
	private final int inputType;
	private final boolean multiLine;

	private final Context context;

	public ArtField(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		Resources res = context.getResources();
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArtField);

		multiLine = a.getBoolean(R.styleable.ArtField_multiLine, false);
		minLines = a.getInt(R.styleable.ArtField_minLines, 1);

		colorTxtNormal = res
				.getColor(a.getResourceId(R.styleable.ArtField_colorTxtNormal, R.color.art_field_value_txt));
		colorTxtEditing = res.getColor(a.getResourceId(R.styleable.ArtField_colorTxtEditing,
				R.color.art_field_value_editing_txt));

		inputType = a.getInt(R.styleable.ArtField_inputType, INPUT_TYPE_TEXT);

		a.recycle();

		setOrientation(VERTICAL);

		initUi(context, attrs);
	}

	private void initUi(Context context, AttributeSet attrs) {
		LayoutInflater inflater = LayoutInflater.from(context);

		label = (TextView) inflater.inflate(R.layout.art_field_label, null);
		value = (AutoCompleteTextView) inflater.inflate(R.layout.art_field_value, null);

		addView(label);
		addView(value);

		LinearLayout.LayoutParams lparams = (LayoutParams) label.getLayoutParams();
		LinearLayout.LayoutParams vparams = (LayoutParams) value.getLayoutParams();

		lparams.width = LayoutParams.FILL_PARENT;
		lparams.height = LayoutParams.WRAP_CONTENT;
		lparams.weight = 0;

		vparams.width = LayoutParams.FILL_PARENT;
		lparams.height = LayoutParams.WRAP_CONTENT;
		lparams.weight = 1;

		label.setLayoutParams(lparams);
		value.setLayoutParams(vparams);

		value.setFocusable(false); // default
		value.setMinLines(minLines);
		value.setRawInputType(multiLine ? inputType | INPUT_TYPE_TEXT_MULTILINE : inputType);
		value.setGravity(multiLine ? Gravity.TOP : Gravity.CENTER_VERTICAL);
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
		value.setTextColor(isEditing ? colorTxtEditing : colorTxtNormal);
		value.setEnabled(isEditing);
	}

	public void setAdapterItems(String[] items) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.autocomplete_item, items);
		value.setAdapter(adapter);
	}
}
