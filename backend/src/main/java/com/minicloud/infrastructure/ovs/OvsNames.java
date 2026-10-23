package com.minicloud.infrastructure.ovs;

import com.minicloud.common.exception.InvalidOvsConfigurationException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bridge names become process arguments and Linux interface names.
 * Phase 7 only creates MiniCloud lab bridges (mc-*).
 */
public final class OvsNames {

    public static final String MANAGED_PREFIX = "mc-";
    public static final String EXTERNAL_ID_KEY = "minicloud";
    public static final String EXTERNAL_ID_VALUE = "true";

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,14}$");
    private static final Set<String> RESERVED = Set.of(
            "br0", "br1", "ovsbr0", "ovs-system", "docker0", "virbr0", "virbr1", "lo"
    );

    private OvsNames() {
    }

    public static boolean isSafe(String name) {
        return name != null && SAFE.matcher(name).matches();
    }

    public static String requireSafe(String name) {
        if (!isSafe(name)) {
            throw new InvalidOvsConfigurationException(
                    "Bridge name must be 1–15 characters: letters, digits, '_', '.', '-'");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(lower) || lower.startsWith("virbr") || lower.startsWith("docker")) {
            throw new InvalidOvsConfigurationException("Bridge name '" + name + "' is reserved for the host.");
        }
        return name;
    }

    public static String requireManagedCreateName(String name) {
        String safe = requireSafe(name);
        if (!safe.startsWith(MANAGED_PREFIX)) {
            throw new InvalidOvsConfigurationException(
                    "MiniCloud creates only bridges named " + MANAGED_PREFIX + "*. Example: mc-br0");
        }
        return safe;
    }

    public static boolean looksManaged(String name) {
        return name != null && name.startsWith(MANAGED_PREFIX);
    }
}
