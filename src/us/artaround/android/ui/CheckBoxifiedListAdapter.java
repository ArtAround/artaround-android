package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.List;

import us.artaround.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

public class CheckBoxifiedListAdapter extends ArrayAdapter<CheckBoxifiedText> {

	private Context context;
	private List<CheckBoxifiedText> items;

	public CheckBoxifiedListAdapter(Context context) {
		super(context, R.layout.checkboxified_text);
		this.context = context;
		items = new ArrayList<CheckBoxifiedText>();
	}

	public void setChecked(boolean value, int position) {
		items.get(position).setChecked(value);
		notifyDataSetChanged();
	}

	public void setListItems(List<CheckBoxifiedText> list) {
		items = list;
		notifyDataSetChanged();
	}

	public void selectAll() {
		for (CheckBoxifiedText cboxtxt : items)
			cboxtxt.setChecked(true);

		notifyDataSetChanged();
	}

	public void deselectAll() {
		for (CheckBoxifiedText cboxtxt : items)
			cboxtxt.setChecked(false);

		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public CheckBoxifiedText getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheckBoxifiedTextView cbtv = (CheckBoxifiedTextView) convertView;
		CheckBoxifiedText model = items.get(position);
		CheckBox checkBox;

		if (cbtv == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(
		      Context.LAYOUT_INFLATER_SERVICE);

			cbtv = (CheckBoxifiedTextView) inflater.inflate(R.layout.checkboxified_text, parent, false);
		}
		else {
			cbtv = (CheckBoxifiedTextView) convertView;
		}

		cbtv.getTextView().setText(model.getText());

		checkBox = cbtv.getCheckBox();
		checkBox.setChecked(model.isChecked());
		checkBox.setTag(position);
		checkBox.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doCheck((CheckBox) v);
			}
		});

		return cbtv;
	}

	private void doCheck(CheckBox checkBox) {
		int position = (Integer) checkBox.getTag();
		CheckBoxifiedText model = getItem(position);
		model.setChecked(!model.isChecked());
		checkBox.setChecked(model.isChecked());
	}

}
