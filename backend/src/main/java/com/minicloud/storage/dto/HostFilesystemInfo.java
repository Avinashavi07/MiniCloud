package com.minicloud.storage.dto;

public record HostFilesystemInfo(
        String name,
        String mount,
        String kind,
        Long capacityBytes,
        Long availableBytes,
        String note
) {
}
