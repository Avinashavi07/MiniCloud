package com.minicloud.network;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.InvalidNetworkConfigurationException;
import com.minicloud.common.exception.NetworkInUseException;
import com.minicloud.infrastructure.hypervisor.HypervisorCapabilities;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VirtualMachineInfo;
import com.minicloud.infrastructure.hypervisor.VmNicInfo;
import com.minicloud.infrastructure.network.NetworkProvider;
import com.minicloud.infrastructure.network.VirtualNetworkInfo;
import com.minicloud.network.dto.CreateNetworkRequestDto;
import com.minicloud.network.service.NetworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkServiceTest {

    @Mock
    private NetworkProvider networkProvider;
    @Mock
    private HypervisorProvider hypervisorProvider;
    @Mock
    private AuditService auditService;

    private NetworkService networkService;

    @BeforeEach
    void setUp() {
        networkService = new NetworkService(networkProvider, hypervisorProvider, auditService);
    }

    @Test
    void listDoesNotInventNetworksWhenDisconnected() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, false, "NOT_INSTALLED", "missing", false
        ));
        when(networkProvider.listNetworks()).thenReturn(List.of());

        assertThat(networkService.list().listingAvailable()).isFalse();
        assertThat(networkService.list().networks()).isEmpty();
    }

    @Test
    void createRejectsOverlapWithDefault() {
        when(hypervisorProvider.capabilities()).thenReturn(HypervisorCapabilities.of(
                "libvirt", "qemu:///system", true, true, "CONNECTED", "ok", true
        ));
        when(networkProvider.listNetworks()).thenReturn(List.of(network("default", "192.168.122.0/24")));

        CreateNetworkRequestDto request = new CreateNetworkRequestDto();
        request.setName("overlap");
        request.setCidr("192.168.122.0/24");
        request.setGateway("192.168.122.1");
        request.setDhcpStart("192.168.122.10");
        request.setDhcpEnd("192.168.122.20");

        assertThatThrownBy(() -> networkService.create(request, "admin"))
                .isInstanceOf(InvalidNetworkConfigurationException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void stopRefusesWhenVmIsAttached() {
        when(networkProvider.getNetwork("default")).thenReturn(network("default", "192.168.122.0/24"));
        when(hypervisorProvider.listVms()).thenReturn(List.of(vmWithNic("ubuntu-test-02", "default")));

        assertThatThrownBy(() -> networkService.stop("default", "admin"))
                .isInstanceOf(NetworkInUseException.class);
    }

    private static VirtualNetworkInfo network(String name, String cidr) {
        return new VirtualNetworkInfo(name, "uuid", "active", true, true, true,
                "virbr0", "nat", cidr, "192.168.122.1", "192.168.122.2", "192.168.122.254", List.of());
    }

    private static VirtualMachineInfo vmWithNic(String name, String network) {
        return new VirtualMachineInfo(
                name, "uuid", "1", "running", "RUNNING", 2, 1L, 1L, "hvm", true, false,
                List.of(),
                List.of(new VmNicInfo("vnet0", "network", network, "virtio", "52:54:00:aa:bb:cc", null, "virbr0", "vnet0"))
        );
    }
}
