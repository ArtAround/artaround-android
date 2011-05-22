package us.artaround.android.common;

import us.artaround.models.ArtAroundException;

public class LoaderPayload {
	public static int RESULT_OK = 0;
	public static int RESULT_ERROR = 1;

	private int status = -1;
	private ArtAroundException exception;
	private Object result;

	public LoaderPayload(int status) {
		this.status = status;
	}

	public LoaderPayload(ArtAroundException exception) {
		this.status = RESULT_ERROR;
		this.exception = exception;
	}

	public LoaderPayload(Object result) {
		this.status = RESULT_OK;
		this.result = result;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoaderPayload [status=").append(status).append(", exception=").append(exception)
				.append(", result=").append(result).append("]");
		return builder.toString();
	}

}
