package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;

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
import android.text.TextUtils;
import android.text.TextWatcher;
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

	private static final String SAVE_FILTERS = "filters";
	private static final String SAVE_FILTER_INDEX = "index";
	private static final String QUERY = "query";

	private HashMap<Integer, ArrayList<String>> filters;
	private ArrayList<String> categories;
	private int filterIndex;
	private int currentInputLength;
	private String allPublic;
	private String allVenues;

	//FIXME fetch data from server if it's not in the database

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(SAVE_FILTERS, filters);
		outState.putInt(SAVE_FILTER_INDEX, filterIndex);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle args) {
		return inflater.inflate(R.layout.art_filter_fragment, parent, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);

		InstantAutoComplete tvType = null;

		if (savedInstanceState != null) {
			filters = (HashMap<Integer, ArrayList<String>>) savedInstanceState.getSerializable(SAVE_FILTERS);
			filterIndex = savedInstanceState.getInt(SAVE_FILTER_INDEX, 0);

			tvType = (InstantAutoComplete) getActivity().findViewById(R.id.filter_type);
			tvType.setText(ArtFilter.FILTER_TYPE_NAMES[filterIndex]);
		}
		if (filters == null) {
			filters = new HashMap<Integer, ArrayList<String>>();
			for (int i = 0; i < ArtFilter.FILTER_TYPE_NAMES.length; i++) {
				filters.put(i, new ArrayList<String>());
			}
		}
		categories = new ArrayList<String>();
		allPublic = getString(R.string.all_public);
		allVenues = getString(R.string.all_venues);

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

		if (tvType == null) {
			tvType = (InstantAutoComplete) getActivity().findViewById(R.id.filter_type);
		}
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
				selection = (selectionArgs == null) ? null : Neighborhoods.NAME + selection;
				return new CursorLoader(getActivity(), Neighborhoods.CONTENT_URI, new String[] { Neighborhoods._ID,
						Neighborhoods.NAME }, selection, selectionArgs, null);

			case ArtFilter.TYPE_TITLE:
				selection = (selectionArgs == null) ? null : Arts.TITLE + selection;
				return new CursorLoader(getActivity(), Arts.CONTENT_URI, new String[] { Arts._ID, Arts.TITLE },
						selection, selectionArgs, null);

			case ArtFilter.TYPE_ARTIST:
				selection = (selectionArgs == null) ? null : Artists.NAME + selection;
				return new CursorLoader(getActivity(), Artists.CONTENT_URI, new String[] { Artists._ID, Artists.NAME },
						selection, selectionArgs, null);
			default:
				return null;
			}
		}

		@Override
		public void onLoadFinished(final Loader<Cursor> loader, Cursor cursor) {
			ListAdapter adapter = getListAdapter();
			if (adapter instanceof MySimpleCursorAdapter) {
				((SimpleCursorAdapter) adapter).swapCursor(cursor);
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

			if (text.equals(getString(R.string.all_venues))) {
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

		//Utils.showToast(getActivity(), filters.toString());
	}

	private void selectAllCategories(int start, int end, boolean isChecked, ListView listView, MyArrayAdapter adapter) {
		for (int i = start; i < end; i++) {
			listView.setItemChecked(i, isChecked);

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
			getListView().clearChoices();
			return super.swapCursor(cursor);
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
			check.setChecked(checked);
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
				//				if (text.equals(allPublic) || text.equals(allVenues))
				//					check = (CheckBox) LayoutInflater.from(getActivity()).inflate(R.layout.art_filter_item, null);
				//				else
				check = (CheckBox) LayoutInflater.from(getActivity()).inflate(R.layout.art_filter_item, null);
			}

			check.setText(text);
			check.setTag(text);

			boolean checked = filters.get(filterIndex).contains(text);
			check.setChecked(checked);

			getListView().setItemChecked(position, checked);

			return check;
		}
	}

	public HashMap<Integer, ArrayList<String>> getFilters() {
		return filters;
	}

}
