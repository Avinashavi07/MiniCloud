package com.minicloud.infrastructure.storage;

import java.util.List;

public record StorageVolume(
        String name,
        String pool,
        String path,
        String type,
        String format,
        Long capacityBytes,
        Long allocationBytes,
        List<ConnectedDiskInfo> usedBy
) {
    public StorageVolume {
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
    }

    public StorageVolume withUsedBy(List<ConnectedDiskInfo> disks) {
        return new StorageVolume(name, pool, path, type, format, capacityBytes, allocationBytes, disks);
    }
}
