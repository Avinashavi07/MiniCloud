package com.minicloud.infrastructure.hypervisor;

import com.minicloud.common.exception.InvalidLibvirtNameException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * libvirt domain names must be passed as discrete process arguments.
 * This validator rejects anything that could be treated as a flag or metacharacter.
 */
public final class LibvirtNames {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,63}$");

    private LibvirtNames() {
    }

    public static boolean isSafe(String name) {
        return name != null && SAFE.matcher(name).matches();
    }

    public static String requireSafe(String name) {
        return requireSafe(name, "libvirt");
    }

    public static String requireSafe(String name, String kind) {
        if (!isSafe(name)) {
            throw new InvalidLibvirtNameException(kind, name);
        }
        return name;
    }

    public static String requireMac(String mac) {
        if (mac == null || !mac.matches("(?i)^([0-9a-f]{2}:){5}[0-9a-f]{2}$")) {
            throw new InvalidLibvirtNameException("MAC address", mac);
        }
        return mac;
    }

    public static String requireNicModel(String model) {
        if (model == null || !model.matches("(?i)^(virtio|e1000|rtl8139)$")) {
            throw new InvalidLibvirtNameException("NIC model", model);
        }
        return model.toLowerCase(Locale.ROOT);
    }

    public static String requireDiskTarget(String target) {
        if (target == null || !target.matches("^[sv]d[a-z]$")) {
            throw new InvalidLibvirtNameException("disk target", target);
        }
        return target;
    }
}
