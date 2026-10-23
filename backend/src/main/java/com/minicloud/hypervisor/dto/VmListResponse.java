package com.minicloud.hypervisor.dto;

import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;

import java.util.List;

public record VmListResponse(
        boolean listingAvailable,
        String note,
        HypervisorCapabilities hypervisor,
        List<VirtualMachineInfo> vms
) {
}
