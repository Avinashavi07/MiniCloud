package com.minicloud.infrastructure.ovs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ovs-vsctl text. Kept separate so unit tests do not need OVS.
 */
public final class OvsOutputParser {

    private static final Pattern VERSION = Pattern.compile("ovs-vsctl \\(Open vSwitch\\) ([0-9.]+)");
    private static final Pattern DB_VERSION = Pattern.compile("DB Schema ([0-9.]+)");

    private OvsOutputParser() {
    }

    public static List<String> parseNameList(String stdout) {
        List<String> names = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return names;
        }
        for (String raw : stdout.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (OvsNames.isSafe(line)) {
                names.add(line);
            }
        }
        return names;
    }

    public static String parseVersion(String stdout) {
        if (stdout == null) {
            return null;
        }
        Matcher matcher = VERSION.matcher(stdout);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String parseDbVersion(String stdout) {
        if (stdout == null) {
            return null;
        }
        Matcher matcher = DB_VERSION.matcher(stdout);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.equals("[]") || trimmed.equals("{}") || trimmed.equals("\"\"")) {
            return null;
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static boolean externalIdsMarkManaged(String externalIds) {
        if (externalIds == null || externalIds.isBlank()) {
            return false;
        }
        String normalized = externalIds.toLowerCase(Locale.ROOT).replace("\"", "");
        return normalized.contains(OvsNames.EXTERNAL_ID_KEY + "=" + OvsNames.EXTERNAL_ID_VALUE)
                || normalized.contains(OvsNames.EXTERNAL_ID_KEY + ":" + OvsNames.EXTERNAL_ID_VALUE);
    }

    public static List<String> parseUuidSet(String stdout) {
        List<String> ids = new ArrayList<>();
        if (stdout == null || stdout.isBlank() || "[]".equals(stdout.trim())) {
            return ids;
        }
        String body = stdout.trim();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        for (String part : body.split("[,\\s]+")) {
            String id = part.trim();
            if (id.matches("[0-9a-fA-F-]{36}")) {
                ids.add(id);
            }
        }
        return ids;
    }
}
