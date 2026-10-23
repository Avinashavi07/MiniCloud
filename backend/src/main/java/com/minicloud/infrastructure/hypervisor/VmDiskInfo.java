package com.minicloud.infrastructure.hypervisor;

public record VmDiskInfo(
        String device,
        String type,
        String target,
        String source
) {
}
