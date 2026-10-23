package com.minicloud.dashboard.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.config.MiniCloudProperties;
import com.minicloud.dashboard.dto.DashboardSummary;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.host.service.HostService;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final HostService hostService;
    private final HypervisorProvider hypervisorProvider;
    private final AuditService auditService;
    private final MiniCloudProperties properties;

    public DashboardService(
            HostService hostService,
            HypervisorProvider hypervisorProvider,
            AuditService auditService,
            MiniCloudProperties properties
    ) {
        this.hostService = hostService;
        this.hypervisorProvider = hypervisorProvider;
        this.auditService = auditService;
        this.properties = properties;
    }

    public DashboardSummary summarize() {
        List<HostResponse> hosts = hostService.listHosts();
        HostResponse local = hosts.stream().filter(HostResponse::local).findFirst().orElse(null);
        List<VirtualMachineInfo> vms = hypervisorProvider.listVms();
        HypervisorCapabilities capabilities = hypervisorProvider.capabilities();

        int running = (int) vms.stream().filter(vm -> "RUNNING".equals(vm.normalizedState())).count();
        int stopped = (int) vms.stream().filter(vm -> "STOPPED".equals(vm.normalizedState())).count();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (VirtualMachineInfo vm : vms) {
            distribution.merge(vm.normalizedState(), 1L, Long::sum);
        }

        Integer totalCpu = local != null && local.metrics() != null ? local.metrics().cpuCores() : null;
        Long totalMemory = local != null && local.metrics() != null ? local.metrics().memoryTotalBytes() : null;
        Long storageCapacity = local != null && local.metrics() != null ? local.metrics().diskTotalBytes() : null;
        Long storageUsed = local != null && local.metrics() != null ? local.metrics().diskUsedBytes() : null;
        int allocatedCpu = vms.stream()
                .map(VirtualMachineInfo::cpuCores)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        long allocatedMemory = vms.stream()
                .map(VirtualMachineInfo::memoryBytes)
                .filter(value -> value != null)
                .mapToLong(Long::longValue)
                .sum();

        return new DashboardSummary(
                properties.getPhase(),
                hosts.size(),
                vms.size(),
                running,
                stopped,
                totalCpu,
                allocatedCpu,
                totalMemory,
                allocatedMemory,
                storageCapacity,
                storageUsed,
                local != null && local.metrics() != null ? local.metrics().cpuUsagePercent() : null,
                local != null && local.metrics() != null ? local.metrics().memoryUsagePercent() : null,
                local != null && local.metrics() != null ? local.metrics().diskUsagePercent() : null,
                distribution,
                capabilities,
                local,
                vms,
                auditService.recent()
        );
    }
}
