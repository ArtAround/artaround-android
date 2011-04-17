package us.artaround.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import us.artaround.R;
import us.artaround.android.common.Utils;
import us.artaround.android.common.task.ArtAroundAsyncCommand;
import us.artaround.android.common.task.ArtAroundAsyncTask;
import us.artaround.android.common.task.ArtAroundAsyncTask.ArtAroundAsyncTaskListener;
import us.artaround.models.ArtAroundException;
import android.os.Bundle;
import android.view.View;

import com.google.android.maps.MapActivity;

public abstract class ArtAroundMapActivity extends MapActivity implements ArtAroundAsyncTaskListener {
	protected static final String TAG = "ArtAround.MapActivity";

	protected static class SavedState {
		List<ArtAroundAsyncTask> tasks;
	}

	protected List<ArtAroundAsyncTask> tasks;
	protected SavedState savedState;

	protected View actionbar;

	protected abstract void onChildCreate(Bundle savedInstanceState);
	protected abstract void onChildEndCreate(Bundle savedInstanceState);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Utils.onSetTheme(this);
		onChildCreate(savedInstanceState);

		initActionbarUi();

		savedState = (SavedState) getLastNonConfigurationInstance();
		if (savedState != null) {
			tasks = savedState.tasks;
			if (tasks != null) {
				for (ArtAroundAsyncTask task : tasks) {
					task.changeListener(this);
				}
			}
		}

		onChildEndCreate(savedInstanceState);
	}

	protected void initActionbarUi() {
		actionbar = findViewById(android.R.id.content).getRootView().findViewById(R.id.actionbar);
		if (actionbar == null) {
			Utils.d(TAG, "Could not find the actionbar view in your activity layout!");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (tasks != null) {
			for (ArtAroundAsyncTask task : tasks) {
				task.clearListener();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (savedState == null) {
			savedState = new SavedState();
		}
		savedState.tasks = tasks;
		return savedState;
	}

	@Override
	public void onPreExecute(ArtAroundAsyncCommand command) {}

	@Override
	public void onPostExecute(ArtAroundAsyncCommand command, Object result, ArtAroundException exception) {
		if (tasks == null) {
			Utils.d(TAG, "onPostExecute(): tasks is null.");
		}
		for (ArtAroundAsyncTask task : tasks) {
			String commandId = task.getCommandId();
			if (command.id.equals(commandId)) {
				tasks.remove(task);
				Utils.d(TAG, "onPostExecute(): removed task '" + commandId + "' after completion.");
				break;
			}
		}
	}

	@Override
	public void onPublishProgress(ArtAroundAsyncCommand command, Object progress) {}

	protected void startTask(ArtAroundAsyncCommand command) {
		if (tasks == null) {
			tasks = Collections.synchronizedList(new ArrayList<ArtAroundAsyncTask>());
		}
		for (ArtAroundAsyncTask task : tasks) {
			String commandId = task.getCommandId();
			if (command.id.equals(commandId)) {
				Utils.d(TAG, "startTask(): task '" + commandId + "' is already running.");
				break;
			}
		}
		ArtAroundAsyncTask newTask = new ArtAroundAsyncTask(command, this);
		tasks.add(newTask);
		newTask.execute();
	}

	protected boolean hasRunningTasks(int token) {
		if (tasks == null || tasks.isEmpty()) return false;

		for (ArtAroundAsyncTask task : tasks) {
			Utils.d(Utils.TAG, "token=" + task.getTokenId());
			if (task.getTokenId() == token) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
