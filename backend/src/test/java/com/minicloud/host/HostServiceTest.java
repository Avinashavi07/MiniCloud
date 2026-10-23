package com.minicloud.host;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.HostNotFoundException;
import com.minicloud.host.dto.HostResponse;
import com.minicloud.host.entity.Host;
import com.minicloud.host.repository.HostRepository;
import com.minicloud.host.service.HostService;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

    @Mock
    private HostRepository hostRepository;
    @Mock
    private HostMetricsProvider hostMetricsProvider;
    @Mock
    private AuditService auditService;

    private HostService hostService;

    @BeforeEach
    void setUp() {
        hostService = new HostService(hostRepository, hostMetricsProvider, auditService);
    }

    @Test
    void discoverLocalHostPersistsLiveSnapshot() {
        when(hostMetricsProvider.collect()).thenReturn(snapshot());
        when(hostRepository.findByHostname("lab-host")).thenReturn(Optional.empty());
        when(hostRepository.save(any(Host.class))).thenAnswer(invocation -> {
            Host host = invocation.getArgument(0);
            setHostId(host);
            return host;
        });

        HostResponse response = hostService.discoverLocalHost();

        ArgumentCaptor<Host> captor = ArgumentCaptor.forClass(Host.class);
        verify(hostRepository).save(captor.capture());
        assertThat(captor.getValue().getHostname()).isEqualTo("lab-host");
        assertThat(captor.getValue().getCpuCores()).isEqualTo(8);
        assertThat(captor.getValue().getKvmStatus()).isEqualTo("UNAVAILABLE");
        assertThat(response.metrics().memoryTotalBytes()).isEqualTo(16_000_000_000L);
        verify(auditService).record(any(), eq("HOST_DISCOVERED"), eq("HOST"), any(), eq("SUCCESS"), any());
    }

    @Test
    void getHostThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(hostRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hostService.getHost(id)).isInstanceOf(HostNotFoundException.class);
    }

    @Test
    void listHostsAttachesLiveMetricsOnlyToLocalHost() {
        when(hostMetricsProvider.collect()).thenReturn(snapshot());
        Host local = new Host();
        local.setHostname("lab-host");
        local.setLocal(true);
        setHostId(local);
        when(hostRepository.findByHostname("lab-host")).thenReturn(Optional.of(local));
        when(hostRepository.save(any(Host.class))).thenReturn(local);
        when(hostRepository.findAll()).thenReturn(List.of(local));

        List<HostResponse> hosts = hostService.listHosts();

        assertThat(hosts).hasSize(1);
        assertThat(hosts.getFirst().metrics()).isNotNull();
        assertThat(hosts.getFirst().metrics().cpuCores()).isEqualTo(8);
    }

    private HostSnapshot snapshot() {
        return new HostSnapshot(
                "lab-host",
                "Ubuntu",
                "Ubuntu 24.04.5 LTS",
                "7.0.0-34-generic",
                "amd64",
                8,
                "Test CPU",
                12.5,
                0.4,
                16_000_000_000L,
                8_000_000_000L,
                500_000_000_000L,
                400_000_000_000L,
                "/",
                new HostSnapshot.KvmDetection(false, false, false, "none", "UNAVAILABLE", "test"),
                new HostSnapshot.LibvirtDetection(false, null, "not-detected", false, false, "qemu:///system", "NOT_INSTALLED", "test"),
                Instant.now()
        );
    }

    private void setHostId(Host host) {
        try {
            var field = Host.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(host, UUID.randomUUID());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
