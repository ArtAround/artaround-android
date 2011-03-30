package us.artaround.android.common.task;

import us.artaround.models.ArtAroundException;


public abstract class ArtAroundAsyncCommand {
    public final int token;
    public final String id;

    public ArtAroundAsyncCommand(int token, String id) {
        super();
        this.token = token;
        this.id = id;
    }

    public abstract Object execute() throws ArtAroundException;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ArtAroundAsyncCommand [mToken=").append(token).append(", mId=")
               .append(id).append("]");
        return builder.toString();
    }

}
