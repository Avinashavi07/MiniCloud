package com.minicloud.infrastructure.hypervisor;

public record CreateVmRequest(
        String name,
        int cpuCores,
        long memoryMb,
        long diskGb,
        String operatingSystem,
        String network,
        boolean startAfterCreate
) {
}
