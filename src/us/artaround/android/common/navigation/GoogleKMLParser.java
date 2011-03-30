package us.artaround.android.common.navigation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.android.maps.GeoPoint;

public class GoogleKMLParser {

	private static final String ELEMENT_PLACEMARK = "Placemark";
	private static final String ELEMENT_NAME = "name";
	private static final String ELEMENT_DESC = "description";
	private static final String ELEMENT_POINT = "Point";
	private static final String ELEMENT_ROUTE = "Route";
	private static final String ELEMENT_GEOM = "GeometryCollection";

	public Route parseRoute(InputStream is) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(is);

		NodeList placemarkList = document.getElementsByTagName(ELEMENT_PLACEMARK);

		List<Placemark> placemarks = new ArrayList<Placemark>();
		for (int i = 0; i < placemarkList.getLength(); i++) {
			Placemark placemark = parsePlacemark(placemarkList.item(i));
			if (placemark != null) {
				placemarks.add(placemark);
			}
		}

		Route route = parseRoute(placemarkList);
		route.setPlacemarks(placemarks);

		return route;
	}

	private Placemark parsePlacemark(Node item) {
		Placemark placemark = new Placemark();

		boolean isRouteElement = false;
		NodeList children = item.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeName().equals(ELEMENT_NAME)) {
				// Get the value of the <name> KML tag.
				// If the value is "Route", this is not a placemark description
				// but a route description.
				//
				String name = node.getFirstChild().getNodeValue();
				if (name.equals(ELEMENT_ROUTE)) {
					isRouteElement = true;
				}
				else {
					isRouteElement = false;
					placemark.setInstructions(name);
				}
			}
			else if (node.getNodeName().equals(ELEMENT_DESC)) {
				// Get the value of the <description> KML tag if it is a placemark
				// that is being described (not a route).
				//
				if (!isRouteElement) {
					String distance = node.getFirstChild().getNodeValue();
					// placemark.setDistance(distance.substring(3).replace("&#160;", " "));
					placemark.setDistance(cleanup(distance));
				}
			}
			else if (node.getNodeName().equals(ELEMENT_POINT)) {
				// Get the value of the <Point> coordinates KML tag if it is a placemark
				// that is being described (not a route).
				//
				if (!isRouteElement) {
					String coords = node.getFirstChild().getFirstChild().getNodeValue();
					String[] latlon = coords.split(",");
					placemark.setGeoPoint(new GeoPoint((int) (Double.parseDouble(latlon[1]) * 1E6), (int) (Double
							.parseDouble(latlon[0]) * 1E6)));
				}
			}
		}

		return isRouteElement ? null : placemark;
	}

	private Route parseRoute(NodeList placemarkList) {
		Route route = null;

		for (int i = 0; i < placemarkList.getLength(); i++) {
			// Iterate through all the <Placemark> KML tags to find the one
			// whose child <name> tag is "Route".
			//
			Node item = placemarkList.item(i);
			NodeList children = item.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node node = children.item(j);
				if (node.getNodeName().equals(ELEMENT_NAME)) {
					String name = node.getFirstChild().getNodeValue();
					if (name.equals(ELEMENT_ROUTE)) {
						route = parseRoute(item);
						break;
					}
				}
			}
		}

		return route;
	}

	private Route parseRoute(Node item) {
		Route route = new Route();

		NodeList children = item.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);

			if (node.getNodeName().equals(ELEMENT_DESC)) {
				// Get the value of the <description> KML tag.
				//
				String distance = node.getFirstChild().getNodeValue();
				// route.setTotalDistance(distance.split("<br/>")[0].substring(10).replace("&#160;",
				// " "));
				distance = cleanup(distance);
				route.setTotalDistance(distance);
			}
			else if (node.getNodeName().equals(ELEMENT_GEOM)) {
				// Get the space-separated coordinates of the geographical points defining the
				// route.
				//
				String path = node.getFirstChild().getFirstChild().getFirstChild().getNodeValue();
				String[] pairs = path.split(" ");

				// For each coordinate, get its {latitude, longitude} values and add the
				// corresponding
				// geographical point to the route.
				//
				List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();
				for (int p = 0; p < pairs.length; p++) {
					String[] coords = pairs[p].split(",");
					GeoPoint geoPoint = new GeoPoint((int) (Double.parseDouble(coords[1]) * 1E6),
							(int) (Double.parseDouble(coords[0]) * 1E6));
					geoPoints.add(geoPoint);
				}
				route.setGeoPoints(geoPoints);
			}
		}

		return route;
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
}
