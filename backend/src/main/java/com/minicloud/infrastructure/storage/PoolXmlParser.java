package com.minicloud.infrastructure.storage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class PoolXmlParser {

    private PoolXmlParser() {
    }

    public record Addressing(String name, String uuid, String type, String path) {
    }

    public static Addressing parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return new Addressing(null, null, null, null);
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            String type = root.hasAttribute("type") ? root.getAttribute("type") : null;
            String name = text(root, "name");
            String uuid = text(root, "uuid");
            Element target = first(root, "target");
            String path = target == null ? null : text(target, "path");
            return new Addressing(name, uuid, type, path);
        } catch (Exception ex) {
            return new Addressing(null, null, null, null);
        }
    }

    private static Element first(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static String text(Element parent, String tag) {
        Element child = first(parent, tag);
        if (child == null || child.getTextContent() == null || child.getTextContent().isBlank()) {
            return null;
        }
        return child.getTextContent().trim();
    }
}
