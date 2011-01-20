package us.artaround.android.commons;

import us.artaround.models.ArtAroundException;

public abstract class ServerCallCommand {
	protected int token;

	public ServerCallCommand() {}

	public ServerCallCommand(int token) {
		this.token = token;
	}

	public int getToken() {
		return token;
	}

	public abstract <Result> Result execute() throws ArtAroundException;
}
