package us.artaround.android.parsers;

public class StreamData {
	private int parserType;
	private boolean isCollection;
	private String httpData;
	private Object[] auxData;

	public StreamData(int parserType) {
		super();
		this.parserType = parserType;
	}

	public StreamData(int parserType, boolean isCollection, String httpData, Object... auxData) {
		super();
		this.parserType = parserType;
		this.isCollection = isCollection;
		this.httpData = httpData;
		this.auxData = auxData;
	}

	public StreamData(int parserType, String httpData, Object[] auxData) {
		this(parserType, false, httpData, auxData);
	}

	public int getParserType() {
		return parserType;
	}

	public void setParserType(int parserType) {
		this.parserType = parserType;
	}

	public String getHttpData() {
		return httpData;
	}

	public void setHttpData(String httpData) {
		this.httpData = httpData;
	}

	public Object[] getAuxData() {
		return auxData;
	}

	public void setAuxData(Object... auxData) {
		this.auxData = auxData;
	}

	public boolean isCollection() {
		return isCollection;
	}

	public void setCollection(boolean isCollection) {
		this.isCollection = isCollection;
	}
}
