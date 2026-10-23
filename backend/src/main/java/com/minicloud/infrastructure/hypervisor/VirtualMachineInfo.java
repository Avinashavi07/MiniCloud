package com.minicloud.infrastructure.hypervisor;

import java.util.List;

/**
 * Read-only view of a domain reported by libvirt.
 * This is not a database record pretending to be a VM.
 */
public record VirtualMachineInfo(
        String name,
        String uuid,
        String hypervisorId,
        String state,
        String normalizedState,
        Integer cpuCores,
        Long memoryBytes,
        Long memoryUsedBytes,
        String osType,
        Boolean persistent,
        Boolean autostart,
        List<VmDiskInfo> disks,
        List<VmNicInfo> nics
) {
    public VirtualMachineInfo {
        disks = disks == null ? List.of() : List.copyOf(disks);
        nics = nics == null ? List.of() : List.copyOf(nics);
    }

    public static VirtualMachineInfo summary(String name, String hypervisorId, String state, String normalizedState) {
        return new VirtualMachineInfo(
                name, null, hypervisorId, state, normalizedState,
                null, null, null, null, null, null, List.of(), List.of()
        );
    }
}
