package com.minicloud.infrastructure.storage;

import java.util.List;

/**
 * Live libvirt storage pool. Not a PostgreSQL row.
 */
public record VirtualStoragePool(
        String name,
        String uuid,
        String state,
        boolean active,
        boolean persistent,
        boolean autostart,
        String type,
        String path,
        Long capacityBytes,
        Long allocationBytes,
        Long availableBytes,
        List<StorageVolume> volumes
) {
    public VirtualStoragePool {
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
    }

    public VirtualStoragePool withVolumes(List<StorageVolume> next) {
        return new VirtualStoragePool(
                name, uuid, state, active, persistent, autostart, type, path,
                capacityBytes, allocationBytes, availableBytes, next
        );
    }
}
