package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import us.artaround.R;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ArtFilterListFragment extends ListFragment {
	private static final String QUERY = "query";

	private HashMap<Integer, HashSet<String>> filters;
	private ArrayList<String> categories;
	private int filterIndex;
	private int currentInputLength;
	private String allPublic;
	private String allVenues;

	//FIXME fetch data from server if it's not in the database



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle args) {
		return inflater.inflate(R.layout.art_filter_fragment, parent, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);

		if (filters == null) {
			filters = new HashMap<Integer, HashSet<String>>();
			for (int i = 0; i < ArtFilter.FILTER_TYPE_NAMES.length; i++) {
				filters.put(i, new HashSet<String>());
			}
		}
		categories = new ArrayList<String>();
		allPublic = getString(R.string.art_filter_group_public);
		allVenues = getString(R.string.art_filter_group_venues);

		final EditText tvSearch = (EditText) getActivity().findViewById(R.id.filter_search);
		tvSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ((currentInputLength == 0 && s.length() >= 3) || currentInputLength > 0) {
					currentInputLength = s.length();

					Bundle args = new Bundle();
					args.putString(QUERY, s.toString().toLowerCase());

					getLoaderManager().restartLoader(filterIndex, args, cursorCallbacks);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		});

		InstantAutoComplete tvType = (InstantAutoComplete) getActivity().findViewById(R.id.filter_type);
		tvType.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (filterIndex == position) return;
				filterIndex = position;

				setListAdapter(createAdapter(position));
				getLoaderManager().restartLoader(position, null, cursorCallbacks);

				tvSearch.setVisibility(filterIndex != ArtFilter.TYPE_CATEGORY ? View.VISIBLE : View.GONE);
				tvSearch.getText().clear();
			}
		});

		setListAdapter(createAdapter(filterIndex));
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		getLoaderManager().restartLoader(filterIndex, null, cursorCallbacks);
	}

	private ListAdapter createAdapter(final int position) {
		SimpleCursorAdapter adapter = null;

		switch (position) {
		case ArtFilter.TYPE_CATEGORY:
			return new MyArrayAdapter(getActivity(), R.layout.art_filter_item, categories);

		case ArtFilter.TYPE_NEIGHBORHOOD:
			adapter = new MySimpleCursorAdapter(new String[] { Neighborhoods._ID, Neighborhoods.NAME },
					new int[] { R.id.checkbox });
			break;

		case ArtFilter.TYPE_TITLE:
			adapter = new MySimpleCursorAdapter(new String[] { Arts._ID, Arts.TITLE }, new int[] { R.id.checkbox });
			break;

		case ArtFilter.TYPE_ARTIST:
			adapter = new MySimpleCursorAdapter(new String[] { Artists._ID, Artists.NAME }, new int[] { R.id.checkbox });
			break;
		}
		return adapter;
	}

	private final LoaderManager.LoaderCallbacks<Cursor> cursorCallbacks = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			String selection = null;
			String[] selectionArgs = null;

			if (args != null) {
				String query = args.getString(QUERY);
				if (!TextUtils.isEmpty(query)) {
					selection = " LIKE ?";
					selectionArgs = new String[] { "%" + query.toLowerCase() + "%" };
				}
			}

			switch (id) {
			case ArtFilter.TYPE_CATEGORY:
				selection = (selectionArgs == null) ? null : Categories.NAME + selection;
				StringBuilder s = new StringBuilder();
				if (selection != null) {
					s.append(selection);
					s.append(" AND ");
				}
				s.append(Categories.NAME).append(" NOT IN (");
				s.append("'").append(getString(R.string.category_gallery)).append("', ");
				s.append("'").append(getString(R.string.category_market)).append("', ");
				s.append("'").append(getString(R.string.category_museum)).append("')");
				selection = s.toString();

				return new CursorLoader(getActivity(), Categories.CONTENT_URI, new String[] { Categories._ID,
						Categories.NAME }, selection, selectionArgs, null);

			case ArtFilter.TYPE_NEIGHBORHOOD:
				StringBuilder b = new StringBuilder(Neighborhoods.NAME).append(" NOT NULL AND ")
						.append(Neighborhoods.NAME).append("<> ''");
				if (selectionArgs != null) {
					b.append(" AND ").append(Neighborhoods.NAME).append(selection);
				}
				return new CursorLoader(getActivity(), Neighborhoods.CONTENT_URI, new String[] { Neighborhoods._ID,
						Neighborhoods.NAME }, b.toString(), selectionArgs, null);

			case ArtFilter.TYPE_TITLE:
				selection = (selectionArgs == null) ? null : Arts.TITLE + selection;
				return new CursorLoader(getActivity(), Arts.CONTENT_URI, new String[] { Arts._ID, Arts.TITLE },
						selection, selectionArgs, "lower(" + Arts.TITLE + ")");

			case ArtFilter.TYPE_ARTIST:
				b = new StringBuilder(Artists.NAME).append(" NOT NULL AND ").append(Artists.NAME)
						.append("<> ''");
				if (selectionArgs != null) {
					b.append(" AND ").append(Artists.NAME).append(selection);
				}
				return new CursorLoader(getActivity(), Artists.CONTENT_URI, new String[] { Artists._ID, Artists.NAME },
						b.toString(), selectionArgs, null);
			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(final Loader<Cursor> loader, Cursor cursor) {
			ListAdapter adapter = getListAdapter();
			if (adapter instanceof MySimpleCursorAdapter) {
				getListView().clearChoices();
				((MySimpleCursorAdapter) adapter).swapCursor(cursor);
			}
			else if (adapter instanceof MyArrayAdapter) {

				if (cursor != null && cursor.moveToFirst()) {
					categories.clear();
					categories.add(allVenues);
					categories.add(getString(R.string.category_gallery));
					categories.add(getString(R.string.category_market));
					categories.add(getString(R.string.category_museum));
					categories.add(allPublic);

					int count = cursor.getCount();
					for (int i = 0; i < count; i++) {
						categories.add(cursor.getString(cursor.getColumnIndex(Categories.NAME)));
						cursor.moveToNext();
					}
				}
				MyArrayAdapter adapter1 = (MyArrayAdapter) adapter;
				adapter1.notifyDataSetChanged();
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			//((MySimpleCursorAdapter) getListAdapter()).swapCursor(null);
		}
	};

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		CheckBox check = (CheckBox) view;

		if (listView.getAdapter() instanceof MyArrayAdapter) {
			MyArrayAdapter adapter1 = (MyArrayAdapter) listView.getAdapter();
			Object item = adapter1.getItem(position);
			String text = null;

			if (item instanceof CursorWrapper) {
				CursorWrapper item1 = (CursorWrapper) item;
				text = item1.getString(item1.getColumnIndex(Categories.NAME));
			}
			else {
				text = (String) item;
			}

			if (text.equals(allVenues)) {
				MyArrayAdapter adapter = (MyArrayAdapter) listView.getAdapter();
				selectAllCategories(0, 4, check.isChecked(), listView, adapter);

			}
			else if (text.equals(allPublic)) {
				MyArrayAdapter adapter = (MyArrayAdapter) listView.getAdapter();
				selectAllCategories(4, adapter.getCount(), check.isChecked(), listView, adapter);
			}
			else {
				if (check.isChecked()) {
					filters.get(filterIndex).remove(text);
				}
				else {
					filters.get(filterIndex).add(text);
				}
			}
		}
		else {

			if (check.isChecked()) {
				filters.get(filterIndex).remove(check.getTag());
			}
			else {
				filters.get(filterIndex).add((String) check.getTag());
			}
		}
	}

	private void selectAllCategories(int start, int end, boolean isChecked, ListView listView, MyArrayAdapter adapter) {
		for (int i = start; i < end; i++) {

			if (isChecked) {
				filters.get(filterIndex).remove(adapter.getItem(i));
			}
			else {
				filters.get(filterIndex).add(adapter.getItem(i));
			}
			adapter.notifyDataSetChanged();
		}
	}

	private class MySimpleCursorAdapter extends SimpleCursorAdapter {
		protected final String colName;
		protected Cursor cursor;

		public MySimpleCursorAdapter(String[] from, int[] to) {
			super(getActivity(), R.layout.art_filter_item, null, from, to, 0);
			colName = from[1];
		}

		@Override
		public Cursor swapCursor(Cursor cursor) {
			this.cursor = cursor;
			return super.swapCursor(this.cursor);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CheckBox check = (CheckBox) convertView;

			if (check == null) {
				check = (CheckBox) LayoutInflater.from(getActivity()).inflate(R.layout.art_filter_item, null);
			}

			cursor.moveToPosition(position);
			String text = cursor.getString(cursor.getColumnIndex(colName));
			check.setText(text);
			check.setTag(text);

			boolean checked = filters.get(filterIndex).contains(text);
			getListView().setItemChecked(position, checked);
			return check;
		}

	}

	private class MyArrayAdapter extends ArrayAdapter<String> {

		public MyArrayAdapter(FragmentActivity context, int resId, ArrayList<String> items) {
			super(context, resId, resId, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String text = getItem(position);
			CheckBox check = (CheckBox) convertView;
			if (check == null) {
				check = (CheckBox) LayoutInflater.from(getActivity()).inflate(R.layout.art_filter_item, null);
			}

			if (text.equals(allPublic) || text.equals(allVenues)) {
				SpannableString str = SpannableString.valueOf(text);
				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), 0);
				str.setSpan(
						new ForegroundColorSpan(getActivity().getResources().getColor(R.color.ArtFilterHeadingText)),
						0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				check.setText(str);
			}
			else {
				check.setText(text);
			}
			check.setTag(text);

			boolean checked = filters.get(filterIndex).contains(text);
			getListView().setItemChecked(position, checked);
			return check;
		}
	}

	public HashMap<Integer, HashSet<String>> getFilters() {
		return filters;
	}

}
