package us.artaround.android.commons;

import java.util.Timer;
import java.util.TimerTask;

import us.artaround.android.commons.TimeoutTimer.Timeout.TimeoutCallback;

// I use Timer because it uses a background thread while CountDownTimer does not.
public class TimeoutTimer extends Timer {
	private final static int TIMEOUT = 20000;
	private final Timeout task;
	private final String id;
	private final int delay;

	public TimeoutTimer(TimeoutCallback callback, String id) {
		this(callback, id, TIMEOUT);
	}

	public TimeoutTimer(TimeoutCallback callback, String id, int delay) {
		this.task = new Timeout(callback, this);
		this.id = id;
		this.delay = delay;
	}

	public String getId() {
		return id;
	}

	public void start() {
		schedule(task, delay);
	}

	public static class Timeout extends TimerTask {
		private final TimeoutCallback callback;
		private final TimeoutTimer timer;

		public Timeout(TimeoutCallback callback, TimeoutTimer timer) {
			this.callback = callback;
			this.timer = timer;
		}

		@Override
		public void run() {
			callback.onTimeout(timer);
			timer.cancel();
		}

		public static interface TimeoutCallback {
			void onTimeout(TimeoutTimer timer);
		}
	}
}