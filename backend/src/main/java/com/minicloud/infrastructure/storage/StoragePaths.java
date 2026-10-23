package com.minicloud.infrastructure.storage;

import com.minicloud.common.exception.InvalidStorageConfigurationException;
import com.minicloud.infrastructure.hypervisor.LibvirtNames;

import java.nio.file.Path;

/**
 * Phase 5 only allows directory pools under the libvirt images tree.
 * Arbitrary host paths are rejected so MiniCloud cannot point a pool at /etc.
 */
public final class StoragePaths {

    public static final Path ROOT = Path.of("/var/lib/libvirt/images");

    private StoragePaths() {
    }

    public static String requireSafeDirectory(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidStorageConfigurationException("Storage pool path is required");
        }
        Path resolved = Path.of(path).normalize();
        if (!resolved.isAbsolute() || path.contains("..") || path.contains(" ")) {
            throw new InvalidStorageConfigurationException("Pool path must be an absolute directory without '..' or spaces");
        }
        if (!resolved.equals(ROOT) && !resolved.startsWith(ROOT)) {
            throw new InvalidStorageConfigurationException("Pool path must be under " + ROOT);
        }
        if (!resolved.equals(ROOT)) {
            Path relative = ROOT.relativize(resolved);
            for (Path part : relative) {
                if (!LibvirtNames.isSafe(part.toString())) {
                    throw new InvalidStorageConfigurationException("Unsafe path component: " + part);
                }
            }
        }
        return resolved.toString();
    }

    public static String requireSafeVolumePath(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidStorageConfigurationException("Volume path is required");
        }
        Path resolved = Path.of(path).normalize();
        if (!resolved.isAbsolute() || path.contains("..") || !resolved.startsWith(ROOT)) {
            throw new InvalidStorageConfigurationException("Volume path must be under " + ROOT);
        }
        Path fileName = resolved.getFileName();
        if (fileName == null || !LibvirtNames.isSafe(fileName.toString())) {
            throw new InvalidStorageConfigurationException("Unsafe volume file name");
        }
        return resolved.toString();
    }

    public static String requireFormat(String format) {
        if (format == null || !(format.equalsIgnoreCase("qcow2") || format.equalsIgnoreCase("raw"))) {
            throw new InvalidStorageConfigurationException("Volume format must be qcow2 or raw");
        }
        return format.toLowerCase(java.util.Locale.ROOT);
    }
}
