package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import us.artaround.R;
import us.artaround.android.common.Utils;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Arts;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ArtFilterListFragment extends ListFragment {

	private static final String SAVE_FILTERS = "filters";
	private static final String SAVE_FILTER_INDEX = "index";
	private static final String QUERY = "query";

	private String[] allVenues;

	private HashMap<Integer, ArrayList<String>> filters;
	private int filterIndex;
	private int currentInputLength;

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

		allVenues = new String[] { getString(R.string.category_gallery), getString(R.string.category_museum),
				getString(R.string.category_market) };

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

		final EditText tvSearch = (EditText) getActivity().findViewById(R.id.filter_search);
		tvSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ((currentInputLength == 0 && s.length() >= 3) || currentInputLength > 0) {
					currentInputLength = s.length();

					Bundle args = new Bundle();
					args.putString(QUERY, s.toString().toLowerCase());

					LoaderManager lm = getLoaderManager();
					if (lm.getLoader(filterIndex) == null) {
						lm.initLoader(filterIndex, args, cursorCallbacks);
					}
					else {
						lm.restartLoader(filterIndex, args, cursorCallbacks);
					}
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
				LoaderManager lm = getLoaderManager();
				if (lm.getLoader(position) == null) {
					lm.initLoader(position, null, cursorCallbacks);
				}
				else {
					lm.restartLoader(position, null, cursorCallbacks);
				}

				tvSearch.setVisibility(filterIndex != ArtFilter.TYPE_CATEGORY ? View.VISIBLE : View.GONE);
				tvSearch.getText().clear();
			}
		});

		setListAdapter(createAdapter(filterIndex));
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		getLoaderManager().initLoader(filterIndex, null, cursorCallbacks);
	}

	private ListAdapter createAdapter(final int position) {
		SimpleCursorAdapter adapter = null;

		switch (position) {
		case ArtFilter.TYPE_CATEGORY:
			// create our list and custom adapter  
			MyAdapterWithHeaders adapter1 = new MyAdapterWithHeaders();
			adapter1.addSection(getString(R.string.all_venues), new ArrayAdapter<String>(getActivity(),
					R.layout.art_filter_item, allVenues));

			adapter1.addSection(getString(R.string.all_public), new MySimpleCursorAdapter(new String[] {
					Categories._ID, Categories.NAME }, new int[] { R.id.checkbox }));
			return adapter1;

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
				boolean first = true;
				for (int i = 0; i < allVenues.length; i++) {
					if (first)
						first = false;
					else
						s.append(", ");
					s.append("'").append(allVenues[i]).append("'");
				}
				s.append(")");
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
			else if (adapter instanceof MyAdapterWithHeaders) {
				MyAdapterWithHeaders adapter1 = (MyAdapterWithHeaders) adapter;
				for (Object section : adapter1.sections.keySet()) {
					Adapter a = adapter1.sections.get(section);
					if (a instanceof MySimpleCursorAdapter) {
						((MySimpleCursorAdapter) a).swapCursor(cursor);
						adapter1.notifyDataSetChanged();
					}
				}
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

		if (listView.getAdapter() instanceof MyAdapterWithHeaders) {
			MyAdapterWithHeaders adapter1 = (MyAdapterWithHeaders) listView.getAdapter();
			Object item = adapter1.getItem(position);
			String text = null;

			if (item instanceof CursorWrapper) {
				CursorWrapper item1 = (CursorWrapper) item;
				text = item1.getString(item1.getColumnIndex(Categories.NAME));
			}
			else {
				text = (String) item;
			}

			if (check.isChecked()) {
				filters.get(filterIndex).remove(text);
			}
			else {
				filters.get(filterIndex).add(text);
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

		Utils.showToast(getActivity(), filters.toString());
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

			if (filterIndex == 0) {
				getListView().setItemChecked(position + allVenues.length + 2, checked); // there are 2 headers
			}
			else {
				getListView().setItemChecked(position, checked);
			}

			return check;
		}

	}

	private class MyAdapterWithHeaders extends BaseAdapter {
		public final Map<String, Adapter> sections = new LinkedHashMap<String, Adapter>();
		public final ArrayAdapter<String> headers;
		public final static int TYPE_SECTION_HEADER = 0;

		public MyAdapterWithHeaders() {
			headers = new ArrayAdapter<String>(getActivity(), R.layout.art_filter_item_header);
		}

		public void addSection(String section, Adapter adapter) {
			this.headers.add(section);
			this.sections.put(section, adapter);
		}

		@Override
		public Object getItem(int position) {
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section  
				if (position == 0) return section;
				if (position < size) return adapter.getItem(position - 1);

				// otherwise jump into next section  
				position -= size;
			}
			return null;
		}

		@Override
		public int getCount() {
			// total together all sections, plus one for each section header  
			int total = 0;
			for (Adapter adapter : this.sections.values())
				total += adapter.getCount() + 1;
			return total;
		}

		@Override
		public int getViewTypeCount() {
			// assume that headers count as one, then total all sections  
			int total = 1;
			for (Adapter adapter : this.sections.values())
				total += adapter.getViewTypeCount();
			return total;
		}

		@Override
		public int getItemViewType(int position) {
			int type = 1;
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section  
				if (position == 0) return TYPE_SECTION_HEADER;
				if (position < size) return type + adapter.getItemViewType(position - 1);

				// otherwise jump into next section  
				position -= size;
				type += adapter.getViewTypeCount();
			}
			return -1;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return (getItemViewType(position) != TYPE_SECTION_HEADER);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int sectionIndex = 0;
			for (Object section : this.sections.keySet()) {
				Adapter adapter = sections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section  
				if (position == 0) return headers.getView(sectionIndex, convertView, parent);
				if (position < size) return adapter.getView(position - 1, convertView, parent);

				// otherwise jump into next section  
				position -= size;
				sectionIndex++;
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	public HashMap<Integer, ArrayList<String>> getFilters() {
		return filters;
	}

}
