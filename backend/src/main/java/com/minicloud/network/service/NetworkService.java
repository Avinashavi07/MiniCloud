package com.minicloud.network.service;

import com.minicloud.audit.service.AuditService;
import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.InvalidNetworkConfigurationException;
import com.minicloud.common.exception.NetworkInUseException;
import com.minicloud.infrastructure.hypervisor.HypervisorProvider;
import com.minicloud.infrastructure.hypervisor.VmNicInfo;
import com.minicloud.infrastructure.network.ConnectedVmInfo;
import com.minicloud.infrastructure.network.CreateNetworkRequest;
import com.minicloud.infrastructure.network.Ipv4Networks;
import com.minicloud.infrastructure.network.NetworkProvider;
import com.minicloud.infrastructure.network.VirtualNetworkInfo;
import com.minicloud.network.dto.CreateNetworkRequestDto;
import com.minicloud.network.dto.NetworkListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);

    private final NetworkProvider networkProvider;
    private final HypervisorProvider hypervisorProvider;
    private final AuditService auditService;

    public NetworkService(
            NetworkProvider networkProvider,
            HypervisorProvider hypervisorProvider,
            AuditService auditService
    ) {
        this.networkProvider = networkProvider;
        this.hypervisorProvider = hypervisorProvider;
        this.auditService = auditService;
    }

    public NetworkListResponse list() {
        boolean connected = hypervisorProvider.capabilities().connected();
        List<VirtualNetworkInfo> networks = networkProvider.listNetworks().stream()
                .map(this::withConnectedVms)
                .toList();
        String note = connected
                ? "These networks come from libvirt. MiniCloud does not invent NAT or bridge records."
                : "libvirt is not connected. MiniCloud will not invent virtual networks.";
        log.info("Network inventory requested: connected={} count={}", connected, networks.size());
        return new NetworkListResponse(connected, note, networks);
    }

    public VirtualNetworkInfo get(String name) {
        log.info("Reading libvirt network {}", name);
        return withConnectedVms(networkProvider.getNetwork(name));
    }

    public VirtualNetworkInfo create(CreateNetworkRequestDto request, String username) {
        log.info("Network creation requested name={} cidr={}", request.getName(), request.getCidr());
        if (!hypervisorProvider.capabilities().connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected. Cannot create a real network.");
        }
        Ipv4Networks.validateCreate(request.getCidr(), request.getGateway(), request.getDhcpStart(), request.getDhcpEnd());
        rejectCidrOverlap(request.getCidr());
        try {
            VirtualNetworkInfo created = networkProvider.createNetwork(new CreateNetworkRequest(
                    request.getName(),
                    request.getCidr(),
                    request.getGateway(),
                    request.getDhcpStart(),
                    request.getDhcpEnd(),
                    request.isAutostart()
            ));
            auditService.record(username, "NETWORK_CREATED", "NETWORK", created.name(), "SUCCESS",
                    "Created libvirt network " + created.name());
            return withConnectedVms(created);
        } catch (RuntimeException ex) {
            auditService.record(username, "NETWORK_CREATED", "NETWORK", request.getName(), "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public VirtualNetworkInfo start(String name, String username) {
        return runAction(name, username, "NETWORK_STARTED", "start", () -> networkProvider.startNetwork(name));
    }

    public VirtualNetworkInfo stop(String name, String username) {
        VirtualNetworkInfo current = withConnectedVms(networkProvider.getNetwork(name));
        if (!current.connectedVms().isEmpty()) {
            throw new NetworkInUseException(
                    "Network '" + name + "' still has connected VM interface(s). Detach them first.");
        }
        return runAction(name, username, "NETWORK_STOPPED", "stop", () -> networkProvider.stopNetwork(name));
    }

    public void delete(String name, String username) {
        log.info("Network deletion requested for {}", name);
        VirtualNetworkInfo current = withConnectedVms(networkProvider.getNetwork(name));
        if (!current.connectedVms().isEmpty()) {
            throw new NetworkInUseException(
                    "Network '" + name + "' still has connected VM interface(s). Detach them first.");
        }
        try {
            networkProvider.deleteNetwork(name);
            auditService.record(username, "NETWORK_DELETED", "NETWORK", name, "SUCCESS", "Deleted libvirt network " + name);
        } catch (RuntimeException ex) {
            auditService.record(username, "NETWORK_DELETED", "NETWORK", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public VirtualNetworkInfo setAutostart(String name, boolean enabled, String username) {
        return runAction(name, username, "NETWORK_AUTOSTART", "autostart=" + enabled,
                () -> networkProvider.setAutostart(name, enabled));
    }

    private VirtualNetworkInfo runAction(String name, String username, String action, String verb, Runnable operation) {
        log.info("Network {} requested for {}", verb, name);
        try {
            operation.run();
            auditService.record(username, action, "NETWORK", name, "SUCCESS", "libvirt " + verb + " " + name);
            return withConnectedVms(networkProvider.getNetwork(name));
        } catch (RuntimeException ex) {
            auditService.record(username, action, "NETWORK", name, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private VirtualNetworkInfo withConnectedVms(VirtualNetworkInfo network) {
        List<ConnectedVmInfo> connected = new ArrayList<>();
        hypervisorProvider.listVms().forEach(vm -> {
            for (VmNicInfo nic : vm.nics()) {
                if (network.name().equals(nic.network())) {
                    connected.add(new ConnectedVmInfo(vm.name(), nic.interfaceName(), nic.mac(), nic.ip()));
                }
            }
        });
        return network.withConnectedVms(connected);
    }

    private void rejectCidrOverlap(String cidr) {
        Ipv4Networks.Block requested = Ipv4Networks.parseCidr(cidr);
        for (VirtualNetworkInfo existing : networkProvider.listNetworks()) {
            if (existing.cidr() == null) {
                continue;
            }
            try {
                Ipv4Networks.Block current = Ipv4Networks.parseCidr(existing.cidr());
                if (requested.overlaps(current)) {
                    throw new InvalidNetworkConfigurationException(
                            "CIDR " + cidr + " overlaps existing libvirt network '" + existing.name() + "' (" + existing.cidr() + ")");
                }
            } catch (InvalidNetworkConfigurationException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("overlaps")) {
                    throw ex;
                }
            }
        }
    }
}
