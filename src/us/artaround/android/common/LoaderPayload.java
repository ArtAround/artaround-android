package us.artaround.android.common;

import us.artaround.models.ArtAroundException;
import android.os.Bundle;

public class LoaderPayload {
	public static int STATUS_OK = 0;
	public static int STATUS_ERROR = 1;

	private int status = -1;
	private ArtAroundException exception;
	private Object result;
	private Bundle args;

	public LoaderPayload(int status) {
		this.status = status;
	}

	public LoaderPayload(ArtAroundException exception) {
		this(exception, null);
	}

	public LoaderPayload(ArtAroundException exception, Bundle args) {
		this.status = STATUS_ERROR;
		this.exception = exception;
		this.args = args;
	}

	public LoaderPayload(int status, Object result) {
		this(status, result, null);
	}

	public LoaderPayload(int status, Object result, Bundle args) {
		this.status = status;
		this.result = result;
		this.args = args;
	}

	public int getStatus() {
		return status;
	}

	public ArtAroundException getException() {
		return exception;
	}

	public Object getResult() {
		return result;
	}

	public Bundle getArgs() {
		return args;
	}

	public void setArgs(Bundle args) {
		this.args = args;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoaderPayload [status=").append(status).append(", exception=").append(exception)
				.append(", result=").append(result).append("]");
		return builder.toString();
	}

}
