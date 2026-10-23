package com.minicloud.host.dto;

import com.minicloud.infrastructure.host.HostSnapshot;

import java.time.Instant;

public record HostMetricsResponse(
        Integer cpuCores,
        String cpuModel,
        Double cpuUsagePercent,
        Double loadAverage1,
        Long memoryTotalBytes,
        Long memoryAvailableBytes,
        Long memoryUsedBytes,
        Double memoryUsagePercent,
        Long diskTotalBytes,
        Long diskAvailableBytes,
        Long diskUsedBytes,
        Double diskUsagePercent,
        String diskMount,
        HostSnapshot.KvmDetection kvm,
        HostSnapshot.LibvirtDetection libvirt,
        Instant collectedAt
) {
    public static HostMetricsResponse from(HostSnapshot snapshot) {
        Long memoryUsed = used(snapshot.memoryTotalBytes(), snapshot.memoryAvailableBytes());
        Long diskUsed = used(snapshot.diskTotalBytes(), snapshot.diskAvailableBytes());
        return new HostMetricsResponse(
                snapshot.cpuCores(),
                snapshot.cpuModel(),
                snapshot.cpuUsagePercent(),
                snapshot.loadAverage1(),
                snapshot.memoryTotalBytes(),
                snapshot.memoryAvailableBytes(),
                memoryUsed,
                percent(memoryUsed, snapshot.memoryTotalBytes()),
                snapshot.diskTotalBytes(),
                snapshot.diskAvailableBytes(),
                diskUsed,
                percent(diskUsed, snapshot.diskTotalBytes()),
                snapshot.diskMount(),
                snapshot.kvm(),
                snapshot.libvirt(),
                snapshot.collectedAt()
        );
    }

    private static Long used(Long total, Long available) {
        if (total == null || available == null) {
            return null;
        }
        return Math.max(0, total - available);
    }

    private static Double percent(Long used, Long total) {
        if (used == null || total == null || total == 0) {
            return null;
        }
        return (used * 100.0) / total;
    }
}
