package com.minicloud.infrastructure.console;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads the first &lt;graphics&gt; device from domain XML.
 */
public final class GraphicsXmlParser {

    private GraphicsXmlParser() {
    }

    public record GraphicsDevice(String type, String listen, Integer port, boolean autoport) {
    }

    public static GraphicsDevice parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = document.getElementsByTagName("graphics");
            if (nodes.getLength() == 0) {
                return null;
            }
            Element graphics = (Element) nodes.item(0);
            String type = blankToNull(graphics.getAttribute("type"));
            String listen = blankToNull(graphics.getAttribute("listen"));
            NodeList listens = graphics.getElementsByTagName("listen");
            if (listens.getLength() > 0) {
                Element listenEl = (Element) listens.item(0);
                String address = blankToNull(listenEl.getAttribute("address"));
                if (address != null) {
                    listen = address;
                }
            }
            Integer port = parsePort(graphics.getAttribute("port"));
            boolean autoport = "yes".equalsIgnoreCase(graphics.getAttribute("autoport"));
            return new GraphicsDevice(type, listen, port, autoport);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Integer parsePort(String raw) {
        if (raw == null || raw.isBlank() || "-1".equals(raw.trim())) {
            return null;
        }
        try {
            int port = Integer.parseInt(raw.trim());
            return port > 0 ? port : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
