package com.minicloud.infrastructure.network;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads addressing fields from {@code virsh net-dumpxml}.
 */
public final class NetworkXmlParser {

    private NetworkXmlParser() {
    }

    public record Addressing(
            String name,
            String uuid,
            String forwardMode,
            String bridge,
            String gateway,
            String netmask,
            String cidr,
            String dhcpStart,
            String dhcpEnd
    ) {
    }

    public static Addressing parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return new Addressing(null, null, null, null, null, null, null, null, null);
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            String name = text(root, "name");
            String uuid = text(root, "uuid");
            String forwardMode = attribute(first(root, "forward"), "mode");
            String bridge = attribute(first(root, "bridge"), "name");
            Element ip = first(root, "ip");
            String gateway = attribute(ip, "address");
            String netmask = attribute(ip, "netmask");
            String cidr = null;
            if (gateway != null && netmask != null) {
                cidr = Ipv4Networks.cidrFromGatewayAndMask(gateway, netmask);
            }
            Element dhcp = ip == null ? null : first(ip, "dhcp");
            Element range = dhcp == null ? null : first(dhcp, "range");
            return new Addressing(
                    name,
                    uuid,
                    forwardMode,
                    bridge,
                    gateway,
                    netmask,
                    cidr,
                    attribute(range, "start"),
                    attribute(range, "end")
            );
        } catch (Exception ex) {
            return new Addressing(null, null, null, null, null, null, null, null, null);
        }
    }

    private static Element first(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private static String text(Element parent, String tag) {
        Element child = first(parent, tag);
        if (child == null || child.getTextContent() == null || child.getTextContent().isBlank()) {
            return null;
        }
        return child.getTextContent().trim();
    }

    private static String attribute(Element element, String name) {
        if (element == null || !element.hasAttribute(name)) {
            return null;
        }
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? null : value;
    }
}
