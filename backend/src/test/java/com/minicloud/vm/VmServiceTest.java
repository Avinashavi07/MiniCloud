package com.minicloud.vm;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.InsufficientResourcesException;
import com.minicloud.common.exception.NotImplementedException;
import com.minicloud.common.exception.VmNotFoundException;
import com.minicloud.host.repository.HostRepository;
import com.minicloud.hypervisor.dto.VmListResponse;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import com.minicloud.infrastructure.hypervisor.CreateVmRequest;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.IsoCatalog;
import com.minicloud.infrastructure.hypervisor.StoragePoolInfo;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.ceph.CephProvider;
import com.minicloud.infrastructure.network.NetworkProvider;
import com.minicloud.infrastructure.ovs.OvsProvider;
import com.minicloud.infrastructure.storage.StorageProvider;
import com.minicloud.vm.dto.CreateVmRequestDto;
import com.minicloud.vm.repository.VirtualMachineRepository;
import com.minicloud.vm.service.VmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VmServiceTest {

    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private HostMetricsProvider hostMetricsProvider;
    @Mock
    private HostRepository hostRepository;
    @Mock
    private VirtualMachineRepository virtualMachineRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private IsoCatalog isoCatalog;
    @Mock
    private NetworkProvider networkProvider;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private OvsProvider ovsProvider;
    @Mock
    private CephProvider cephProvider;

    private VmService vmService;

    @BeforeEach
    void setUp() {
        vmService = new VmService(
                hypervisorProvider, hostMetricsProvider, hostRepository,
                virtualMachineRepository, auditService, isoCatalog, networkProvider, storageProvider, ovsProvider,
                cephProvider
        );
    }

    @Test
    void listDoesNotInventVmsWhenDisconnected() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, false, "NOT_INSTALLED", "missing", false
        ));
        when(hypervisorProvider.listVms()).thenReturn(List.of());

        VmListResponse response = vmService.list();

        assertThat(response.listingAvailable()).isFalse();
        assertThat(response.vms()).isEmpty();
    }

    @Test
    void createRejectsWhenHostMemoryIsTooSmall() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, true, "CONNECTED", "ok", true
        ));
        when(hostMetricsProvider.collect()).thenReturn(host(4, 256L * 1024 * 1024));

        CreateVmRequestDto request = request("too-big", 2, 4096, 10);

        assertThatThrownBy(() -> vmService.create(request, "admin"))
                .isInstanceOf(InsufficientResourcesException.class);
    }

    @Test
    void createPersistsMetadataOnlyAfterHypervisorSucceeds() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, true, "CONNECTED", "ok", true
        ));
        when(hostMetricsProvider.collect()).thenReturn(host(8, 16L * 1024 * 1024 * 1024));
        when(hypervisorProvider.storagePool()).thenReturn(new StoragePoolInfo("default", "running", 500L * 1024 * 1024 * 1024, 10L, 400L * 1024 * 1024 * 1024));
        when(hypervisorProvider.createVm(any())).thenReturn(
                VirtualMachineInfo.summary("web-01", "1", "running", "RUNNING")
        );
        when(virtualMachineRepository.findByName("web-01")).thenReturn(Optional.empty());
        when(hostRepository.findFirstByLocalTrue()).thenReturn(Optional.empty());
        when(virtualMachineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VirtualMachineInfo created = vmService.create(request("web-01", 2, 2048, 10), "admin");

        assertThat(created.name()).isEqualTo("web-01");
        ArgumentCaptor<CreateVmRequest> captor = ArgumentCaptor.forClass(CreateVmRequest.class);
        verify(hypervisorProvider).createVm(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("web-01");
        verify(virtualMachineRepository).save(any());
        verify(auditService).record(eq("admin"), eq("VM_CREATED"), eq("VM"), eq("web-01"), eq("SUCCESS"), any());
    }

    @Test
    void getPropagatesMissingDomain() {
        when(hypervisorProvider.getVm("missing")).thenThrow(new VmNotFoundException("missing"));
        assertThatThrownBy(() -> vmService.get("missing")).isInstanceOf(VmNotFoundException.class);
    }

    @Test
    void lifecycleFailureIsStillNotPretendSuccess() {
        when(hypervisorProvider.createVm(null)).thenThrow(new NotImplementedException("no"));
        assertThatThrownBy(() -> hypervisorProvider.createVm(null)).isInstanceOf(NotImplementedException.class);
    }

    private HostSnapshot host(int cores, long availableMemory) {
        return new HostSnapshot(
                "lab", "Ubuntu", "24.04", "6.8", "amd64", cores, "CPU", 1.0, 0.1,
                32L * 1024 * 1024 * 1024, availableMemory, 100L, 50L, "/",
                new HostSnapshot.KvmDetection(true, true, true, "intel-vt-x", "AVAILABLE", "ok"),
                new HostSnapshot.LibvirtDetection(true, "/run/libvirt/libvirt-sock", "active", true, true, "qemu:///system", "CONNECTED", "ok"),
                Instant.now()
        );
    }

    private CreateVmRequestDto request(String name, int cpu, long memory, long disk) {
        CreateVmRequestDto dto = new CreateVmRequestDto();
        dto.setName(name);
        dto.setCpuCores(cpu);
        dto.setMemoryMb(memory);
        dto.setDiskGb(disk);
        dto.setOperatingSystem("none");
        dto.setNetwork("default");
        dto.setStartAfterCreate(false);
        return dto;
    }
}
