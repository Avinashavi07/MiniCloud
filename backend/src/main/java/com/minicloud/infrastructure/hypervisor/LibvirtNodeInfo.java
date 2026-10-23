package com.minicloud.infrastructure.hypervisor;

public record LibvirtNodeInfo(
        String cpuModel,
        Integer cpus,
        Integer sockets,
        Integer coresPerSocket,
        Integer threadsPerCore,
        Long memoryBytes
) {
}
