package com.minicloud.storage.dto;

import com.minicloud.infrastructure.storage.VirtualStoragePool;

import java.util.List;

public record StorageListResponse(
        boolean listingAvailable,
        String note,
        List<HostFilesystemInfo> hostFilesystems,
        List<VirtualStoragePool> pools
) {
}
