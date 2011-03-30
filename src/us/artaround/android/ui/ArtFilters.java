package us.artaround.android.ui;

import java.util.HashMap;
import java.util.HashSet;

import us.artaround.R;
import us.artaround.android.common.BackgroundCommand;
import us.artaround.android.common.LoadCategoriesCommand;
import us.artaround.android.common.LoadNeighborhoodsCommand;
import us.artaround.android.common.LoadingTask;
import us.artaround.android.common.NotifyingAsyncQueryHandler;
import us.artaround.android.common.Utils;
import us.artaround.android.common.LoadingTask.LoadingTaskCallback;
import us.artaround.android.common.NotifyingAsyncQueryHandler.NotifyingAsyncQueryListener;
import us.artaround.android.database.ArtAroundDatabase;
import us.artaround.android.database.ArtAroundDatabase.Artists;
import us.artaround.android.database.ArtAroundDatabase.Categories;
import us.artaround.android.database.ArtAroundDatabase.Neighborhoods;
import us.artaround.android.database.ArtAroundProvider;
import us.artaround.models.ArtAroundException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class ArtFilters extends ListActivity implements OnItemClickListener, NotifyingAsyncQueryListener,
		LoadingTaskCallback {
	private static final String TAG = "ArtAround.ArtFilters";

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

	private View loading, content;
	private EditText editText;
	private Spinner spinner;

	private int currentToken;
	private int currentInputLength;

	private LoadingTask loadCTask, loadNTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.art_filters);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		//--- enable crash reporting ---
		Utils.enableDump(this);
		// -----------------------------

		initVars();
		setupUi();
		restoreState();
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
		Holder holder = new Holder();
		holder.loadCTask = loadCTask;
		holder.loadNTask = loadNTask;
		return holder;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (loadCTask != null) loadCTask.detachCallback();
		if (loadNTask != null) loadNTask.detachCallback();
	}

	@SuppressWarnings("unchecked")
	private void initVars() {
		filters = (HashMap<Integer, HashSet<String>>) getIntent().getSerializableExtra("filters");
		if (filters.isEmpty()) {
			filters = new HashMap<Integer, HashSet<String>>();

			for (int i = FILTER_NAMES.length - 1; i >= 0; i--) {
				filters.put(i, new HashSet<String>());
			}
		}
		queryHandler = new NotifyingAsyncQueryHandler(ArtAroundProvider.contentResolver, this);
	}

	private void restoreState() {
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			loadCTask = holder.loadCTask;
			loadNTask = holder.loadNTask;

			if (loadCTask != null) loadCTask.attachCallback(this);
			if (loadNTask != null) loadNTask.attachCallback(this);
		}
	}

	private void setupState() {
		currentToken = 0;
		startDatabaseQuery(currentToken, true, null, null);
	}

	private void startDatabaseQuery(int token, boolean first, String where, String[] args) {
		Utils.d(TAG, "startDatabaseQuery() with token " + token);
		//showLoading(true);
		getCurrentUri(token);
		queryHandler.startQuery(token, first, uri, proj, where, args, null);
	}

	private void afterDatabaseQuery(int token, Object cookie, Cursor cursor) {
		Utils.d(TAG, "afterDatabaseQuery() with token " + token);

		if (cursor == null) {
			Utils.d(TAG, "---> unexpected null cursor!");
			//showLoading(false);
			return;
		}

		startManagingCursor(cursor);
		boolean hasData = cursor.moveToFirst();

		if ((Boolean) cookie == true) {
			Utils.d(TAG, "---> first activity run");
			createListAdapter(cursor);
		}
		else {
			Utils.d(TAG, "---> not first activity run");
			changeAdapterCursor(cursor);
		}

		if (!hasData) {
			Utils.d(TAG, "--> no data in the database");
			startServerLoad(token);
		}
		else {
			//showLoading(false);
		}
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		afterDatabaseQuery(token, cookie, cursor);
	}

	private void createListAdapter(Cursor cursor) {
		Utils.d(TAG, "createListAdapter()");
		setListAdapter(new CheckboxifiedCursorAdapter(this, R.layout.checkboxified_text, cursor, FROM, TO));
	}

	private void changeAdapterCursor(Cursor cursor) {
		Utils.d(TAG, "changeAdapterCursor()");
		((CheckboxifiedCursorAdapter) getListAdapter()).changeCursor(cursor);
	}

	private void startServerLoad(int token) {
		Utils.d(TAG, "startServerLoad() for token " + token);
		switch (token) {
		case FILTER_CATEGORY:
			loadCTask = (LoadingTask) new LoadingTask(this, new LoadCategoriesCommand(FILTER_CATEGORY)).execute();
			break;

		case FILTER_NEIGHBORHOOD:
			loadNTask = (LoadingTask) new LoadingTask(this, new LoadNeighborhoodsCommand(FILTER_NEIGHBORHOOD))
					.execute();
			break;
		}
	}

	private void getCurrentUri(int token) {
		switch (token) {
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
		Utils.d(TAG, "getCurrentUri() " + uri);
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
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position != currentToken) {
					currentToken = position;
					startDatabaseQuery(currentToken, false, null, null);
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
				if ((currentInputLength == 0 && s.length() >= 3) || currentInputLength > 0) {
					currentInputLength = s.length();

					String where = FROM[0] + " LIKE ?";
					String[] args = new String[] { "%" + s.toString().toLowerCase() + "%" };

					startDatabaseQuery(currentToken, false, where, args);
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

		HashSet<String> f = filters.get(currentToken);

		boolean hasFilter = f.contains(txt);
		if (hasFilter) {
			f.remove(txt);
		}
		else if (getListView().isItemChecked(position)) {
			f.add(txt);
		}

		Utils.d(TAG, "onItemClick() checked filters " + filters);
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
		Utils.d(TAG, "Filters are " + filters);
		finish();
	}

	class CheckboxifiedCursorAdapter extends SimpleCursorAdapter {
		private final LayoutInflater inflater;
		private final String[] from;
		private Cursor cursor;

		public CheckboxifiedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			this.cursor = c;
			this.from = from;
			inflater = LayoutInflater.from(context);
		}

		@Override
		public void changeCursor(Cursor cursor) {
			super.changeCursor(cursor);
			this.cursor = cursor;
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

			HashSet<String> f = filters.get(currentToken);
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

	private void showLoading(boolean isLoading) {
		loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
		content.setVisibility(isLoading ? View.GONE : View.VISIBLE);
		editText.setEnabled(!isLoading);
		spinner.setEnabled(!isLoading);
	}

	@Override
	public void beforeLoadingTask(BackgroundCommand command) {
		showLoading(true);
	}

	@Override
	public void afterLoadingTask(BackgroundCommand command, Object result) {
		finishTask(command.getToken());
	}

	@Override
	public void onLoadingTaskError(BackgroundCommand command, ArtAroundException exception) {
		finishTask(command.getToken());
	}

	private void finishTask(int token) {
		Utils.d(TAG, "finishTask() " + token);

		switch (token) {
		case FILTER_CATEGORY:
			loadCTask = null;
			break;
		case FILTER_NEIGHBORHOOD:
			loadNTask = null;
			break;
		}
		showLoading(false);
	}

	private static class Holder {
		LoadingTask loadCTask, loadNTask;
	}
}
