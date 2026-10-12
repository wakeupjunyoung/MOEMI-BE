package com.example.moemi.hazard.its;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.hazard.HazardFetchException;

/**
 * Parses the ITS eventInfo XML response:
 * {@code <response><header><resultCode>0</resultCode>...</header><body><items><item>...</item></items></body></response>}.
 */
final class ItsEventParser {

    private static final String SUCCESS = "0";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ItsEventParser() {
    }

    static List<ItsEvent> parse(byte[] body) {
        Element root = readDocument(body).getDocumentElement();
        String resultCode = text(root, "resultCode");
        if (!SUCCESS.equals(resultCode)) {
            throw new HazardFetchException("ITS resultCode=" + resultCode + " " + text(root, "resultMsg"));
        }

        List<ItsEvent> events = new ArrayList<>();
        NodeList items = root.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            GeoPoint location = location(item);
            if (location == null) {
                continue;
            }
            events.add(new ItsEvent(
                    text(item, "type"),
                    text(item, "eventType"),
                    text(item, "eventDetailType"),
                    dateTime(text(item, "startDate")),
                    dateTime(text(item, "endDate")),
                    location,
                    text(item, "linkId"),
                    text(item, "roadName"),
                    text(item, "roadNo"),
                    text(item, "roadDrcType"),
                    text(item, "lanesBlockType"),
                    text(item, "lanesBlocked"),
                    text(item, "message")));
        }
        return events;
    }

    private static Document readDocument(byte[] body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The response is external input: refuse DTDs and external entities (XXE).
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new HazardFetchException("ITS response is not valid XML: " + e.getClass().getSimpleName());
        }
    }

    private static GeoPoint location(Element item) {
        try {
            String x = text(item, "coordX");
            String y = text(item, "coordY");
            if (x == null || y == null) {
                return null;
            }
            return new GeoPoint(Double.parseDouble(y), Double.parseDouble(x));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime dateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Text of the first descendant with the tag, or {@code null} when missing or blank.
     * Line breaks and indentation inside the source text are collapsed to single spaces.
     */
    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        String text = WHITESPACE.matcher(node.getTextContent()).replaceAll(" ").trim();
        return text.isEmpty() ? null : text;
    }
}
