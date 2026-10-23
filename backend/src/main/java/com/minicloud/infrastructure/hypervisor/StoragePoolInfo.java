package com.minicloud.infrastructure.hypervisor;

public record StoragePoolInfo(
        String name,
        String state,
        Long capacityBytes,
        Long allocationBytes,
        Long availableBytes
) {
}
