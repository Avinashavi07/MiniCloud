package com.minicloud.dashboard.dto;

import com.minicloud.audit.dto.AuditEventResponse;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;

import java.util.List;
import java.util.Map;

public record DashboardSummary(
        int phase,
        int totalHosts,
        int totalVms,
        int runningVms,
        int stoppedVms,
        Integer totalCpuCores,
        Integer allocatedCpuCores,
        Long totalMemoryBytes,
        Long allocatedMemoryBytes,
        Long storageCapacityBytes,
        Long storageUsedBytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent,
        Double storageUsagePercent,
        Map<String, Long> vmStatusDistribution,
        HypervisorCapabilities hypervisor,
        HostResponse localHost,
        List<VirtualMachineInfo> vms,
        List<AuditEventResponse> recentEvents
) {
}
