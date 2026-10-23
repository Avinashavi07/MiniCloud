package com.minicloud.infrastructure.ceph;

import com.minicloud.common.exception.CephNotAvailableException;
import com.minicloud.common.exception.CephOperationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads monitor hosts and the admin key from local Ceph client files.
 * Never logs the key value.
 */
public final class CephClientConfig {

    private static final Pattern MON_HOST = Pattern.compile("(?im)^\\s*mon[_ ]host\\s*=\\s*(.+)$");
    private static final Pattern KEY = Pattern.compile("(?im)^\\s*key\\s*=\\s*(\\S+)\\s*$");
    /** Stable libvirt secret UUID for MiniCloud's client.admin key. */
    public static final String LIBVIRT_SECRET_UUID = "a7f3c2e1-9b4d-4e8a-9c1f-2d6e8b0a5f31";

    private CephClientConfig() {
    }

    public static Path confPath() {
        Path conf = CephCli.resolveConf();
        if (conf == null) {
            throw new CephNotAvailableException("No ceph.conf found under MicroCeph or /etc/ceph.");
        }
        return conf;
    }

    public static Path keyringPath() {
        Path keyring = CephCli.resolveKeyring();
        if (keyring == null) {
            throw new CephNotAvailableException(
                    "No readable ceph.client.admin.keyring. Grant the libvirt group read access.");
        }
        return keyring;
    }

    public static List<String> monitorHosts() {
        try {
            String text = Files.readString(confPath());
            Matcher matcher = MON_HOST.matcher(text);
            if (!matcher.find()) {
                throw new CephOperationException("ceph.conf has no mon_host / mon host entry.");
            }
            List<String> hosts = new ArrayList<>();
            for (String part : matcher.group(1).split("[,\\s]+")) {
                String host = part.trim();
                if (!host.isEmpty()) {
                    // strip optional port; libvirt host element can carry port separately
                    int colon = host.lastIndexOf(':');
                    if (colon > 0 && host.indexOf(']') < 0) {
                        hosts.add(host.substring(0, colon));
                    } else {
                        hosts.add(host.replace("[", "").replace("]", ""));
                    }
                }
            }
            if (hosts.isEmpty()) {
                throw new CephOperationException("ceph.conf mon_host is empty.");
            }
            return List.copyOf(hosts);
        } catch (IOException ex) {
            throw new CephOperationException("Could not read ceph.conf: " + ex.getMessage());
        }
    }

    /** Base64 cephx key for client.admin. Caller must not log the return value. */
    public static String adminKey() {
        try {
            String text = Files.readString(keyringPath());
            Matcher matcher = KEY.matcher(text);
            if (!matcher.find()) {
                throw new CephOperationException("keyring has no key= entry.");
            }
            return matcher.group(1).trim();
        } catch (IOException ex) {
            throw new CephOperationException("Could not read Ceph keyring: " + ex.getMessage());
        }
    }

    public static String secretXml() {
        return """
                <secret ephemeral='no' private='yes'>
                  <uuid>%s</uuid>
                  <usage type='ceph'>
                    <name>client.admin secret</name>
                  </usage>
                </secret>
                """.formatted(LIBVIRT_SECRET_UUID);
    }

    public static String rbdDiskXml(String pool, String image, String target, List<String> monitors, String secretUuid) {
        String safePool = CephNames.requireSafe(pool, "pool");
        String safeImage = CephNames.requireImage(image);
        StringBuilder xml = new StringBuilder();
        xml.append("<disk type='network' device='disk'>\n");
        xml.append("  <driver name='qemu' type='raw'/>\n");
        xml.append("  <auth username='admin'>\n");
        xml.append("    <secret type='ceph' uuid='").append(escape(secretUuid)).append("'/>\n");
        xml.append("  </auth>\n");
        xml.append("  <source protocol='rbd' name='")
                .append(escape(safePool)).append('/').append(escape(safeImage)).append("'>\n");
        for (String mon : monitors) {
            xml.append("    <host name='").append(escape(mon)).append("' port='6789'/>\n");
        }
        xml.append("  </source>\n");
        xml.append("  <target dev='").append(escape(target.toLowerCase(Locale.ROOT))).append("' bus='virtio'/>\n");
        xml.append("</disk>\n");
        return xml.toString();
    }

    private static String escape(String value) {
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
