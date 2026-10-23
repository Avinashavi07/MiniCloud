package com.minicloud.infrastructure.ceph;

import com.minicloud.common.exception.InvalidCephConfigurationException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ceph pool / RBD image names become process arguments.
 * MiniCloud only creates pools named {@code mc-*}.
 */
public final class CephNames {

    public static final String MANAGED_PREFIX = "mc-";
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$");
    private static final Set<String> RESERVED = Set.of(
            "device_health_metrics", ".mgr", ".rgw.root", "cephfs_metadata", "cephfs_data"
    );

    private CephNames() {
    }

    public static boolean isSafe(String name) {
        return name != null && SAFE.matcher(name).matches();
    }

    public static String requireSafe(String name, String kind) {
        if (!isSafe(name)) {
            throw new InvalidCephConfigurationException(
                    kind + " name must be 1–64 characters: letters, digits, '_', '.', '-'");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(lower) || lower.startsWith(".")) {
            throw new InvalidCephConfigurationException(kind + " name '" + name + "' is reserved.");
        }
        return name;
    }

    public static String requireManagedPool(String name) {
        String safe = requireSafe(name, "pool");
        if (!safe.startsWith(MANAGED_PREFIX)) {
            throw new InvalidCephConfigurationException(
                    "MiniCloud creates only Ceph pools named " + MANAGED_PREFIX + "*. Example: mc-rbd");
        }
        return safe;
    }

    public static boolean looksManaged(String name) {
        return name != null && name.startsWith(MANAGED_PREFIX);
    }

    public static String requireImage(String name) {
        return requireSafe(name, "image");
    }
}
