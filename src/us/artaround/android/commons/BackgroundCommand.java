package us.artaround.android.commons;

import us.artaround.models.ArtAroundException;

public abstract class BackgroundCommand {
	protected final int token;
	protected String id;

	public BackgroundCommand(int token) {
		this(token, String.valueOf(token));
	}

	public BackgroundCommand(int token, String id) {
		this.token = token;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getToken() {
		return token;
	}

	public abstract <Result> Result execute() throws ArtAroundException;
}
