package us.artaround.models;

public class ArtAroundException extends Exception {
	private static final long serialVersionUID = 6157902756731129293L;

	private String message;
	private Throwable cause;
	
	public ArtAroundException(Throwable cause) {
		super();
		this.cause = cause;
	}

	public ArtAroundException(String message) {
		this(message, null);
	}

	public ArtAroundException(String message, Throwable cause) {
		this.message = message;
		this.cause = cause;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public Throwable getCause() {
		return cause;
	}

	public static class NotFound extends ArtAroundException {
		private static final long serialVersionUID = -190352704112514681L;

		public NotFound(String message) {
			super(message);
		}
	}
}
