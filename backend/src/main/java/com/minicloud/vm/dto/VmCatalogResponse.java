package com.minicloud.vm.dto;

import com.minicloud.infrastructure.hypervisor.IsoCatalog;
import com.minicloud.infrastructure.hypervisor.NetworkSummary;
import com.minicloud.infrastructure.hypervisor.StoragePoolInfo;

import java.util.List;

public record VmCatalogResponse(
        boolean connected,
        String hypervisorStatus,
        Integer hostCpuCores,
        Long hostMemoryAvailableBytes,
        StoragePoolInfo storagePool,
        List<NetworkSummary> networks,
        List<OperatingSystemOption> operatingSystems,
        List<IsoCatalog.IsoImage> isoImages
) {
    public record OperatingSystemOption(String id, String label, boolean isoAvailable) {
    }
}
