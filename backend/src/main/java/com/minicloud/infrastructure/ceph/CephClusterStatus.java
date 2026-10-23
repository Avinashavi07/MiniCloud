package com.minicloud.infrastructure.ceph;

import java.util.List;

public record CephClusterStatus(
        String health,
        String healthDetail,
        int monCount,
        int osdCount,
        int osdUp,
        int osdIn,
        long totalBytes,
        long usedBytes,
        long availableBytes,
        List<String> services,
        String rawStatus
) {
    public CephClusterStatus {
        services = services == null ? List.of() : List.copyOf(services);
    }
}
