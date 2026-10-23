package com.minicloud.infrastructure.host;

import java.time.Instant;

/**
 * Live measurements collected from the operating system. Null numeric fields
 * mean the value could not be read — they are never invented.
 */
public record HostSnapshot(
        String hostname,
        String osName,
        String osVersion,
        String kernel,
        String architecture,
        Integer cpuCores,
        String cpuModel,
        Double cpuUsagePercent,
        Double loadAverage1,
        Long memoryTotalBytes,
        Long memoryAvailableBytes,
        Long diskTotalBytes,
        Long diskAvailableBytes,
        String diskMount,
        KvmDetection kvm,
        LibvirtDetection libvirt,
        Instant collectedAt
) {
    public record KvmDetection(
            boolean devicePresent,
            boolean moduleLoaded,
            boolean hardwareVirtualization,
            String vendorHint,
            String status,
            String detail
    ) {
    }

    public record LibvirtDetection(
            boolean socketPresent,
            String socketPath,
            String serviceState,
            boolean virshAvailable,
            boolean connectionSucceeded,
            String uri,
            String status,
            String detail
    ) {
    }
}
