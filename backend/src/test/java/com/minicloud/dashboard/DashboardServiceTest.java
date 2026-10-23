package com.minicloud.dashboard;

import com.minicloud.audit.service.AuditService;
import com.minicloud.config.MiniCloudProperties;
import com.minicloud.dashboard.dto.DashboardSummary;
import com.minicloud.dashboard.service.DashboardService;
import com.minicloud.host.dto.HostMetricsResponse;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.host.service.HostService;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private HostService hostService;
    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private AuditService auditService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        MiniCloudProperties properties = new MiniCloudProperties();
        properties.setPhase(2);
        dashboardService = new DashboardService(hostService, hypervisorProvider, auditService, properties);
    }

    @Test
    void summarizeUsesLiveHostAndDoesNotInventVms() {
        HostMetricsResponse metrics = new HostMetricsResponse(
                12, "CPU", 10.0, 0.2,
                16_000L, 8_000L, 8_000L, 50.0,
                100_000L, 40_000L, 60_000L, 60.0,
                "/", null, null, Instant.now()
        );
        HostResponse host = new HostResponse(
                UUID.randomUUID(), "lab", "Ubuntu", "24.04", "6.8", "amd64", true,
                "UNAVAILABLE", "NOT_INSTALLED", "qemu:///system", Instant.now(), Instant.now(), metrics
        );
        when(hostService.listHosts()).thenReturn(List.of(host));
        when(hypervisorProvider.listVms()).thenReturn(List.of());
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", false, false, "NOT_INSTALLED", "none", false
        ));
        when(auditService.recent()).thenReturn(List.of());

        DashboardSummary summary = dashboardService.summarize();

        assertThat(summary.phase()).isEqualTo(2);
        assertThat(summary.totalHosts()).isEqualTo(1);
        assertThat(summary.totalVms()).isZero();
        assertThat(summary.allocatedCpuCores()).isZero();
        assertThat(summary.totalCpuCores()).isEqualTo(12);
        assertThat(summary.storageUsedBytes()).isEqualTo(60_000L);
    }

    @Test
    void summarizeCountsOnlyHypervisorReportedVms() {
        HostResponse host = new HostResponse(
                UUID.randomUUID(), "lab", "Ubuntu", "24.04", "6.8", "amd64", true,
                "AVAILABLE", "CONNECTED", "qemu:///system", Instant.now(), Instant.now(), null
        );
        when(hostService.listHosts()).thenReturn(List.of(host));
        when(hypervisorProvider.listVms()).thenReturn(List.of(
                new VirtualMachineInfo("web", null, "1", "running", "RUNNING", 2, 4_096L * 1024 * 1024, null, "hvm", true, false, List.of(), List.of()),
                VirtualMachineInfo.summary("db", "-", "shut off", "STOPPED")
        ));
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, true, "CONNECTED", "ok", false
        ));
        when(auditService.recent()).thenReturn(List.of());

        DashboardSummary summary = dashboardService.summarize();

        assertThat(summary.totalVms()).isEqualTo(2);
        assertThat(summary.runningVms()).isEqualTo(1);
        assertThat(summary.stoppedVms()).isEqualTo(1);
        assertThat(summary.vmStatusDistribution()).containsEntry("RUNNING", 1L);
        assertThat(summary.allocatedCpuCores()).isEqualTo(2);
        assertThat(summary.allocatedMemoryBytes()).isEqualTo(4_096L * 1024 * 1024);
    }
}
