package us.artaround.android.ui;

public class CheckBoxifiedText implements Comparable<CheckBoxifiedText> {

	private String text;
	private boolean checked;

	public CheckBoxifiedText(String text, boolean checked) {
		this.text = text;
		this.checked = checked;
	}

	public CheckBoxifiedText(String text) {
		this(text, false);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	@Override
	public int compareTo(CheckBoxifiedText txt) {
		if (null != text) {
			return text.compareTo(txt.getText());
		}
		else
			throw new IllegalArgumentException();
	}

}
