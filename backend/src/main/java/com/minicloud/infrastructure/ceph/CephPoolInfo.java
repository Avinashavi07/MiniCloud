package com.minicloud.infrastructure.ceph;

import java.util.List;

public record CephPoolInfo(
        String name,
        Integer id,
        Integer pgNum,
        String application,
        boolean managedByMiniCloud,
        long storedBytes,
        long maxAvailableBytes,
        int objectCount,
        List<RbdImageInfo> images,
        String detail
) {
    public CephPoolInfo {
        images = images == null ? List.of() : List.copyOf(images);
    }

    public CephPoolInfo withImages(List<RbdImageInfo> next) {
        return new CephPoolInfo(
                name, id, pgNum, application, managedByMiniCloud,
                storedBytes, maxAvailableBytes, objectCount, next, detail);
    }
}
