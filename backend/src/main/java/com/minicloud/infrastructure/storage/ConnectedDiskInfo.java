package com.minicloud.infrastructure.storage;

public record ConnectedDiskInfo(
        String vmName,
        String target,
        String source
) {
}
