package com.minicloud.host.dto;

import com.minicloud.host.entity.Host;

import java.time.Instant;
import java.util.UUID;

public record HostResponse(
        UUID id,
        String hostname,
        String osName,
        String osVersion,
        String kernel,
        String architecture,
        boolean local,
        String kvmStatus,
        String libvirtStatus,
        String libvirtUri,
        Instant lastSeenAt,
        Instant createdAt,
        HostMetricsResponse metrics
) {
    public static HostResponse from(Host host, HostMetricsResponse metrics) {
        return new HostResponse(
                host.getId(),
                host.getHostname(),
                host.getOsName(),
                host.getOsVersion(),
                host.getKernel(),
                host.getArchitecture(),
                host.isLocal(),
                host.getKvmStatus(),
                host.getLibvirtStatus(),
                host.getLibvirtUri(),
                host.getLastSeenAt(),
                host.getCreatedAt(),
                metrics
        );
    }
}
