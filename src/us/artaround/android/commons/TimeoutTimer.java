package us.artaround.android.commons;

import java.util.Timer;
import java.util.TimerTask;

import us.artaround.android.commons.TimeoutTimer.Timeout.TimeoutCallback;
import android.os.Handler;

// Timer because it uses a background thread while CountDownTimer does not.
public class TimeoutTimer extends Timer
{
	public static final String TAG = "TIMEOUT_TIMER";

	private final static int TIMEOUT = 20000;
	private final Timeout task;
	private final String id;
	private Object tag;
	private boolean isCancelled;

	private int delay = TIMEOUT;

	public TimeoutTimer(TimeoutCallback callback, Handler handler, String id)
	{
		this(callback, handler, id, TIMEOUT);
	}

	public TimeoutTimer(TimeoutCallback callback, Handler handler, String id, int delay)
	{
		this.id = id;
		this.delay = delay;
		this.task = new Timeout(callback, handler, this);
	}

	public String getId() {
		return id;
	}
	
	public Object getTag()
	{
		return tag;
	}
	
	public void setTag(Object tag)
	{
		this.tag = tag;
	}

	public void start()
	{
		try
		{
			if (!isCancelled)
			{
				schedule(task, delay);
				Utils.d(TAG, "Scheduled task " + task + " on timer " + this);
			}
		}
		catch (Exception e)
		{
			Utils.d(TAG, "Could not schedule timer " + e);
		}
	}
	
	public void stop()
	{
		this.cancel();
		isCancelled = true;
		boolean ok = task.cancel();
		Utils.d(TAG, "Canceled task " + task + " on timer " + this + " canceled=" + ok);
	}

	public static class Timeout extends TimerTask
	{
		private final TimeoutCallback callback;
		private final TimeoutTimer timer;
		private final Handler handler;
		
		public Timeout(TimeoutCallback callback, Handler handler, TimeoutTimer timer)
		{
			this.callback = callback;
			this.timer = timer;
			this.handler = handler;
		}
		
		final Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				callback.onTimeout(timer);
			}
		};

		@Override
		public void run()
		{
			Utils.d(TAG, "Task " + this + " on timer " + timer + " cancel status is " + timer.isCancelled);
			if (!timer.isCancelled)
			{
				boolean ok = handler.post(runnable);
				Utils.d(TAG, "Posted timeout to main thread " + ok);
			}
		}
		
		public static interface TimeoutCallback
		{
			void onTimeout(TimeoutTimer timer);
		}
	}
}
