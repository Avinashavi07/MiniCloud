package com.minicloud.infrastructure.ceph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ceph / rbd JSON and text. Kept separate so tests do not need a cluster.
 */
public final class CephOutputParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern VERSION = Pattern.compile("ceph version ([0-9.]+)");

    private CephOutputParser() {
    }

    public static String parseVersion(String stdout) {
        if (stdout == null) {
            return null;
        }
        Matcher matcher = VERSION.matcher(stdout);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String parseHealthStatus(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            return "UNKNOWN";
        }
        String first = stdout.trim().split("\\R", 2)[0].trim();
        if (first.startsWith("HEALTH_")) {
            return first.split("\\s+", 2)[0];
        }
        for (String token : List.of("HEALTH_OK", "HEALTH_WARN", "HEALTH_ERR")) {
            if (stdout.contains(token)) {
                return token;
            }
        }
        return "UNKNOWN";
    }

    public static CephClusterStatus parseStatusText(String stdout) {
        String health = parseHealthStatus(stdout);
        int mons = extractInt(stdout, "mon:\\s*(\\d+)");
        int osds = extractInt(stdout, "osd:\\s*(\\d+)");
        int up = extractInt(stdout, "(\\d+)\\s+up");
        int in = extractInt(stdout, "(\\d+)\\s+in");
        long total = extractBytes(stdout, "(\\d+(?:\\.\\d+)?)\\s*(KiB|MiB|GiB|TiB|PiB)\\s*/\\s*");
        // ceph -s usage line is like: usage: 123 MiB used, 10 GiB / 12 GiB avail
        long avail = extractLastBytes(stdout, "(\\d+(?:\\.\\d+)?)\\s*(KiB|MiB|GiB|TiB)\\s+avail");
        long used = extractBytes(stdout, "(\\d+(?:\\.\\d+)?)\\s*(KiB|MiB|GiB|TiB)\\s+used");
        List<String> services = new ArrayList<>();
        if (stdout.contains("mgr:")) {
            services.add("mgr");
        }
        if (mons > 0) {
            services.add("mon");
        }
        if (osds > 0) {
            services.add("osd");
        }
        return new CephClusterStatus(
                health,
                firstNonBlankLine(stdout),
                mons, osds, up, in,
                total > 0 ? total : used + avail,
                used, avail,
                services,
                stdout == null ? "" : stdout.trim()
        );
    }

    public static List<String> parsePoolNamesJson(String stdout) {
        List<String> names = new ArrayList<>();
        JsonNode root = readTree(stdout);
        if (root == null) {
            return names;
        }
        if (root.isArray()) {
            for (JsonNode node : root) {
                if (node.isTextual() && CephNames.isSafe(node.asText())) {
                    names.add(node.asText());
                }
            }
        }
        return names;
    }

    public static List<CephPoolInfo> parsePoolDetailsJson(String stdout) {
        List<CephPoolInfo> pools = new ArrayList<>();
        JsonNode root = readTree(stdout);
        if (root == null || !root.isArray()) {
            return pools;
        }
        for (JsonNode node : root) {
            String name = text(node, "pool_name");
            if (!CephNames.isSafe(name)) {
                continue;
            }
            Integer id = intOrNull(node, "pool");
            Integer pgNum = intOrNull(node, "pg_num");
            String application = firstApplication(node.get("application_metadata"));
            boolean managed = CephNames.looksManaged(name);
            pools.add(new CephPoolInfo(
                    name, id, pgNum, application, managed,
                    0, 0, 0, List.of(),
                    managed ? "MiniCloud-managed Ceph pool" : "Cluster pool (read-only deletes in MiniCloud)"
            ));
        }
        return pools;
    }

    public static void applyDfStats(List<CephPoolInfo> pools, String dfJson) {
        JsonNode root = readTree(dfJson);
        if (root == null) {
            return;
        }
        JsonNode poolStats = root.path("pools");
        if (!poolStats.isArray()) {
            return;
        }
        for (int i = 0; i < pools.size(); i++) {
            CephPoolInfo pool = pools.get(i);
            for (JsonNode node : poolStats) {
                if (!pool.name().equals(text(node, "name"))) {
                    continue;
                }
                JsonNode stats = node.path("stats");
                pools.set(i, new CephPoolInfo(
                        pool.name(), pool.id(), pool.pgNum(), pool.application(), pool.managedByMiniCloud(),
                        stats.path("stored").asLong(0),
                        stats.path("max_avail").asLong(0),
                        stats.path("objects").asInt(0),
                        pool.images(),
                        pool.detail()
                ));
            }
        }
    }

    public static List<String> parseRbdNamesJson(String stdout) {
        List<String> names = new ArrayList<>();
        JsonNode root = readTree(stdout);
        if (root == null) {
            return names;
        }
        if (root.isArray()) {
            for (JsonNode node : root) {
                if (node.isTextual() && CephNames.isSafe(node.asText())) {
                    names.add(node.asText());
                }
            }
        }
        return names;
    }

    public static RbdImageInfo parseRbdInfoJson(String pool, String stdout) {
        JsonNode root = readTree(stdout);
        if (root == null || root.isMissingNode()) {
            return null;
        }
        String name = text(root, "name");
        if (!CephNames.isSafe(name)) {
            return null;
        }
        long size = root.path("size").asLong(0);
        int format = root.path("format").asInt(2);
        return new RbdImageInfo(
                name, pool, size, 0, "rbd" + format, List.of(),
                "RBD image in pool " + pool);
    }

    public static long parseRbdUsedBytes(String duJson) {
        JsonNode root = readTree(duJson);
        if (root == null) {
            return 0;
        }
        JsonNode images = root.path("images");
        if (images.isArray() && !images.isEmpty()) {
            return images.get(0).path("used_size").asLong(0);
        }
        return root.path("total_used_size").asLong(0);
    }

    private static String firstApplication(JsonNode metadata) {
        if (metadata == null || !metadata.isObject()) {
            return null;
        }
        Iterator<String> fields = metadata.fieldNames();
        return fields.hasNext() ? fields.next() : null;
    }

    private static JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.canConvertToInt() ? null : value.asInt();
    }

    private static int extractInt(String text, String regex) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static long extractBytes(String text, String regex) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        return toBytes(Double.parseDouble(matcher.group(1)), matcher.group(2));
    }

    private static long extractLastBytes(String text, String regex) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        long value = 0;
        while (matcher.find()) {
            value = toBytes(Double.parseDouble(matcher.group(1)), matcher.group(2));
        }
        return value;
    }

    private static long toBytes(double amount, String unit) {
        String u = unit.toUpperCase(Locale.ROOT);
        double factor = switch (u) {
            case "KIB" -> 1024d;
            case "MIB" -> 1024d * 1024d;
            case "GIB" -> 1024d * 1024d * 1024d;
            case "TIB" -> 1024d * 1024d * 1024d * 1024d;
            case "PIB" -> 1024d * 1024d * 1024d * 1024d * 1024d;
            default -> 1d;
        };
        return Math.round(amount * factor);
    }

    private static String firstNonBlankLine(String text) {
        if (text == null) {
            return "";
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return "";
    }
}
