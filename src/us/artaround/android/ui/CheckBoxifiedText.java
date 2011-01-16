package us.artaround.android.ui;

import java.io.Serializable;

public class CheckBoxifiedText implements Serializable {
	private static final long serialVersionUID = 1088474292127162855L;
	private String text;
	private boolean checked;

	public CheckBoxifiedText(String text, boolean checked) {
		this.text = text;
		this.checked = checked;
	}

	public CheckBoxifiedText(String text) {
		this(text, false);
	}

	public CheckBoxifiedText() {}

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
	public String toString() {
		return "[text=" + text + ", checked=" + checked + "]";
	}
}
