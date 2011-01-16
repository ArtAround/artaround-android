package us.artaround.android.commons.navigation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RoadProvider {

	public static Road getRoad(InputStream is) {
		KMLHandler handler = new KMLHandler();
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(is, handler);
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return handler.routeRoad;
	}

	public static String getUrl(float fromLat, float fromLon, float toLat, float toLon) {// connect to map web service
		StringBuffer urlString = new StringBuffer();
		urlString.append("http://maps.google.com/maps?f=d&hl=en");
		urlString.append("&saddr=");// from
		urlString.append(Float.toString(fromLat));
		urlString.append(",");
		urlString.append(Float.toString(fromLon));
		urlString.append("&daddr=");// to
		urlString.append(Float.toString(toLat));
		urlString.append(",");
		urlString.append(Float.toString(toLon));
		urlString.append("&ie=UTF8&0&om=0&output=kml");
		return urlString.toString();
	}
}

class KMLHandler extends DefaultHandler {
	Road routeRoad;
	boolean isPlacemark;
	boolean isRoute;
	boolean isItemIcon;
	private final Stack<String> currElem = new Stack<String>();
	private String text;

	public KMLHandler() {
		routeRoad = new Road();
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		currElem.push(localName);
		if (localName.equalsIgnoreCase("Placemark")) {
			isPlacemark = true;
			routeRoad.points = addPoint(routeRoad.points);
		}
		else if (localName.equalsIgnoreCase("ItemIcon")) {
			if (isPlacemark) isItemIcon = true;
		}
		text = new String();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String chars = new String(ch, start, length).trim();
		text = text.concat(chars);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if (text.length() > 0) {
			if (localName.equalsIgnoreCase("name")) {
				if (isPlacemark) {
					isRoute = text.equalsIgnoreCase("Route");
					if (!isRoute) {
						routeRoad.points[routeRoad.points.length - 1].name = text;
					}
				}
				else {
					routeRoad.name = text;
				}
			}
			else if (localName.equalsIgnoreCase("color") && !isPlacemark) {
				routeRoad.color = Integer.parseInt(text, 16);
			}
			else if (localName.equalsIgnoreCase("width") && !isPlacemark) {
				routeRoad.width = Integer.parseInt(text);
			}
			else if (localName.equalsIgnoreCase("description")) {
				if (isPlacemark) {
					String description = cleanup(text);
					if (!isRoute)
						routeRoad.points[routeRoad.points.length - 1].description = description;
					else
						routeRoad.description = description;
				}
			}
			else if (localName.equalsIgnoreCase("href")) {
				if (isItemIcon) {
					routeRoad.points[routeRoad.points.length - 1].iconUrl = text;
				}
			}
			else if (localName.equalsIgnoreCase("coordinates")) {
				if (isPlacemark) {
					if (!isRoute) {
						String[] xyParsed = split(text, ",");
						float lon = Float.parseFloat(xyParsed[0]);
						float lat = Float.parseFloat(xyParsed[1]);
						routeRoad.points[routeRoad.points.length - 1].latitude = lat;
						routeRoad.points[routeRoad.points.length - 1].longitude = lon;
					}
					else {
						String[] coodrinatesParsed = split(text, " ");
						routeRoad.route = new float[coodrinatesParsed.length][2];
						for (int i = 0; i < coodrinatesParsed.length; i++) {
							String[] xyParsed = split(coodrinatesParsed[i], ",");
							for (int j = 0; j < 2 && j < xyParsed.length; j++)
								routeRoad.route[i][j] = Float.parseFloat(xyParsed[j]);
						}
					}
				}
			}
		}
		currElem.pop();
		if (localName.equalsIgnoreCase("Placemark")) {
			isPlacemark = false;
			if (isRoute) isRoute = false;
		}
		else if (localName.equalsIgnoreCase("ItemIcon")) {
			if (isItemIcon) isItemIcon = false;
		}
	}

	private String cleanup(String value) {
		String remove = "<br/>";
		int index = value.indexOf(remove);
		if (index != -1) value = value.substring(0, index);
		remove = "&#160;";
		index = value.indexOf(remove);
		int len = remove.length();
		while (index != -1) {
			value = value.substring(0, index).concat(value.substring(index + len, value.length()));
			index = value.indexOf(remove);
		}
		return value;
	}

	public Point[] addPoint(Point[] points) {
		Point[] result = new Point[points.length + 1];
		for (int i = 0; i < points.length; i++)
			result[i] = points[i];
		result[points.length] = new Point();
		return result;
	}

	private static String[] split(String strString, String strDelimiter) {
		String[] strArray;
		int iOccurrences = 0;
		int iIndexOfInnerString = 0;
		int iIndexOfDelimiter = 0;
		int iCounter = 0;
		if (strString == null) {
			throw new IllegalArgumentException("Input string cannot be null.");
		}
		if (strDelimiter.length() <= 0 || strDelimiter == null) {
			throw new IllegalArgumentException("Delimeter cannot be null or empty.");
		}
		if (strString.startsWith(strDelimiter)) {
			strString = strString.substring(strDelimiter.length());
		}
		if (!strString.endsWith(strDelimiter)) {
			strString += strDelimiter;
		}
		while ((iIndexOfDelimiter = strString.indexOf(strDelimiter, iIndexOfInnerString)) != -1) {
			iOccurrences += 1;
			iIndexOfInnerString = iIndexOfDelimiter + strDelimiter.length();
		}
		strArray = new String[iOccurrences];
		iIndexOfInnerString = 0;
		iIndexOfDelimiter = 0;
		while ((iIndexOfDelimiter = strString.indexOf(strDelimiter, iIndexOfInnerString)) != -1) {
			strArray[iCounter] = strString.substring(iIndexOfInnerString, iIndexOfDelimiter);
			iIndexOfInnerString = iIndexOfDelimiter + strDelimiter.length();
			iCounter += 1;
		}

		return strArray;
	}
}