package com.minicloud.infrastructure.network;

/**
 * Builds libvirt network XML for a NAT + DHCP network.
 * Callers must already validate names and addresses.
 */
public final class NetworkXmlFactory {

    private NetworkXmlFactory() {
    }

    public static String natNetwork(String name, String gateway, String netmask, String dhcpStart, String dhcpEnd) {
        StringBuilder xml = new StringBuilder();
        xml.append("<network>\n");
        xml.append("  <name>").append(escape(name)).append("</name>\n");
        xml.append("  <forward mode='nat'>\n");
        xml.append("    <nat>\n");
        xml.append("      <port start='1024' end='65535'/>\n");
        xml.append("    </nat>\n");
        xml.append("  </forward>\n");
        xml.append("  <ip address='").append(escape(gateway)).append("' netmask='").append(escape(netmask)).append("'>\n");
        xml.append("    <dhcp>\n");
        xml.append("      <range start='").append(escape(dhcpStart)).append("' end='").append(escape(dhcpEnd)).append("'/>\n");
        xml.append("    </dhcp>\n");
        xml.append("  </ip>\n");
        xml.append("</network>\n");
        return xml.toString();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
