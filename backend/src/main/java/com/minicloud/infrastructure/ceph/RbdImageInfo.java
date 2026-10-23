package com.minicloud.infrastructure.ceph;

import java.util.List;

public record RbdImageInfo(
        String name,
        String pool,
        long sizeBytes,
        long usedBytes,
        String format,
        List<String> usedByVms,
        String detail
) {
    public RbdImageInfo {
        usedByVms = usedByVms == null ? List.of() : List.copyOf(usedByVms);
    }

    public RbdImageInfo withUsedBy(List<String> vms) {
        return new RbdImageInfo(name, pool, sizeBytes, usedBytes, format, vms, detail);
    }
}
