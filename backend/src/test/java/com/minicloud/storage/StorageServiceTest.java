package com.minicloud.storage;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.StorageInUseException;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.hypervisor.VmDiskInfo;
import com.minicloud.infrastructure.storage.StorageProvider;
import com.minicloud.infrastructure.storage.StorageVolume;
import com.minicloud.infrastructure.storage.VirtualStoragePool;
import com.minicloud.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private HostMetricsProvider hostMetricsProvider;
    @Mock
    private AuditService auditService;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(storageProvider, hypervisorProvider, hostMetricsProvider, auditService);
    }

    @Test
    void listDoesNotInventPoolsWhenDisconnected() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, false, "NOT_INSTALLED", "missing", false
        ));
        when(storageProvider.listPools()).thenReturn(List.of());
        when(hostMetricsProvider.collect()).thenReturn(host());

        assertThat(storageService.list().listingAvailable()).isFalse();
        assertThat(storageService.list().pools()).isEmpty();
        assertThat(storageService.list().hostFilesystems()).isNotEmpty();
    }

    @Test
    void stopRefusesWhenVolumeIsAttached() {
        when(storageProvider.getPool("default")).thenReturn(pool("ubuntu-test-02.qcow2", "/var/lib/libvirt/images/ubuntu-test-02.qcow2"));
        when(hypervisorProvider.listVms()).thenReturn(List.of(vmWithDisk("ubuntu-test-02", "/var/lib/libvirt/images/ubuntu-test-02.qcow2")));

        assertThatThrownBy(() -> storageService.stop("default", "admin"))
                .isInstanceOf(StorageInUseException.class);
    }

    private static VirtualStoragePool pool(String volume, String path) {
        return new VirtualStoragePool(
                "default", "uuid", "running", true, true, true, "dir", "/var/lib/libvirt/images",
                100L, 10L, 90L,
                List.of(new StorageVolume(volume, "default", path, "file", "qcow2", 20L, 1L, List.of()))
        );
    }

    private static VirtualMachineInfo vmWithDisk(String name, String source) {
        return new VirtualMachineInfo(
                name, "uuid", "1", "running", "RUNNING", 2, 1L, 1L, "hvm", true, false,
                List.of(new VmDiskInfo("disk", "file", "vda", source)),
                List.of()
        );
    }

    private static HostSnapshot host() {
        return new HostSnapshot(
                "lab", "Ubuntu", "24.04", "6.8", "x86_64", 8, "cpu", 1.0, 1.0,
                16_000L, 8_000L, 100_000L, 40_000L, "/",
                null, null, Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
