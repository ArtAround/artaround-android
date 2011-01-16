package us.artaround.android.ui;

import java.util.HashMap;
import java.util.HashSet;

import us.artaround.R;
import us.artaround.android.commons.NotifyingAsyncQueryHandler;
import us.artaround.android.commons.NotifyingAsyncQueryHandler.AsyncQueryListener;
import us.artaround.android.commons.Utils;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.android.services.ServiceFactory;
import us.artaround.models.ArtAroundException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class ArtFilters extends ListActivity implements OnItemClickListener, AsyncQueryListener {
	public static final String[] FILTER_NAMES = { "category", "neighborhood", "artist" };

	public static final int FILTER_CATEGORY = 0;
	public static final int FILTER_NEIGHBORHOOD = 1;
	public static final int FILTER_ARTIST = 2;

	private static final String BUNDLE_FILTERS_KEY = "filters";

	private static final String[] FROM = { "name" }; // that's generic, for all 3 filters
	private static final int[] TO = { R.id.text };

	private HashMap<Integer, HashSet<String>> filters;
	private NotifyingAsyncQueryHandler queryHandler;

	private String[] proj;
	private Uri uri;
	private int filterPos;

	private View loading, content;
	private EditText editText;
	private Spinner spinner;

	private LoadTask loadTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_filters);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		initVars();
		setupUi();
		setupState();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		filters = (HashMap<Integer, HashSet<String>>) state.getSerializable(BUNDLE_FILTERS_KEY);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(BUNDLE_FILTERS_KEY, filters);
		super.onSaveInstanceState(outState);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadTask;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (loadTask != null) loadTask.detach();
	}

	@SuppressWarnings("unchecked")
	private void initVars() {
		filters = (HashMap<Integer, HashSet<String>>) getIntent().getSerializableExtra("filters");

		if (filters == null || filters.isEmpty()) {
			filters = new HashMap<Integer, HashSet<String>>();

			for (int i = FILTER_NAMES.length - 1; i >= 0; i--) {
				filters.put(i, new HashSet<String>());
			}
		}

		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);

		loadTask = (LoadTask) getLastNonConfigurationInstance();
		if (loadTask != null) loadTask.attach(this);
	}

	private void setupState() {
		Utils.d(Utils.TAG, "setup state---");
		setFilterUri();

		showLoading(true);
		queryHandler.startQuery(FILTER_CATEGORY, true, uri, proj);
	}

	private void showLoading(boolean isLoading) {
		loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
		content.setVisibility(isLoading ? View.GONE : View.VISIBLE);
		editText.setEnabled(!isLoading);
		spinner.setEnabled(!isLoading);
	}

	private void setFilterUri() {
		switch (filterPos) {
		default:
		case FILTER_CATEGORY:
			proj = ArtAroundDatabase.CATEGORIES_PROJECTION;
			uri = Categories.CONTENT_URI;
			break;
		case FILTER_NEIGHBORHOOD:
			proj = ArtAroundDatabase.NEIGHBORHOODS_PROJECTION;
			uri = Neighborhoods.CONTENT_URI;
			break;
		case FILTER_ARTIST:
			proj = ArtAroundDatabase.ARTISTS_PROJECTION;
			uri = Artists.CONTENT_URI;
			break;
		}
		Utils.d(Utils.TAG, "Current uri=" + uri);
	}


	private void setupUi() {
		ListView listView = getListView();
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		loading = findViewById(R.id.loading);
		content = findViewById(R.id.content);

		int length = FILTER_NAMES.length;
		String by = getString(R.string.by);

		String[] typeNames = new String[length];
		for (int i = 0; i < length; i++) {
			typeNames[i] = by + " " + FILTER_NAMES[i];
		}

		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				typeNames);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setAdapter(spinnerAdapter);
		spinner.setSelection(0);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position != filterPos) {
					filterPos = position; // remember which filter was selected

					setFilterUri();

					showLoading(true);
					queryHandler.startQuery(position, uri, proj);

					editText.setText("");
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		editText = (EditText) findViewById(R.id.autocomplete);
		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

				if (s.length() > 0) {
					showLoading(true);

					String where = FROM[0] + " LIKE ?";
					String[] args = new String[] { s.toString().toLowerCase() + "%" };

					queryHandler.startQuery(filterPos, uri, proj, where, args);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		CheckBoxifiedTextView tView = (CheckBoxifiedTextView) view;
		String txt = tView.getText();

		HashSet<String> f = filters.get(filterPos);

		boolean hasFilter = f.contains(txt);
		if (hasFilter) {
			f.remove(txt);
		}
		else if (getListView().isItemChecked(position)) {
			f.add(txt);
		}

		Utils.d(Utils.TAG, "f=" + filters);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			doFinish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		doFinish();
		return;
	}

	private void doFinish() {
		Intent intent = new Intent();
		intent.putExtra("filters", filters);
		setResult(Activity.RESULT_OK, intent);
		Utils.d(Utils.TAG, "Filters are " + filters);
		finish();
	}

	class CheckboxifiedCursorAdapter extends SimpleCursorAdapter implements Filterable {

		private LayoutInflater inflater;
		private String[] from;
		private Cursor cursor;

		public CheckboxifiedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			this.cursor = c;
			this.from = from;
			inflater = LayoutInflater.from(context);
		}

		@Override
		public void changeCursor(Cursor c) {
			super.changeCursor(c);
			cursor = c;
			getListView().clearChoices();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CheckBoxifiedTextView view = (CheckBoxifiedTextView) convertView;

			if (view == null) {
				view = (CheckBoxifiedTextView) inflater.inflate(R.layout.checkboxified_text, null);
			}

			cursor.moveToPosition(position);
			String txt = cursor.getString(cursor.getColumnIndex(from[0]));

			ListView listView = getListView();
			boolean checked = listView.isItemChecked(position);

			HashSet<String> f = filters.get(filterPos);
			boolean hasFilter = f.contains(txt);


			if (hasFilter) {
				listView.setItemChecked(position, true);
				view.setCheckBoxState(true);
				view.setText(txt);
				return view;
			}
			else if (checked) {
				f.add(txt);
			}

			view.setCheckBoxState(checked);
			view.setText(txt);
			return view;
		}
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		showLoading(false);

		startManagingCursor(cursor);

		// first query
		if (cookie != null) {
			//Utils.d(Utils.TAG, "--- first query ---");
			if (cursor == null || !cursor.moveToFirst()) {
				loadFromServer(token);
				if (cursor != null) cursor.close();
				return;
			}

			setListAdapter(new CheckboxifiedCursorAdapter(this, R.layout.checkboxified_text, cursor, FROM, TO));
		}
		else {
			if (cursor == null || !cursor.moveToFirst()) {
				Utils.w(Utils.TAG, "Could not change cursor!");
				if (cursor != null) cursor.close();
				return;
			}
			CheckboxifiedCursorAdapter adapter = (CheckboxifiedCursorAdapter) getListAdapter();
			adapter.changeCursor(cursor);
			//Utils.d(Utils.TAG, "--- changed cursor ---");
		}
	}

	private void loadFromServer(int token) {
		loadTask = (LoadTask) new LoadTask(this).execute();
	}

	private static class LoadTask extends AsyncTask<Void, Void, Boolean> {
		private ArtFilters context;

		public LoadTask(ArtFilters context) {
			this.context = context;
		}

		public void attach(ArtFilters context) {
			this.context = context;
		}

		public void detach() {
			this.context = null;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				Utils.d(Utils.TAG, "Loading filters from server.");
				ServiceFactory.getArtService().getCategories();
				ServiceFactory.getArtService().getNeighborhoods();
				return true;
			}
			catch (ArtAroundException e) {
				return false;
			}
		}

		@Override
		protected void onPreExecute() {
			context.showLoading(true);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			context.showLoading(false);
			context.loadTask = null;

			if (result) {
				context.setupState();
			}
		}
	}
}
